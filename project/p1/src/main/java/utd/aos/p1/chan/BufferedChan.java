package utd.aos.p1.chan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import utd.aos.p1.pusher.SubscriberManager;

public class BufferedChan<T> implements Chan<T> {
    private List<T> queue;
    private SubscriberManager<T> subs;

    public BufferedChan() {
        this.queue = new ArrayList<>();
        this.subs = new SubscriberManager<>();
    }

    public synchronized Optional<T> pull() {
        if (queue.isEmpty())
            return Optional.empty();

        T first = queue.getFirst();
        queue.removeFirst();

        return Optional.of(first);
    }

    public void push(T v) {
        synchronized(this) {
            queue.add(v);
        }
        
        subs.push(v);
    }

    public void registerCallback(Consumer<T> cb) {
        subs.registerCallback(cb);
    }
}
