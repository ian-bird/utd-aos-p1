package utd.aos.p1.chan;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

public class UnbufferedChan<T> implements Chan<T> {
	private SynchronousQueue<T> q;
	public UnbufferedChan() {
		q = new SynchronousQueue<>();
	}

	public void push(T v) {
		try {
			q.put(v);
		} catch (InterruptedException _e) {
		}
	}

	public Optional<T> pull() {
		try {
			return Optional.of(q.take());
		} catch (InterruptedException _e) {
			return Optional.empty();
		}
	}

	public void registerCallback(Consumer<T> cb) {
		CompletableFuture.runAsync(()->{
			while(true) {
				cb.accept(pull().get());
			}
		});
	}
}
