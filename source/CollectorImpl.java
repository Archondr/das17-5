import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectorImpl extends UnicastRemoteObject implements Collector {

    private static final int PORT = 1099;

    private Map<Edge, String> edges = new ConcurrentHashMap<>();

    public CollectorImpl() throws RemoteException {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            System.err.println("cannot create registry");
            e.printStackTrace();
            registry = LocateRegistry.getRegistry(PORT);
        }
        try {
            registry.rebind("collector", this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Map<Edge, String> getEdges() {
        return edges;
    }

    @Override
    public void putEdges(List<Edge> edges) throws RemoteException {
        for (Edge e : edges) {
            this.edges.put(e, "");
        }
    }

    public static void main(String[] args) throws RemoteException {
        CollectorImpl collector = new CollectorImpl();
        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        collector.getEdges().keySet().forEach(System.out::println);
        System.out.println(collector.getEdges().keySet().size());
    }
}
