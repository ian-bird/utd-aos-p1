package utd.aos.p1.pusher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Listener<T> implements Pusher<T> {
	private SubscriberManager<T> subs;
	private ServerSocket s;
	private Socket clientSocket;
	private Function<String, T> builder;

	public Listener(Integer port, Function<String, T> builder) throws IOException {
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
				BufferedReader r = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				subs.push(builder.apply(r.readLine()));
			} catch (Exception _ex) {
			}
		}
	}

	private Optional<T> tryToReceive() {
		try {
			if (clientSocket.getInputStream().available() == 0)
				return Optional.empty();

			BufferedReader r = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			return Optional.of(builder.apply(r.readLine()));
		} catch (Exception _ex) {
			return Optional.empty();
		}
	}
}
