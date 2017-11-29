import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final int PORT = 1099;

    public static void main(String[] args) throws RemoteException {
        CollectorImpl collector = new CollectorImpl();
        ArrayList<Server> servers = new ArrayList<>();
        Server firstServer = new Server("0", Arrays.asList("https://www.google.co.uk"));
        servers.add(firstServer);
        Map<Server, ArrayList<Client>> clients = new HashMap<>();
        for (int i = 1; i < 2; ++i) {
            String name = Integer.toString(i);
            Server s = null;
            try {
                s = new Server(name, "0");
            } catch (Exception e) {
                e.printStackTrace();
            }
            ArrayList<Client> clientList = new ArrayList<>();
            for (int j = 0; j < 1; ++j) {
                Client c = new Client(name, 1);
                clientList.add(c);
            }
            if (s != null) {
                servers.add(s);
                clients.put(s, clientList);
            }
        }
        System.out.println("nodes started");
    }
}
