import java.rmi.*;

public interface ServerInterface extends Remote {
    String getUrl() throws RemoteException;
    void putEdges(Iterable<Edge> edges) throws RemoteException;
}
