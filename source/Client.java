import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.stream.Collectors;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        String host = "localhost";

        if(args[0] != null){
            host = args[0];
        }

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ServerInterface stub = (ServerInterface) registry.lookup("CrawlerServer");

            String url = stub.getUrl();
            for (int i = 0; i < 20 && url != null; ++i) {

                URL urlToCrawl = new URL(url);
                List<URL> foundUrls = Crawler.crawl(urlToCrawl, 20);
                Iterable<String> urlStrings = foundUrls.stream().map(Object::toString).collect(Collectors.toList());
                stub.putUrls(url, urlStrings);
                url = stub.getUrl();

            }
        } catch (Exception ex) {
            System.err.println("Client exception: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
