import java.time.LocalTime;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SlidingWindowLog {

    private final Queue<Long> LOG_QUEUE;
    private final long WINDOW_SIZE_IN_SECONDS;
    private final int MAX_REQUESTS_PER_WINDOW;

    public SlidingWindowLog(long windowSizeInSeconds, int maxRequestsPerWindow) {
        WINDOW_SIZE_IN_SECONDS = windowSizeInSeconds;
        MAX_REQUESTS_PER_WINDOW = maxRequestsPerWindow;

        LOG_QUEUE = new LinkedBlockingDeque<>(MAX_REQUESTS_PER_WINDOW);
    }

    public synchronized boolean canProcessRequest() {

        long now = System.currentTimeMillis();
        long currentWindowStart = now - TimeUnit.SECONDS.toMillis(WINDOW_SIZE_IN_SECONDS);

        while(!LOG_QUEUE.isEmpty() && LOG_QUEUE.peek() < currentWindowStart) {
            LOG_QUEUE.poll();
        }

        return LOG_QUEUE.offer(now);

    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowLog slidingWindowLog = new SlidingWindowLog(5, 5);

        for(int i=1; i<7; i++) {
            System.out.println("Request " + i + (slidingWindowLog.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

        Thread.sleep(5000);

        for(int i=7; i<12; i++) {
            System.out.println("Request " + i + (slidingWindowLog.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }
    }
}