public class DumbPusher<T> implements Pusher<T> {
    private List<Consumer<T>> subscribers;

    DumbPusher() {
        this.subscribers = new ArrayList<>();
    }

    public synchronized void push(T v) {
        subscribers.stream().map((s) -> s.accept(v));
    }

    public synchronized void registerCallback(Consumer<T> cb) {
        subscribers.add(cb);
    }
}
