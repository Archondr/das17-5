import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

public class Worker implements Runnable {

    private final String NAME;

    private WorkQueue workQueue;

    public Worker(WorkQueue workQueue, String name, int threadNumber) {
        this.workQueue = workQueue;
        NAME = name;
        for (int i = 0; i < threadNumber; ++i) {
            String threadName = name + "-" + Integer.toString(i);
            new Thread(new Worker(workQueue, threadName)).start();
            System.err.println("Thread " + threadName + " started");
        }
    }

    public Worker(WorkQueue workQueue, String name) {
        this.workQueue = workQueue;
        NAME = name;
    }

    public void run() {
        while (true) {
            String s = null;
            try {
                s = workQueue.getWork();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //System.out.println(NAME + ": started crawling " + s);
            if (s == null) {
                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            try {
                List<Edge> results = Crawler.crawlModified(new URL(s), 1);
                workQueue.addResults(results);
            } catch (MalformedURLException|RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
