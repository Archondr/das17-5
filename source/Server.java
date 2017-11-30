import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private static final int PORT = 1099;

    private final String name;

    private Collector collector;
    private NodeImpl<String> node;
    private Set<Edge> edges = new HashSet<>(); // final results
    private HashMap<String, LocalDateTime> unconfirmed = new HashMap<>();

    public Node<String> getNode() throws RemoteException { return node; }

    public Server(String name, Iterable<String> seedUrls) throws RemoteException {
        Registry registry = init(PORT);
        node = new NodeImpl<>(name);
        seedUrls.forEach(node::enqueue);
        seedUrls.forEach(url -> node.put(url, url));
        this.name = "server " + name;
        registry.rebind(this.name, this);
        System.err.println(Arrays.asList(registry.list()));
    }

    public Server(String name, String other) throws Exception {
        Registry registry = init(PORT);
        ServerInterface server = (ServerInterface) registry.lookup("server "+other);
        Node<String> node = server.getNode();
        this.node = new NodeImpl<>(name, node);
        this.name = "server " + name;
        registry.rebind(this.name, this);
        System.err.println(Arrays.asList(registry.list()));
    }

    private Registry init(int port) throws RemoteException {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(port);
        } catch (RemoteException e) {
            e.printStackTrace();
            registry = LocateRegistry.createRegistry(port);
        }
        try {
            collector = (Collector) registry.lookup("collector");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return registry;
    }

    public synchronized String getUrl() {
        String url = node.dequeue();
        if (url != null) {
            unconfirmed.put(url, LocalDateTime.now());
            node.remove(url);
        }
        return url;
    }

    public synchronized void putEdges(Iterable<Edge> edges) {
        Set<Edge> uniqueEdges = new HashSet<>();
        edges.forEach(e -> {
            unconfirmed.remove(e.getFrom());
            node.put(e.getFrom(), e.getFrom());
            if (node.get(e.getTo()) == null) {
                node.enqueue(e.getTo());
                node.put(e.getTo(), e.getTo());
            }
            this.edges.add(e);
            uniqueEdges.add(e);
        });
        Set<String> uniqueCrawled = new HashSet<>();
        for (Edge e : uniqueEdges) {
            uniqueCrawled.add(e.getFrom());
        }
        for (String s : uniqueCrawled) {
            System.out.println(name + ": " + s + " has just been crawled");
        }
        try {
            collector.add(new LinkedList<>(uniqueEdges));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void clientCheckIn(String url) throws RemoteException {
        unconfirmed.replace(url, LocalDateTime.now());
    }

    public synchronized void printUrls() {
        //edges.forEach(System.out::println);
        //System.out.println(edges.size());
        //unconfirmed.keySet().forEach(System.out::println);
        //System.out.println(unconfirmed.size());
        //GraphGenerator.generate(edges);
    }

    public synchronized void moveUnconfirmedToQueue() {
        //TODO find a way to do without a copy of unconfirmed
        Iterable<String> urlsToMove = new LinkedList<>(unconfirmed.keySet());
        urlsToMove.forEach(url -> {
            if( unconfirmed.get(url).plusMinutes(1).isBefore(LocalDateTime.now()) ){
                unconfirmed.remove(url);
                node.enqueue(url);
                node.put(url, url);
            }
        });
    }

    public static void main(String[] args) {

        int registryPort = 1099;
        String name = "first";

        if(args.length > 0 && args[0] != null) {
            registryPort = Integer.parseInt(args[0]);
        }

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            Iterable<String> seedUrls = Arrays.asList("https://www.google.co.uk");
            Server server = new Server(name, seedUrls);
            //name = "second"; Server server = new Server(name, "first");

            Runnable moveUnconfirmed = server::moveUnconfirmedToQueue;
            scheduler.scheduleAtFixedRate(moveUnconfirmed, 40, 40, TimeUnit.SECONDS);

            while (true) {
                Thread.sleep(30 * 1000);
                server.printUrls();
            }

        } catch(Exception ex){
            System.err.println("Server exception: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
