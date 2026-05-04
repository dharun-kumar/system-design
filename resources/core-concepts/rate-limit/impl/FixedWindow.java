import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class FixedWindow {

    private final long WINDOW_SIZE_IN_SECONDS;
    private final long MAX_REQUESTS_PER_WINDOW;
    private long counter;
    private long currentWindowStart;

    public FixedWindow(long windowSizeInSeconds, long maxRequestsPerWindow) {
        WINDOW_SIZE_IN_SECONDS = windowSizeInSeconds;
        MAX_REQUESTS_PER_WINDOW = maxRequestsPerWindow;
        currentWindowStart = System.currentTimeMillis();
        counter = 0;
    }

    public synchronized boolean canProcessRequest() {

        long now = System.currentTimeMillis();
        long secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(now - currentWindowStart);

        if(secondsElapsed >= WINDOW_SIZE_IN_SECONDS) {
            currentWindowStart = now;
            counter = 0;
        }

        if(counter < MAX_REQUESTS_PER_WINDOW) {
            counter++;
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        FixedWindow fixedWindow = new FixedWindow(5, 5);

        for(int i=1; i<7; i++) {
            System.out.println("Request " + i + (fixedWindow.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

        Thread.sleep(5000);

        for(int i=7; i<12; i++) {
            System.out.println("Request " + i + (fixedWindow.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

    }

}