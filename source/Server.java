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

    private Set<String> unqueueable = new HashSet<>(); // crawled (confirmed) + queued
    private Set<Edge> edges = new HashSet<>(); // final results
    private HashMap<String, LocalDateTime> unconfirmed = new HashMap<>();

    private DHT<String> dht;

    public Server(String name, Iterable<String> seedUrls) throws RemoteException {
        dht = new NodeImpl<>(name);
        seedUrls.forEach(dht::enqueue);
        seedUrls.forEach(unqueueable::add);
    }

    public Server(String name, String other) throws Exception {
        dht = new NodeImpl<>(name, "localhost", 1099, other);
    }

    public synchronized String getUrl() {
        String url = dht.dequeue();
        if (url != null) {
            unconfirmed.put(url, LocalDateTime.now());
            unqueueable.remove(url);
        }
        return url;
    }

    public synchronized void putEdges(Iterable<Edge> edges) {
        edges.forEach(e -> {
            unconfirmed.remove(e.getFrom());
            unqueueable.add(e.getFrom());
            if (!unqueueable.contains(e.getTo())) {
                dht.enqueue(e.getTo());
                unqueueable.add(e.getTo());
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
                dht.enqueue(url);
                unqueueable.add(url);
            }
        });
    }

    public static void main(String[] args) {

        int registryPort = 1099;

        if(args.length > 0 && args[0] != null) {
            registryPort = Integer.parseInt(args[0]);
        }

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            Iterable<String> seedUrls = Arrays.asList("https://www.google.co.uk");
            Server server = new Server("0", seedUrls);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(registryPort);
            } catch (RemoteException ex) {
                registry = LocateRegistry.getRegistry(registryPort);
            }
            registry.rebind("CrawlerServer", server);

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
