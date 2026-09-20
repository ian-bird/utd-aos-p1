import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import pusher.SubscriberManager;

public class Listener<T> implements Pusher<T> {
	private SubscriberManager subs;
	private ServerSocket s;
	private Socket clientSocket;
	private Function<String, T> builder;

	public Listener(Integer port, Function<String, T> builder) {
		this.subs = new SubscriberManager<>();
		this.builder = builder;
		this.s = new ServerSocket(port); // need to assign this to the obj so it'll close
		this.clientSocket = s.accept();

		CompletableFuture.runAsync(this::listen);
	}

	public void push(T _v) {
	}

	public void registerCallback(Consumer<T> cb) {
		subs.registerCallback(cb);
	}

	private void listen() {
		while (true) {
			try {
				subs.push(builder
						.apply(new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readline()));
			} catch (_ex Exception) {
			}
		}
	}

	private Optional<T> tryToReceive() {
		if (clientSocket.getInputStream().available() == 0)
			return Optional.empty();

		try {
			return Optional.of(
					builder.apply(new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readline()));
		} catch (_ex Exception) {
			return Optional.empty();
		}
	}
}
