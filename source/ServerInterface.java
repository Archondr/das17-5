import java.rmi.*;

public interface ServerInterface extends Remote {
    String getUrl() throws RemoteException;
    void putUrls(String src, Iterable<String> urls) throws RemoteException;
    void clientCheckIn(String url) throws RemoteException;
}
