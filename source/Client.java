import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable {

    private ServerInterface stub;

    private Client(ServerInterface server) {
        stub = server;
    }

    public void run() {
        try {
            ClientCheckIn clientCheckIn = new ClientCheckIn(stub);
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(clientCheckIn, 0, 30, TimeUnit.SECONDS);

            String url = stub.getUrl();
            System.err.println(url);
            clientCheckIn.updateUrl(url);
            for (int i = 0; i < 20 && url != null; ++i) {

                URL urlToCrawl = new URL(url);
                List<Edge> edges = Crawler.crawlModified(urlToCrawl, 20);
                stub.putEdges(edges);
                url = stub.getUrl();
                clientCheckIn.updateUrl(url);

            }
        } catch (Exception ex) {
            System.err.println("In client.run()");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String host = "localhost";
        int threadNumber = 1;

        if(args.length > 0 && args[0] != null){
            host = args[0];
        }

        if(args.length > 1 && args[1] !=null){
            threadNumber = Integer.parseInt(args[1]);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ServerInterface stub = (ServerInterface) registry.lookup("CrawlerServer");
            List<Thread> threads = new LinkedList<>();
            for (int i = 0; i < threadNumber; ++i) {
                Thread t = new Thread(new Client(stub));
                threads.add(t);
                t.start();
                System.out.println("thread " + i + " started");
            }
        } catch (Exception ex) {
            System.err.println("Client exception: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
