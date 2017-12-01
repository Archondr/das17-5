import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Peer extends Remote {

    void add(String s) throws RemoteException;
}
