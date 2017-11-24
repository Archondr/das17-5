public class CronJob implements Runnable {

    private Server server;

    public CronJob(Server server) {
        this.server = server;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            server.moveUnconfirmedToQueue();
        }
    }
}
