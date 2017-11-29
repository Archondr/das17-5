import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Collector extends Remote {

    void putEdges(List<Edge> edges) throws RemoteException;
}
