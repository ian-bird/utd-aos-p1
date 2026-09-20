import pusher.SubscriberManager;

public class Sender<T> implements Pusher<T> {
    private Socket clientSocket;
    private SubscriberManager<T> subs;
    private PrintWriter out;
    private Function<T, String> toString;

    Sender(int ip, int port, Function<T, String> toString) {
        this.subs = new SubscriberManager();
        this.clientSocket = new Socket(ip, port);
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public void push(T v) {
        out.println(v.toString());
        subs.push(v);
    }

    public void registerCallback(Consumer<T> cb) {
        subs.registerCallback(cb);
    }
}
