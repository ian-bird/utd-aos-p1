
public class BufferedChan<T> implements Chan<T> {
    private List<T> queue;
    private List<(T)->void> subscribers;

    BufferedChan() {
	this.queue = Arrays.new<T>();
	this.subscribers = Arrays.new<(T)->void>();
    }

    public synchronized Optional<T> pull() {
	if(queue.length == 0)
	    return Optional.empty();
	T first = queue[0];
	queue = Arrays.copyOfRange(queue, 1, queue.length);
	return first;
    }

    public synchronized void push(T v) {
	queue.add(v);
	subscribers.map((s) -> s(v));
    }

    public synchronized void registerCallback((T)->void cb) {
	subscribers.add(cb);
    }
}

    
	    
