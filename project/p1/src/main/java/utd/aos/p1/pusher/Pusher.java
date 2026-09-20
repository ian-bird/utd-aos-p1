package utd.aos.p1.pusher;

import java.util.function.Consumer;

// abstracts anything that can have values pushed into it.
// upon receiving a value, the pusher will notify everything subscribed to it
// that a new value has been pushed into it.
public interface Pusher<T> {
    void push(T v);

    void registerCallback(Consumer<T> cb);
}
