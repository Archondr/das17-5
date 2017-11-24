import java.rmi.*;

public interface ServerInterface extends Remote {
    String sayHello() throws RemoteException;
    String getUrl() throws RemoteException;
    void putUrl(String url) throws RemoteException;
}
