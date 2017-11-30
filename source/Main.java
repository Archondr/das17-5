import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {

    //private static final int PORT = 1099;
    private static final int SERVER_NUMBER = 2;
    private static final int CLIENTS_PER_SERVER = 2;
    private static final int THREADS_PER_CLIENT = 2;

    public static void main(String[] args) throws RemoteException {
        CollectorImpl collector = new CollectorImpl();
        ArrayList<Server> servers = new ArrayList<>();
        Server firstServer = new Server("0", Arrays.asList("https://www.google.co.uk"));
        ArrayList<Client> clientList = new ArrayList<>();
        for (int j = 0; j < CLIENTS_PER_SERVER; ++j) {
            Client c = new Client("0", 1, Integer.toString(j));
            clientList.add(c);
        }
        Map<Server, ArrayList<Client>> clients = new HashMap<>();
        servers.add(firstServer);
        clients.put(firstServer, clientList);
        for (int i = 1; i < SERVER_NUMBER; ++i) {
            String name = Integer.toString(i);
            Server s = null;
            try {
                s = new Server(name, "0");
            } catch (Exception e) {
                e.printStackTrace();
            }
            clientList = new ArrayList<>();
            for (int j = 0; j < CLIENTS_PER_SERVER; ++j) {
                Client c = new Client(name, THREADS_PER_CLIENT, Integer.toString(j));
                clientList.add(c);
            }
            if (s != null) {
                servers.add(s);
                clients.put(s, clientList);
            }
        }
        System.out.println("nodes started");
        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        GraphGenerator.generate(collector.getEdges().keySet());
        try {
            Thread.sleep(57 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        collector.getEdges().forEach(System.out::println);
        System.out.println(collector.getEdges().size());
    }
}
