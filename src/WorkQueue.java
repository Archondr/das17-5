import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkQueue extends Remote {

    String getWork() throws RemoteException;
    void addResults(Iterable<Edge> results) throws RemoteException;
}
