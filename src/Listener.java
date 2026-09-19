import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Listener<T> implements Pusher<T> {
	private List<Consumer<T>> subscribers;
	private ServerSocket serverSocket;
	private Function<String, T> builder;

	Listener(Integer port, Function<String, T> builder) {
		this.subscribers = new ArrayList<Consumer<T>>();
		this.builder = builder;
		serverSocket = new ServerSocket(port);
		CompletableFuture.runAsync(this::listen);
	}

	public synchronized void push(T _v) {
	}

	public synchronized void registerCallback(Consumer<T> cb) {
		subscribers.add(cb);
	}

	private void listen() {
		while (true) {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String msg = in.readline();

			try {
				T fromSocket = builder.apply(msg);
				synchronized (this) {
					subscribers.stream().peek((s) -> s.accept(fromSocket));
				}
			} catch (_ex Exception) {
			}
		}
	}

	private Optional<T> tryToReceive() {
		if (!clientSocket.getInputStream().available())
			return Optional.empty();

		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String msg = in.readline();

		try {
			return Optional.of(builder.apply(msg));
		} catch (_ex Exception) {
			return Optional.empty();
		}
	}
}
