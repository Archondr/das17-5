import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

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

    private final Map<String, LocalDateTime> unconfirmed = new ConcurrentHashMap<>();

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
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::queueUnconfirmed, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void add(String s) throws RemoteException {
        Stats.addManagerIn(s);
        addInternal(s);
    }

    private void addInternal(String s) throws RemoteException {
        int hashValue = hash(s);
        if (hashValue == ID) {
            if (!set.containsKey(s) && !unconfirmed.containsKey(s)) {
                set.put(s, true);
                queue.add(s);
            }
        } else {
            getPeer(hashValue).add(s);
            Stats.addManagerOut(s);
        }
    }

    @Override
    public String getWork() throws RemoteException {
        String s = queue.poll();
        if (s != null) {
            set.remove(s);
            unconfirmed.put(s, LocalDateTime.now());
        }
        Stats.addManagerOut(s);
        return s;
    }

    @Override
    public void addResults(Iterable<Edge> results) throws RemoteException {
        Stats.addManagerIn(results);
        Set<Edge> found = new HashSet<>();
        for (Edge e : results) {
            unconfirmed.remove(e.getFrom());
            set.put(e.getFrom(), true);
            addInternal(e.getTo());
            found.add(e);
        }
        Set<String> crawled = new HashSet<>();
        found.forEach(e -> crawled.add(e.getFrom()));
        for (String s : crawled) {
            System.out.println(NAME + ": crawled " + s);
        }
        collector.add(found);
        //Stats.addManagerOut(found);
    }

    @Override
    public void checkIn(String s) throws RemoteException {
        Stats.addManagerIn(s);
        if (s != null) {
            unconfirmed.replace(s, LocalDateTime.now());
        }
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
        //System.err.println(NAME + ": queueing unconfirmed...");
        Set<String> set = unconfirmed.keySet();
        for (String s : set) {
            if (unconfirmed.get(s).plusSeconds(40).isBefore(LocalDateTime.now())) {
                unconfirmed.remove(s);
                try {
                    add(s);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int hash(String s) {
        long hashValue = s.hashCode();
        hashValue -= Integer.MIN_VALUE;
        hashValue %= N;
        return (int) hashValue;
    }
}
