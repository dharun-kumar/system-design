import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class SlidingWindowCounter {

    private final long WINDOW_SIZE_IN_SECONDS;
    private final long MAX_REQUESTS_PER_WINDOW;
    private long currentWindowStart;
    private long currentWindowCount;
    private long previousWindowCount;

    public SlidingWindowCounter(long windowSizeInSeconds, long maxRequestsPerWindow) {

        WINDOW_SIZE_IN_SECONDS = windowSizeInSeconds;
        MAX_REQUESTS_PER_WINDOW = maxRequestsPerWindow;
        currentWindowCount = 0;
        previousWindowCount = 0;
        currentWindowStart = System.currentTimeMillis();

    }

    public synchronized boolean canProcessRequest() {

        long now = System.currentTimeMillis();
        long secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(now - currentWindowStart);

        if(secondsElapsed >= WINDOW_SIZE_IN_SECONDS) {
            previousWindowCount = currentWindowCount;
            currentWindowCount = 0;
            currentWindowStart = now;
            secondsElapsed = 0;
        }

        double weight = (double) (WINDOW_SIZE_IN_SECONDS - secondsElapsed) / WINDOW_SIZE_IN_SECONDS;
        double estimatedCount = currentWindowCount +  (previousWindowCount * weight);
        if(estimatedCount < MAX_REQUESTS_PER_WINDOW) {
            currentWindowCount++;
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowCounter slidingWindowCounter = new SlidingWindowCounter(5, 5);

        for(int i=1; i<7; i++) {
            System.out.println("Request " + i + (slidingWindowCounter.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

        Thread.sleep(5000);

        for(int i=7; i<12; i++) {
            System.out.println("Request " + i + (slidingWindowCounter.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

    }
}