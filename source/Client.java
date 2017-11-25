import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        String host = "localhost";
        int threads = 0;

        if(args.length > 0 && args[0] != null){
            host = args[0];
        }

        if(args.length > 1 && args[1] !=null){
            threads = Integer.parseInt(args[1]);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ServerInterface stub = (ServerInterface) registry.lookup("CrawlerServer");

            ClientCheckIn clientCheckIn = new ClientCheckIn(stub);
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(clientCheckIn, 0, 30, TimeUnit.SECONDS);

            String url = stub.getUrl();
            clientCheckIn.updateUrl(url);
            for (int i = 0; i < 20 && url != null; ++i) {

                URL urlToCrawl = new URL(url);
                List<Edge> edges = Crawler.crawlModified(urlToCrawl, 20);
                stub.putEdges(edges);
                url = stub.getUrl();
                clientCheckIn.updateUrl(url);

            }
        } catch (Exception ex) {
            System.err.println("Client exception: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
