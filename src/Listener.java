
public class Listener<T implements FromStringer> implements Pusher<T> {
    private List<(T)->void> subscribers;
    private ServerSocket serverSocket;
    
    Listener<T>(Integer port) {
	this.subscribers = Arrays.new<(T)->void>();
        serverSocket = new ServerSocket(port);
	CompletableFuture::runAsync(this::listen);
    }

    public synchronized void push(T _v) {
    }

    public synchronized void registerCallback((T)->void cb) {
	subscribers.add(cb);
    }

    private void listen() {
	while(true) {
	    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	    String msg = in.readline();
	
	    try {
		T fromSocket = T::fromString(msg);
		synchronized {
		    subscribers.stream().peek((s)->s(fromSocket));
		}
		
	    } catch (_ex Exception) {
	    }
	}
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
