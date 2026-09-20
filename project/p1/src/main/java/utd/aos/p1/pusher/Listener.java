package utd.aos.p1.pusher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

// abstracts a TCP socket to the pusher interface.
// This one listens to the socket and sends values to its callback when they're received.
// the push value is unoperative.
public class Listener<T> implements Pusher<T> {
	private SubscriberManager<T> subs;
	private ServerSocket s;
	private Socket clientSocket;
	private Function<String, T> builder;

	// creates a new listener on the specified port. the builder allows
	// objects to be returned from the listener rather than just strings.
	public Listener(Integer port, Function<String, T> builder) throws IOException {
		this.subs = new SubscriberManager<>();
		this.builder = builder;
		this.s = new ServerSocket(port); // need to assign this to the obj so it'll close
		
		CompletableFuture.runAsync(()->{
			try {
				this.clientSocket = s.accept();
				this.listen();
			} catch(Exception ex) {
				System.err.println("failed to open socket");
				System.exit(1);
			}
		});
	}

	public void push(T _v) {
	}

	public void registerCallback(Consumer<T> cb) {
		subs.registerCallback(cb);
	}

	// utility function. Infinite loop that waits on the socket and runs in a promise that will never return.
	private void listen() {
		while (true) {
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				subs.push(builder.apply(r.readLine()));
			} catch (Exception _ex) {
			}
		}
	}
}
