package utd.aos.p1.pusher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// core functionality of notifying clients of a new value on push.
// this is widely used and must be synchronized.
//
// placing the code here means there cannot be forgotten synchronize keywords
// elsewhere in the program.
public class SubscriberManager<T> implements Pusher<T> {
    private List<Consumer<T>> subscribers;

    public SubscriberManager() {
        this.subscribers = new ArrayList<>();
    }

    public void push(T v) {
        List<Consumer<T>> subs;

        // atomically get the subs
        synchronized(this) {
            subs = subscribers;
        }

        for(Consumer<T> subscriber : subs)
            subscriber.accept(v);
    }

    public synchronized void registerCallback(Consumer<T> cb) {
        subscribers.add(cb);
    }
}
