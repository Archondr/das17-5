import java.rmi.RemoteException;
import java.util.*;

public class Runner {

    private static final int MANAGER_NUMBER = 1000;
    private static final int WORKERS_PER_MANAGER = 1;
    private static final int THREADS_PER_WORKER = 1;

    public static void main(String[] args) throws RemoteException {
        Sink sink = new Sink();
        List<Manager> managers = new ArrayList<>(MANAGER_NUMBER);
        for (int i = 0; i < MANAGER_NUMBER; ++i) {
            try {
                managers.add(new Manager(MANAGER_NUMBER, i));
            } catch (RemoteException e) {
                e.printStackTrace();
                managers.add(null);
            }
        }
        System.out.println("Managers started");
        for (int i = 0; i < managers.size(); ++i) {
            for (int j = 0; j < WORKERS_PER_MANAGER; ++j) {
                String name = Integer.toString(i) + "-" + Integer.toString(j);
                new Worker(managers.get(i), name, THREADS_PER_WORKER);
            }
        }
        System.out.println("Workers started");
        managers.get(0).add("https://www.google.co.uk");
        while (true) {
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Set<Edge> edges = sink.getEdges();
            edges.forEach(System.out::println);
            System.out.println(edges.size());
        }
    }
}
