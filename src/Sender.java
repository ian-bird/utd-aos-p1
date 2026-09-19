
public class Sender<T implements ToStringer> implements Pusher<T> {
    private Socket clientSocket;
    private List<(T)->void> subscribers;
    private PrintWriter out;
    
    Sender(int ip, int port) {
	this.subscribers = Arrays.new<(T)->void>();
	this.clientSocket = new Socket(ip, port);
	this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public void push(T v) {
	out.println(v.toString());
	subscribers.map((s) -> s(v));
    }

    public void registerCallback((T)->void cb) {
	subscribers.add(cb);
    }
}
