import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class Server extends UnicastRemoteObject implements ServerInterface {

    static int registryPort = 1099;

    public Server() throws RemoteException {
        super();
    }

    public String sayHello() throws RemoteException {
        return "Hello World!";
    }

    public static void main(String args[]) {

        try{
            Server server = new Server();

            Registry registry = LocateRegistry.createRegistry(registryPort); // TODO change back to LocateRegistry.getRegistry()


            registry.rebind("Server", server);

        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
