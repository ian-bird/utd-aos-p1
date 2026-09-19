
public class Sender<T> implements Pusher<T> {
    private Socket clientSocket;
    private List<Consumer<T>> subscribers;
    private PrintWriter out;
    private Function<T, String> toString;
    
    Sender(int ip, int port, Function<T, String> toString) {
	this.subscribers = new ArrayList<Consumer<T>>();
	this.clientSocket = new Socket(ip, port);
	this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public synchronized void push(T v) {
	out.println(v.toString());
	subscribers.map((s) -> s.accept(v));
    }

    public synchronized void registerCallback(Consumer<T> cb) {
	subscribers.add(cb);
    }
}
