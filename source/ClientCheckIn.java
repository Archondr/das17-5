import java.rmi.RemoteException;

public class ClientCheckIn implements Runnable{

    ServerInterface stub;
    String url;

    public ClientCheckIn(ServerInterface stub){
        this.stub = stub;
    }

    public void updateUrl(String url) {
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
