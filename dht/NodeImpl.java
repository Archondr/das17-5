import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The implementation of a node/peer in the distributed hash table.
 * Interfaces for usage and node-to-node communication are implemented.
 * @author Andreas
 * @param <E> is the type of value that is stored in the table.
 *
 */
public class NodeImpl<E> extends UnicastRemoteObject implements Node<E>, DHT<E> {

	private static final long serialVersionUID = 7837010474371220959L;
	
	/**
	 * The name (human-readable) of this node.
	 */
	private final String name;
	
	/**
	 * A key that decides which values this node is responsible for.
	 */
	private final String key;
	
	/**
	 * The successor in the ring.
	 */
	private AtomicReference<Node<E>> successor = new AtomicReference<>();
	
	/**
	 * The predecessor in the ring.
	 */
	private AtomicReference<Node<E>> predecessor = new AtomicReference<>();
	
	/**
	 * The actual storage of values that this node is responsible for.
	 */
	private Map<String, E> storage = new ConcurrentHashMap<>();
	private Deque<String> queue = new ConcurrentLinkedDeque<>();
	
	/**
	 * The routing table.
	 */
	private Map<String, Node<E>> fingers = new ConcurrentHashMap<>();
	
	/**
	 * The maximum number of keys (nodes/values) in the network.
	 */
	private static final int N = 1048576;
	
	/**
	 * A Default port used for RMI-connection.
	 */
	public static final int DEFAULT_PORT = 1099;
	
	/**
	 * Constructor to initiate a single node.
	 * @param name
	 * @throws RemoteException
	 */
	public NodeImpl(String name) throws RemoteException {
		this.name = name;
		key = Key.generate(name, N);
//		System.out.println(name + ": My key is " + key);
		successor.set(this);
		predecessor.set(this);
		Registry registry;
		try {
			registry = LocateRegistry.createRegistry(DEFAULT_PORT);
//			System.out.println("Create Registry");
		} catch (Exception e) {
			registry = LocateRegistry.getRegistry(DEFAULT_PORT);
//			System.out.println("Get registry.");
		}
		registry.rebind(name, this);
	}
	
	/**
	 * Constructor used to join another node (can be remote).
	 * @param name
	 * @param other
	 * @throws RemoteException
	 */
	public NodeImpl(String name, Node<E> other) throws RemoteException {
		this(name);
		join(other);
	}
	
	/**
	 * Constructor used to join a Node on a remote network.
	 * @param name
	 * @param host
	 * @param port
	 * @param ohterName
	 * @throws RemoteException
	 * @throws NotBoundException 
	 */
	@SuppressWarnings("unchecked")
	public NodeImpl(String name, String host, int port, String ohterName) throws RemoteException, NotBoundException {
		this(name);
		Registry registry = LocateRegistry.getRegistry(host, port);
		Node<E> other = (Node<E>) registry.lookup(ohterName);
		join(other);
	}
	
	@Override
	public void join(Node<E> other) {
		try {
		boolean joined = false;
			while(!joined) {
				Node<E> pred = other.getPredecessor();
				String otherKey = other.getKey();
				String predKey = pred.getKey();
				
				if(Key.between(key, predKey, otherKey)) {
					pred.setSuccessor(this);
					other.setPredecessor(this);
					setSuccessor(other);
					setPredecessor(pred);
					
					/*Get a share of the storage from our new successor*/
//					System.out.println(name + ": Asking successor to handover");
					Map<String, E> handover = successor.get().handover(predKey, key);
					for(String k : handover.keySet())
						storage.put(k, handover.get(k));
					/////////////////////////////////////////
					for (String s : successor.get().handoverQueue(predKey, key)) enqueueLocal(s);
					/////////////////////////////////////////
//					System.out.println(name + ": Done with handover.");
					
					joined = true;
				} else
					other = other.getSuccessor();
			}
//			System.out.println("Updating routing table...");
			updateRouting();
//			System.out.println("Done!");
		} catch(RemoteException e) {
			System.err.println("Error joining " + other);
			e.printStackTrace();
		}
	}
	
