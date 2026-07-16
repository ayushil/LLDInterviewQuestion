import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class SingleQueuePubSub {
    private static final int QUEUE_SIZE = 32;

    private final BlockingQueue<Message> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    private final Thread consumer = new Thread(this::consumeLoop, "pubsub-consumer");
    private volatile boolean running = true;

    public SingleQueuePubSub() {
        consumer.setDaemon(true);
        consumer.start();
    }

    public void subscribe(Subscriber subscriber) {
        subscribers.add(Objects.requireNonNull(subscriber, "subscriber"));
    }

    public void subscribe(Subscriber a, Subscriber b) {
        subscribe(a);
        subscribe(b);
    }

    public void unSubscribe(int idx) {
        subscribers.remove(idx);
    }

    public void publish(String text) throws InterruptedException {
        queue.put(new Message(nextId.getAndIncrement(), text));
    }

    private void consumeLoop() {
        while (running) {
            try {
                Message m = queue.take();
                for (Subscriber s : subscribers) s.onMessage(m);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() throws InterruptedException {
        running = false;
        consumer.interrupt();
        consumer.join(1000);
    }

    public static void main(String[] args) throws Exception {
        SingleQueuePubSub bus = new SingleQueuePubSub();

        bus.subscribe(new LoggingSubscriber("Alice"));
        bus.subscribe(new LoggingSubscriber("Bob"));

        bus.publish("hello");
        bus.publish("world");
        bus.publish("from single queue");

        Thread.sleep(300);
        bus.subscribe(new LoggingSubscriber("Cat"));
        bus.publish("next message");
        Thread.sleep(300);
        bus.unSubscribe(1);
        bus.publish("next message 2");
        bus.stop();
    }
}

final class Message {
    private final long id;
    private final String text;

    Message(long id, String text) {
        this.id = id;
        this.text = text;
    }

    String getText() { return text; }

    @Override
    public String toString() { return "[" + id + "] " + text; }
}

@FunctionalInterface
interface Subscriber {
    void onMessage(Message message);
}

final class LoggingSubscriber implements Subscriber {
    private final String name;

    LoggingSubscriber(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(name + " consumed: " + message.getText());
    }
}