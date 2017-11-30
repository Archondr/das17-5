import java.rmi.RemoteException;
import java.util.*;

public class Runner {

    private static int MANAGER_NUMBER = 1;
    private static int WORKERS_PER_MANAGER = 1;
    private static int THREADS_PER_WORKER = 1;

    public static void main(String[] args) throws RemoteException {

        if(args.length == 3){
            MANAGER_NUMBER = Integer.parseInt(args[0]);
            WORKERS_PER_MANAGER = Integer.parseInt(args[1]);
            THREADS_PER_WORKER = Integer.parseInt(args[2]);
        }

        System.out.println("" + MANAGER_NUMBER + " * " + WORKERS_PER_MANAGER + " * " + THREADS_PER_WORKER);
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
                Thread.sleep(300 * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Set<Edge> edges = sink.getEdges();
            //edges.forEach(System.out::println);
            System.out.println(edges.size());
            Stats.print(MANAGER_NUMBER, MANAGER_NUMBER * WORKERS_PER_MANAGER);
            System.exit(0);
        }
    }
}
