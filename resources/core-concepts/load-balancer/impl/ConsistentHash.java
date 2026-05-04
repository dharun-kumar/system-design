import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHash {

    private TreeMap<Long, String> hashRing = new TreeMap<>();
    private int numberOfVirtualNodes;

    public ConsistentHash (Set<String> servers, int numberOfVirtualNodes) {
        this.numberOfVirtualNodes = numberOfVirtualNodes;
        for(String server : servers) {
            addServer(server);
        }
    }

    public void addServer(String server) {
        for(int i=0; i<numberOfVirtualNodes; i++) {
            hashRing.put(hash(server + "_" + i), server);
        }
    }

    public void removeServer(String server) {
        for(int i=0; i<numberOfVirtualNodes; i++) {
            hashRing.remove(hash(server + "_" + i));
        }
    }

    public String routeRequest(String request) {
        if(hashRing.isEmpty()) {
            return null;
        }

        Map.Entry<Long, String> ceilEntry = hashRing.ceilingEntry(hash(request));
        return ceilEntry != null ? ceilEntry.getValue() : hashRing.firstEntry().getValue();
    }

    private long hash(String request) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest(request.getBytes());
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static void main(String[] args) {
        Set<String> servers = new HashSet<>(Arrays.asList("ServerA", "ServerB", "ServerC"));
        ConsistentHash consistentHash = new ConsistentHash(servers, 5);

        System.out.println(consistentHash.routeRequest("/api/getUsers"));   //ServerB
        System.out.println(consistentHash.routeRequest("/api/getOrders"));  //ServerA

    }

}