	public void leave() {
		try {
			/*Hand over items to successor.*/
//			System.out.println(name + ": I'm leaving. " + successor + " will handle my storage. (" + storage.size() + ") items.");
			for(String k : storage.keySet())
				successor.get().addStored(k, storage.get(k));
			///////////////////////////////////
			String s = dequeueLocal();
			while (s != null) {
				successor.get().enqueueLocal(s);
				s = dequeueLocal();
			}
			///////////////////////////////////
			System.out.println(name + ": Done.");
			
			successor.get().setPredecessor(predecessor.get());
			predecessor.get().setSuccessor(successor.get());
			successor.set(this);
			predecessor.set(this);
			updateRouting();
		} catch(RemoteException e) {
			System.err.println("Error leaving.");
			e.printStackTrace();
		}
	}

	@Override
	public Map<String, E> handover(String oldPredKey, String newPredKey) throws RemoteException {
//		System.out.println(name + ": Handing over values to new predecessor.");
		Map<String, E> handover = new LinkedHashMap<>();
		List<String> keys = new ArrayList<String>(storage.keySet());
		for(String k : keys)
			if(Key.between(k, oldPredKey, newPredKey))
				handover.put(k, storage.remove(k));
//		System.out.println(name + ": Handing over " + handover.size() + " keys/values.");
		return handover;
	}

