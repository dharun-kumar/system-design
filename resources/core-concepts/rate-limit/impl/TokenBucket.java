import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class TokenBucket {

    private final long MAX_TOKENS;
    private final long REFILL_RATE;
    private long tokensAvailable;
    private long lastRefillMs;

    public TokenBucket(long maxTokens, long refillsPerSecond) {
        MAX_TOKENS = maxTokens;
        REFILL_RATE = refillsPerSecond;
        tokensAvailable = maxTokens;
        lastRefillMs = System.currentTimeMillis();
    }

    public synchronized boolean canProcessRequest() {
        refillTokens();
        if(tokensAvailable > 0) {
            tokensAvailable -= 1;
            return true;
        }
        return false;
    }

    private void refillTokens() {
        long now = System.currentTimeMillis();
        long tokensToRefill = REFILL_RATE * TimeUnit.MILLISECONDS.toSeconds(now - lastRefillMs);
        if(tokensToRefill > 0) {
            tokensAvailable = Math.min(MAX_TOKENS, tokensAvailable + tokensToRefill);
            lastRefillMs = now;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(5, 1);

        for(int i=1; i<7; i++) {
            System.out.println("Request " + i + (tokenBucket.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }

        Thread.sleep(5000);

        for(int i=7; i<12; i++) {
            System.out.println("Request " + i + (tokenBucket.canProcessRequest() ? " 200 OK " : " 429 Too Many Requests ") + LocalTime.now().withNano(0));
        }
    }

}
