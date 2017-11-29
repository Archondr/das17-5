import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Manager extends UnicastRemoteObject implements WorkQueue, Peer {

    private static final int PORT = 1099;

    private final int N;
    private final int ID;
    private final String NAME;

    private final Registry registry;
    private final List<Peer> peers;
    private final Collector collector;

    private final Map<String, Boolean> set = new ConcurrentHashMap<>();
    private final Deque<String> queue = new ConcurrentLinkedDeque<>();

    private final Map<String, Boolean> unconfirmed = new ConcurrentHashMap<>();

    public Manager(int n, int id) throws RemoteException {
        N = n;
        ID = id;
        NAME = Integer.toString(ID);
        peers = new ArrayList<>(N);
        for (int i = 0; i < N; ++i) {
            peers.add(null);
        }
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(PORT);
        }
        this.registry = registry;
        registry.rebind(NAME, this);
        Collector collector = null;
        try {
            collector = (Collector) registry.lookup("collector");
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        this.collector = collector;
        System.err.println(Arrays.asList(registry.list()));
    }

    @Override
    public void add(String s) throws RemoteException {
        int hashValue = hash(s);
        //System.err.println("" + hashValue + " " + ID);
        if (hashValue == ID) {
            if (!set.containsKey(s) && !unconfirmed.containsKey(s)) {
                set.put(s, true);
                queue.add(s);
            }
        } else {
            getPeer(hashValue).add(s);
        }
    }

    @Override
    public String getWork() throws RemoteException {
        String s = queue.poll();
        if (s != null) {
            set.remove(s);
            unconfirmed.put(s, true);
        }
        return s;
    }

    @Override
    public void addResults(Iterable<Edge> results) throws RemoteException {
        Set<Edge> found = new HashSet<>();
        for (Edge e : results) {
            unconfirmed.remove(e.getFrom());
            set.put(e.getFrom(), true);
            add(e.getTo());
            found.add(e);
        }
        Set<String> crawled = new HashSet<>();
        found.forEach(e -> crawled.add(e.getFrom()));
        for (String s : crawled) {
            System.out.println(NAME + ": crawled " + s);
        }
        collector.add(found);
    }

    private Peer getPeer(int id) throws RemoteException {
        synchronized (peers) {
            Peer peer = peers.get(id);
            if (peer == null) {
                try {
                    peer = (Peer) registry.lookup(Integer.toString(id));
                    peers.set(id, peer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return peer;
        }
    }

    private void queueUnconfirmed() {
        Set<String> set = unconfirmed.keySet();
        unconfirmed.clear();
        for (String s : set) {
            try {
                add(s);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private int hash(String s) {
        long hashValue = s.hashCode();
        hashValue -= Integer.MIN_VALUE;
        hashValue %= N;
        //System.err.println(s + " -> " + hashValue + ", " + s.hashCode());
        return (int) hashValue;
    }
}
