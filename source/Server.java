import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private static int registryPort = 1099;

    private Set<String> crawled = new HashSet<>();
    private Queue<String> queue = new LinkedList<>();
    private Set<String> unconfirmed = new HashSet<>();
    private List<Edge> edges = new LinkedList<>();

    public Server(Iterable<String> seedUrls) throws RemoteException {
        seedUrls.forEach(queue::add);
    }

    public String sayHello() throws RemoteException {
        return "Hello World!";
    }

    public synchronized String getUrl() {
        String url = queue.poll();
        if (url != null) {
            unconfirmed.add(url);
        }
        return url;
    }

    public synchronized void putUrls(String src, Iterable<String> urls) {
        if (src != null) {
            unconfirmed.remove(src);
        }
        urls.forEach(url -> {
            if (!crawled.contains(url)) {
                crawled.add(url);
                queue.add(url);
            }
            edges.add(new Edge(src, url));
        });
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
            queue.add(url);
        });
    }

    public static void main(String args[]) {
        try {
            Iterable<String> seedUrls = Arrays.asList("https://www.google.co.uk");
            Server server = new Server(seedUrls);
            Registry registry = LocateRegistry.createRegistry(registryPort); // TODO change back to LocateRegistry.getRegistry()
            registry.rebind("Server", server);

            new Thread(new CronJob(server)).start();
            while (true) {
                Thread.sleep(30 * 1000);
                server.printUrls();
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
