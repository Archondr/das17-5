import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.time.LocalDateTime;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private Set<String> crawled = new HashSet<>();
    private Set<String> queued = new HashSet<>();
    private LinkedList<String> queue = new LinkedList<>();
    private Set<Edge> edges = new HashSet<>();
    private HashMap<String, LocalDateTime> unconfirmed = new HashMap<>();

    public Server(Iterable<String> seedUrls) throws RemoteException {
        seedUrls.forEach(queue::add);
        seedUrls.forEach(queued::add);
    }

    public synchronized String getUrl() {
        String url = queue.poll();
        if (url != null) {
            unconfirmed.put(url, LocalDateTime.now());
            queued.remove(url);
        }
        return url;
    }

    public synchronized void putEdges(Iterable<Edge> edges) {
        edges.forEach(e -> {
            unconfirmed.remove(e.getFrom());
            crawled.add(e.getFrom());
            if (!crawled.contains(e.getTo()) && !queued.contains(e.getTo())) {
                queue.add(e.getTo());
                queued.add(e.getTo());
            }
            this.edges.add(e);
        });
    }

    public synchronized void clientCheckIn(String url) throws RemoteException {
        unconfirmed.replace(url, LocalDateTime.now());
    }

    public synchronized void printUrls() {
        crawled.forEach(System.out::println);
        System.out.println(crawled.size());
        unconfirmed.keySet().forEach(System.out::println);
        System.out.println(unconfirmed.size());
    }

    public synchronized void moveUnconfirmedToQueue() {
        //TODO find a way to do without a copy of unconfirmed
        Iterable<String> urlsToMove = new LinkedList<>(unconfirmed.keySet());
        urlsToMove.forEach(url -> {
            if( unconfirmed.get(url).plusMinutes(1).isBefore(LocalDateTime.now()) ){
                unconfirmed.remove(url);
                queue.addFirst(url);
                queued.add(url);
            }
        });
    }

    public static void main(String[] args) {

        int registryPort = 1099;

        if(args.length > 0 && args[0] != null) {
            registryPort = Integer.parseInt(args[0]);
        }

        try {
            Iterable<String> seedUrls = Arrays.asList("https://www.google.co.uk");
            Server server = new Server(seedUrls);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(registryPort);
            } catch (RemoteException ex) {
                registry = LocateRegistry.getRegistry(registryPort);
            }
            registry.rebind("CrawlerServer", server);

            new Thread(new CronJob(server)).start();

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
