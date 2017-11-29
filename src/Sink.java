import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Sink extends UnicastRemoteObject implements Collector {

    private static final int PORT = 1099;

    private final Map<Edge, Boolean> edges = new ConcurrentHashMap<>();

    public Sink() throws RemoteException {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(PORT);
        }
        registry.rebind("collector", this);
    }

    public void add(Iterable<Edge> edges) throws RemoteException {
        Stats.addSinkIn(edges);
        for (Edge e : edges) {
            this.edges.put(e, true);
        }
    }

    public Set<Edge> getEdges() throws RemoteException {
        return edges.keySet();
    }
}
