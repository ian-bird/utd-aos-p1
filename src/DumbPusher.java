public class DumbPusher<T> {
    private List<(T)->void> subscribers;

    DumbPusher<T>() {
	this.subscribers = Arrays.new<>();
    }

    public synchronized void push(T v) {
	subscribers.stream().map((s)->s(v));
    }

    public synchronized void registerCallback((T)->void cb) {
	subscribers.add(cb);
    }
}
