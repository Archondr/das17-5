import java.rmi.*;

public interface ServerInterface extends Remote {
    Node<String> getNode() throws RemoteException;
    String getUrl() throws RemoteException;
    void clientCheckIn(String url) throws RemoteException;
    void putEdges(Iterable<Edge> edges) throws RemoteException;
}
