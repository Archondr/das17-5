import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private static int registryPort = 1099;

    private HashSet<String> crawled = new HashSet<>();
    private Queue<String> queue = new LinkedList<>();


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
        return queue.poll();
    }

    public synchronized void putUrls(Iterable<String> urls) {
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
    }

    public static void main(String args[]) {
        try {
            Server server = new Server();
            Registry registry = LocateRegistry.createRegistry(registryPort); // TODO change back to LocateRegistry.getRegistry()
            registry.rebind("Server", server);

            Thread.sleep(30 * 1000);
            server.printUrls();
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
