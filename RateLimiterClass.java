import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

enum ClientType {
    NORMAL,
    PREMIUM
}

public class RateLimiterClass {
    public static void main(String[] args) {
        RateLimiter rateLimiter = RateLimiter.getInstance();
        int limit = 10; // requests per second
        Client firstClient = new Client(1, ClientType.NORMAL, rateLimiter);
        Client secondClient  = new Client(2, ClientType.PREMIUM, rateLimiter);
        rateLimiter.registerClient(firstClient, limit);
        rateLimiter.registerClient(secondClient, limit);

        while(true) {
            firstClient.makeRequest();
            secondClient.makeRequest();
        }
    }
}

class Client {
    public int clientId;
    public ClientType clientType;
    RateLimiter rateLimiter;

    public Client(int clientId, ClientType clientType, RateLimiter rateLimiter)
    {
        this.clientId = clientId;
        this.clientType = clientType;
        this.rateLimiter = rateLimiter;
    }

    public boolean makeRequest() {
        boolean success = rateLimiter.sendRequest(clientId);
        if (success) {
            System.out.println("SUCCESS " + clientId + " " + System.currentTimeMillis());
        } else {
//            System.out.println("LIMIT REACHED. TRY AGAIN LATER. " + clientId);
        }
        return success;
    }

}

/*
* SLIDING WINDOW
* TUMBLING WINDOW
* LEAKY BUCKET
* */
interface RateLimiterStrategy {
    public boolean serveRequest(int clientId);
    public void registerClient(Client c, int limit);
    public void resetLimit();
}

class ResetLimit implements Runnable {
    TumblingWindowStrategy tumblingWindowStrategy;
    public ResetLimit(TumblingWindowStrategy tumblingWindowStrategy) {
        this.tumblingWindowStrategy = tumblingWindowStrategy;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                tumblingWindowStrategy.resetLimit();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

class TumblingWindowStrategy implements RateLimiterStrategy {
    private final Map<Integer, Integer> clientRequestLimits = new HashMap<>();
    private final Map<Integer, PriorityQueue<Long>> clientRequests = new HashMap<>();
    private final Map<Integer, ReentrantLock> clientLocks = new HashMap<>();
    private final ReentrantLock registerLock = new ReentrantLock();

    public TumblingWindowStrategy() {
        Thread t = new Thread(new ResetLimit(this));
        t.setDaemon(true);
        t.start();
    }

    private ReentrantLock lockForClient(int clientId) {
        return clientLocks.get(clientId);
    }

    public boolean serveRequest(int clientId) {
        ReentrantLock clientLock = lockForClient(clientId);
        if (clientLock == null) {
            return false;
        }

        clientLock.lock();
        try {
            PriorityQueue<Long> requests = clientRequests.get(clientId);
            if (requests == null) {
                return false;
            }

            long now = System.currentTimeMillis();
            while (!requests.isEmpty() && now - requests.peek() > 1000) {
                requests.poll();
            }

            int limit = clientRequestLimits.get(clientId);
            if (requests.size() >= limit) {
                return false;
            }

            requests.add(now);
            return true;
        } finally {
            clientLock.unlock();
        }
    }

    public void registerClient(Client c, int limit) {
        registerLock.lock();
        try {
            if (c.clientType == ClientType.PREMIUM) {
                limit *= 2;
            }
            clientRequestLimits.put(c.clientId, limit);
            clientRequests.put(c.clientId, new PriorityQueue<>());
            clientLocks.put(c.clientId, new ReentrantLock());
        } finally {
            registerLock.unlock();
        }
    }

    public void resetLimit() {
        for (int clientId : new ArrayList<>(clientLocks.keySet())) {
            ReentrantLock clientLock = lockForClient(clientId);
            if (clientLock == null) {
                continue;
            }
            clientLock.lock();
            try {
                PriorityQueue<Long> requests = clientRequests.get(clientId);
                if (requests != null) {
                    requests.clear();
                }
            } finally {
                clientLock.unlock();
            }
        }
    }
}

class RateLimiter {
    public static RateLimiter rateLimiter;
    public static ReentrantLock lock = new ReentrantLock();
    public RateLimiterStrategy rateLimiterStrategy;

    private RateLimiter() {
        this.rateLimiterStrategy = new TumblingWindowStrategy();
    }

    public static RateLimiter getInstance() {
        if (rateLimiter == null) {
            lock.lock();
            try {
                if (rateLimiter == null) {
                    rateLimiter = new RateLimiter();
                }
            } finally {
                lock.unlock();
            }
        }
        return rateLimiter;
    }

    public void registerClient(Client c, int limit) {
        this.rateLimiterStrategy.registerClient(c, limit);
    }

    public boolean sendRequest(int clientId) {
        return this.rateLimiterStrategy.serveRequest(clientId);
    }
}

