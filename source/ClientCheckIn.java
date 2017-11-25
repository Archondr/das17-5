import java.rmi.RemoteException;

public class ClientCheckIn implements Runnable{

    ServerInterface stub;
    String url;

    public ClientCheckIn(ServerInterface stub, String url){
        this.stub = stub;
        this.url = url;
    }

    @Override
    public void run() {
        try{
            stub.clientCheckIn(url);
        } catch (RemoteException ex){
            System.err.println("ClientCheckIn Error: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
