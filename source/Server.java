import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private static int registryPort = 1099;

    private Set<String> crawled = new HashSet<>();
    private Queue<String> queue = new LinkedList<>();
    private Set<String> unconfirmed = new HashSet<>();

    public Server() throws RemoteException {
        super();
        String seedUrl = "https://www.google.co.uk";
        queue.add(seedUrl);
        crawled.add(seedUrl);
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
            Server server = new Server();
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
