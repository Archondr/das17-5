import java.rmi.*;

public interface ServerInterface extends Remote {
    String sayHello() throws RemoteException;
}