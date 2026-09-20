package utd.aos.p1.pusher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SubscriberManager<T> implements Pusher<T> {
    private List<Consumer<T>> subscribers;

    public SubscriberManager() {
        this.subscribers = new ArrayList<>();
    }

    public synchronized void push(T v) {
        subscribers.stream().peek((s) -> s.accept(v));
    }

    public synchronized void registerCallback(Consumer<T> cb) {
        subscribers.add(cb);
    }
}
