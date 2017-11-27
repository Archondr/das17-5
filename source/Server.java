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

    private NodeImpl<String> node;
    private Set<Edge> edges = new HashSet<>(); // final results
    private HashMap<String, LocalDateTime> unconfirmed = new HashMap<>();

    public Node<String> getNode() throws RemoteException { return node; }

    public Server(String name, Iterable<String> seedUrls) throws RemoteException {
        node = new NodeImpl<>(name);
        seedUrls.forEach(node::enqueue);
        seedUrls.forEach(url -> node.put(url, url));
    }

    public Server(String name, Registry registry, String other) throws Exception {
        ServerInterface server = (ServerInterface) registry.lookup("server"+other);
        Node<String> node = server.getNode();
        this.node = new NodeImpl<>(name, node);
    }

    public synchronized String getUrl() {
        System.err.println("getUrl()");
        String url = node.dequeue();
        if (url != null) {
            unconfirmed.put(url, LocalDateTime.now());
            node.remove(url);
        }
        return url;
    }

    public synchronized void putEdges(Iterable<Edge> edges) {
        edges.forEach(e -> {
            unconfirmed.remove(e.getFrom());
            node.put(e.getFrom(), e.getFrom());
            if (node.get(e.getTo()) == null) {
                node.enqueue(e.getTo());
                node.put(e.getTo(), e.getTo());
            }
            this.edges.add(e);
        });
    }

    public synchronized void clientCheckIn(String url) throws RemoteException {
        unconfirmed.replace(url, LocalDateTime.now());
    }

    public synchronized void printUrls() {
        edges.forEach(System.out::println);
        System.out.println(edges.size());
        unconfirmed.keySet().forEach(System.out::println);
        System.out.println(unconfirmed.size());
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
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(registryPort);
            } catch (RemoteException ex) {
                registry = LocateRegistry.getRegistry(registryPort);
            }
            Server server = new Server(name, seedUrls);
            //name = "second"; Server server = new Server(name, registry, "first");
            registry.rebind("server"+name, server);
            System.err.println(Arrays.asList(registry.list()));

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