	@Override
	public List<String> handoverQueue(String oldPredKey, String newPredKey) throws RemoteException {
		List<String> handover = new LinkedList<>();
		Deque<String> left = new ConcurrentLinkedDeque<>();
		for (String s : queue) {
			String key = Key.generate(s, N);
			if (Key.between(key, oldPredKey, newPredKey)) handover.add(s);
			else left.add(s);
		}
		queue = left;
		return handover;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public String getKey() throws RemoteException {
		return key;
	}

	@Override
	public Node<E> getSuccessor() throws RemoteException {
		return successor.get();
	}

	@Override
	public Node<E> getPredecessor() throws RemoteException {
		return predecessor.get();
	}

	@Override
	public void setSuccessor(Node<E> succ) throws RemoteException {
		successor.set(succ);
	}

	@Override
	public void setPredecessor(Node<E> pred) throws RemoteException {
		predecessor.set(pred);
	}

	@Override
	public void probe(String key, int count) throws RemoteException {
		if(this.key.equals(key) && count > 0) {
			System.out.println("Probe returned after " + count + " hops.");
		} else {
			System.out.println(name + ": Forwarding probe to " + successor.get());
			successor.get().probe(key, count+1);
		}
	}

	@Override
	public Node<E> lookup(String key) throws RemoteException {
		String predKey = predecessor.get().getKey();
		if(Key.between(key, predKey, getKey()))
			return this;
		else if(fingers.keySet().size() < 3) {
			return successor.get().lookup(key);
		}
		else {
			String[] keys = {};
			keys = fingers.keySet().toArray(keys);
			for(int i=0; i<(keys.length-1); i++) {
				String currentKey = keys[i];
				String nextKey = keys[i+1];
				if(Key.between(key, currentKey, nextKey)) {
					Node<E> currentNode = fingers.get(currentKey);
					Node<E> node = currentNode.getSuccessor();
					return node.lookup(key);
				}
			}
			return fingers.get(keys[keys.length-1]).getSuccessor().lookup(key);
		}
		
	}
	
	@Override
	public E getStored(String key) throws RemoteException {
		return storage.get(key);
	}
	
	@Override
	public void addStored(String key, E value) throws RemoteException {
//		System.out.println(name + ": Adding <" + key +", " + value + "> to my storage.");
		storage.put(key, value);
	}

	@Override
	public void enqueueLocal(String s) {
		//System.err.println("enqueueLocal: " + s);
		//System.err.println("queue.size() before: " + queue.size());
		queue.add(s);
		//System.err.println("queue.size() after: " + queue.size());
	}

	@Override
	public String dequeueLocal() {
		//System.err.println("queue.size() before: " + queue.size());
		String ret = queue.poll();
		//System.err.println("dequeueLocal: " + ret);
		return ret;
	}
	
	@Override
	public void removeStored(String key) throws RemoteException {
		storage.remove(key);
	}

	@Override
	public E get(String key) {
		try {
			String k = Key.generate(key, N);
			Node<E> node = lookup(k);
//			System.out.println("get " + node.getStored(k) + " from " + node);
			return node.getStored(k);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void put(String key, E object) {
		try {
			String k = Key.generate(key, N);
			Node<E> node = lookup(k);
			node.addStored(k, object);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void enqueue(String s) {
		try {
			String k = Key.generate(s, N);
			Node<E> node = lookup(k);
			node.enqueueLocal(s);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String dequeue() {
		return dequeueLocal();
	}

	@Override
	public void remove(String key) {
		try {
			String k = Key.generate(key, N);
			Node<E> node = lookup(k);
			node.removeStored(k);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<String> listAll() {
		Node<E> currentNode = this;
		ArrayList<String> all = new ArrayList<>();
		try {
			do {
				for(E element : currentNode.getValues())
					all.add(element.toString());
				currentNode = currentNode.getSuccessor();
			} while(!this.equals(currentNode));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return all;
	}

	@Override
	public List<E> getValues() throws RemoteException {
//		return storage.values();
		ArrayList<E> values = new ArrayList<>(storage.values());
		return values;
	}

	/**
	 * Get a list of all nodes in the network (Warning: May take some time, and consume a lot of resources!).
	 * @return a list of all nodes.
	 */
	private List<Node<E>> allNodes() {
		ArrayList<Node<E>> nodes = new ArrayList<>();
		try {
			Node<E> current = this;
			do {
				nodes.add(current);
//				System.out.println(name + ": node=" + current.toString());
				current = current.getSuccessor();
			} while(!this.equals(current));
		} catch(RemoteException e){
			System.err.println("Error finding all nodes!");
		}
		Collections.sort(nodes, new Comparator<Node<E>>() {
			@Override
			public int compare(Node<E> n1, Node<E> n2) {
				try {
					String key1 = n1.getKey();
					String key2 = n2.getKey();
					int i1 = Integer.parseInt(key1, 2);
					int i2 = Integer.parseInt(key2, 2);
					
					if(i1 > i2)
						return 1;
					if(i1 < i2)
						return -1;
					else
						return 0;
				}catch(RemoteException e) {
					return 0;
				}
			}
		});
		return nodes;
	}
	
	public void updateFingers(List<Node<E>> nodes) {
		Map<String, Node<E>> fingers = new ConcurrentHashMap<>();
		fingers.put(key, this);
		try {
			int myIndex = nodes.indexOf(this);
			
			for(int i=1; i<nodes.size(); i = i*2) {
				int nodeIndex = (myIndex + i) % nodes.size();
				Node<E> n = nodes.get(nodeIndex);
				fingers.put(n.getKey(), n);
			}
		} catch(RemoteException e) {
			e.printStackTrace();
		}
		this.fingers = fingers;
	}
	
	@Override
	public boolean equals(Object other) {
//		if(other instanceof NodeImpl<?>)
//			try {
//				return key.equals(((NodeImpl<?>) other).getKey());
//			} catch (RemoteException e) {
//				e.printStackTrace();
//				return false;
//			}
//		else
//			return false;
		if(other instanceof Node<?>)
			try {
				return key.equals(((Node<?>) other).getKey());
			} catch (RemoteException e) {
				e.printStackTrace();
				return false;
			}
		else
			return false;
	}

	@Override
	public void updateRouting() {
		try {
			List<Node<E>> nodes = allNodes();
//			System.out.println("Updating fingers on " + nodes.size() + " nodes.");
			for(Node<E> n : nodes)
				n.updateFingers(nodes);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
