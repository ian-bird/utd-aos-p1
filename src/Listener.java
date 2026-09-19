
public class Listener<T implements FromStringer> implements Pusher<T> {
    private List<(T)->void> subscribers;
    private ServerSocket serverSocket;
    
    Listener<T>(Integer port) {
	this.subscribers = Arrays.new<(T)->void>();
        serverSocket = new ServerSocket(port);
    }

    public void push(T v) {
    }

    public void registerCallback((T)->void cb) {
	subscribers.add(cb);
    }

    public void attend() {
	tryToReceive().map((v) -> subscribers.map((s) -> s(v)));
    }

    private Optional<T> tryToReceive() {
	if(!clientSocket.getInputStream().available())
	    return Optional.empty();
	
	in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	String msg = in.readline();
	
	try {
	    return Optional.of(T::fromString(msg));
	} catch (_ex Exception) {
	    return Optional.empty();
	}
    }
}
