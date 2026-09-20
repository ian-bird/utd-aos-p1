package utd.aos.p1.pusher;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Function;

// abstracts a TCP socket to the pusher interface.
// this one sends values to a specific host and port.
public class Sender<T> implements Pusher<T> {
    private Socket clientSocket;
    private SubscriberManager<T> subs;
    private PrintWriter out;
    private Function<T, String> toString;

    // toString allows for custom serialization protocols for any object specified by the user.
    public Sender(InetAddress ip, int port, Function<T, String> toString) throws IOException {
        this.subs = new SubscriberManager<>();
        this.clientSocket = new Socket(ip, port);
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.toString = toString;
    }

    public void push(T v) {
        out.println(toString.apply(v));
        subs.push(v);
    }

    public void registerCallback(Consumer<T> cb) {
        subs.registerCallback(cb);
    }
}
