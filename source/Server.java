import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private Set<String> crawled = new HashSet<>();
    private Set<String> queued = new HashSet<>();
    private LinkedList<String> queue = new LinkedList<>();
    private Set<String> unconfirmed = new HashSet<>();
    private Set<Edge> edges = new HashSet<>();

    public Server(Iterable<String> seedUrls) throws RemoteException {
        seedUrls.forEach(queue::add);
        seedUrls.forEach(queued::add);
    }

    public synchronized String getUrl() {
        String url = queue.poll();
        if (url != null) {
            unconfirmed.add(url);
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
        /*if (src != null) {
            unconfirmed.remove(src);
        }
        urls.forEach(url -> {
            if (!crawled.contains(url)) {
                crawled.add(url);
                queue.add(url);
            }
            edges.add(new Edge(src, url));
        });*/
    }

    public synchronized void printUrls() {
        crawled.forEach(System.out::println);
        System.out.println(crawled.size());
        unconfirmed.forEach(System.out::println);
        System.out.println(unconfirmed.size());
    }

    public synchronized void moveUnconfirmedToQueue() {
        //TODO find a way to do without a copy of unconfirmed
        Iterable<String> urlsToMove = new LinkedList<>(unconfirmed);
        urlsToMove.forEach(url -> {
            unconfirmed.remove(url);
            queue.addFirst(url);
            queued.add(url);
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
