package utd.aos.p1.pusher;

import java.util.function.Consumer;

public interface Pusher<T> {
    void push(T v);

    void registerCallback(Consumer<T> cb);
}
