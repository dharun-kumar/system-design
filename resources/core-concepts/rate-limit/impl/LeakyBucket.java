import java.time.LocalTime;
import java.util.concurrent.*;

public class LeakyBucket {

    private final BlockingQueue<Long> BUCKET;
    private final ScheduledExecutorService PROCESSOR;

    public LeakyBucket(int capacity, int requestsPerSecond) {
        BUCKET = new LinkedBlockingDeque<>(capacity);
        PROCESSOR = Executors.newSingleThreadScheduledExecutor();

        long leakIntervalMs = TimeUnit.SECONDS.toMillis(1) / requestsPerSecond;
        PROCESSOR.scheduleAtFixedRate(this::leakBucket, 0, leakIntervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean canProcessRequest(long requestID) {
        return BUCKET.offer(requestID);
    }

    private synchronized void leakBucket() {
        Long requestID = BUCKET.poll();
        if(requestID != null) {
            System.out.println("Request " + requestID + " 200 OK " + LocalTime.now().withNano(0));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LeakyBucket leakyBucket = new LeakyBucket(5, 1);

        for(int i=1; i<7; i++) {
            if(!leakyBucket.canProcessRequest(i)) {
                System.out.println("Request " + i + " 429 Too Many Requests " + LocalTime.now().withNano(0));
            }
        }

        Thread.sleep(5000);

        for(int i=7; i<12; i++) {
            if(!leakyBucket.canProcessRequest(i)) {
                System.out.println("Request " + i + " 429 Too Many Requests " + LocalTime.now().withNano(0));
            }
        }
    }

}
