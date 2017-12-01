public class Stats {

    private static Integer managerInBytes = 0;
    private static Integer managerInN = 0;
    private static Integer managerOutBytes = 0;
    private static Integer managerOutN = 0;
    private static Integer workerInBytes = 0;
    private static Integer workerInN = 0;
    private static Integer workerOutBytes = 0;
    private static Integer workerOutN = 0;
    private static Integer sinkInBytes = 0;
    private static Integer sinkInN = 0;

    public static int getBytes(Object o) {
        if (o == null) {
            return 4 * 2;
        }
        if (o instanceof String) {
            return ((String) o).length() * 2;
        } else {
            int total = 0;
            for (Object e : ((Iterable) o)) {
                total += e.toString().length();
            }
            return total * 2;
        }
    }

    public synchronized static void addManagerIn(Object o) {
        managerInBytes += getBytes(o);
        ++managerInN;
    }

    public synchronized static void addManagerOut(Object o) {
        managerOutBytes += getBytes(o);
        ++managerOutN;
    }

    public synchronized static void addWorkerIn(Object o) {
        workerInBytes += getBytes(o);
        ++workerInN;
    }

    public synchronized static void addWorkerOut(Object o) {
        workerOutBytes += getBytes(o);
        ++workerOutN;
    }

    public synchronized static void addSinkIn(Object o) {
        sinkInBytes += getBytes(o);
        ++sinkInN;
    }

    public synchronized static void print(float m, float w) {
        System.out.println("Results(N) - mIn: " + (managerInN / m) + ", mOut: " + (managerOutN / m)
                + ", wIn: " + (workerInN / w) + ", wOut: " + (workerOutN / w));// + ", sIn: " + (sinkInN));
        System.out.println("Results(Bytes) - mIn: " + h(managerInBytes / m) + ", mOut: " +h (managerOutBytes / m)
                + ", wIn: " + h(workerInBytes / w) + ", wOut: " + h(workerOutBytes / w));// + ", sIn: " + h(sinkInBytes));
    }

    public static String h(float f) {
        return String.format("%.2e", f);
    }

    public static void main(String[] args) {
        System.out.println(h(1234));
    }
}
