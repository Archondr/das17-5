import java.rmi.*;

public interface ServerInterface extends Remote {
    String sayHello() throws RemoteException;
    String getUrl() throws RemoteException;
    void putUrls(Iterable<String> urls) throws RemoteException;
}
