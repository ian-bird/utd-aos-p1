
public class BufferedChan<T> implements Chan<T> {
    private List<T> queue;
    private SubscriberManager subs;

    BufferedChan() {
        this.queue = new ArrayList<>();
        this.subs = new SubscriberManager<>();
    }

    public synchronized Optional<T> pull() {
        if (queue.length == 0)
            return Optional.empty();
        T first = queue[0];
        queue = Arrays.copyOfRange(queue, 1, queue.length);
        return first;
    }

    public void push(T v) {
        subs.push(v);
        
        synchronized(this) {
            queue.add(v);
        }
    }

    public void registerCallback(Runnable<T> cb) {
        subs.registerCallback(cb);
    }
}
