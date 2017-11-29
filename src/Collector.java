import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Collector extends Remote {

    void add(Iterable<Edge> edges) throws RemoteException;
    Set<Edge> getEdges() throws RemoteException;
}
