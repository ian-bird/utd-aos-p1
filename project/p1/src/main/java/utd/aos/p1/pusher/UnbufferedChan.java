package utd.aos.p1.pusher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UnbufferedChan<T> implements Pusher<T> {
    private List<Consumer<T>> subscribers;

    public UnbufferedChan() {
        this.subscribers = new ArrayList<>();
    }

    // every subscriber must be notified before control can continue from here.
    public synchronized void push(T v) {
        for(Consumer<T> subscriber : subscribers)
            subscriber.accept(v);
    }

    public synchronized void registerCallback(Consumer<T> cb) {
        subscribers.add(cb);
    }
}

