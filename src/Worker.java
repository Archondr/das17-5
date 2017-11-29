import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Worker implements Runnable {

    private final String NAME;

    private final WorkQueue workQueue;
    private AtomicReference<String> currentUrl = new AtomicReference<>();

    public Worker(WorkQueue workQueue, String name, int threadNumber) {
        this.workQueue = workQueue;
        NAME = name;
        for (int i = 0; i < threadNumber; ++i) {
            String threadName = name + "-" + Integer.toString(i);
            new Thread(new Worker(workQueue, threadName)).start();
            //System.err.println("Thread " + threadName + " started");
        }

    }

    public Worker(WorkQueue workQueue, String name) {
        this.workQueue = workQueue;
        NAME = name;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkIn, 30, 30, TimeUnit.SECONDS);
    }

    private void checkIn() {
        /* some randomization would not hurt otherwise
        all worker-threads might contact manager at the same time
        try {
            Thread.sleep(Math.round(Math.random() * 15 * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }*/
        System.err.println(NAME + ": checking in with " + currentUrl.get());
        try {
            workQueue.checkIn(currentUrl.get());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            String s = null;
            try {
                s = workQueue.getWork();
                currentUrl.set(s);
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
