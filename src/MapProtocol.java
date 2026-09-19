private enum NodeState {
    PASSIVE,
    ACTIVE_READY,
    ACTIVE_SLEEP
}

public class MapProtocol<T> implements Chan<T> {
    private Chan<T> inbox;
    private Chan<T> outbox;
    private Timer timer;
    private Rng rng;
    private List<(T)->void> subscribers;

    private Pusher<T> input;
    private List<Pusher<T>> outputs;
    private NodeState s;
    
    private int toSend;
    private int sentThisPeriod;
    private int totalSent;
    

    MapProtocol(Pusher<T> i, List<Pusher<T>> o, Timer timer, Rng rng, NodeState init) {
        this.inbox = Arrays.new<T>();
        this.outbox = Arrays.new<T>();
	this.subscribers = Arrays.new<(T)->void>();
	this.timer = timer;
	this.rng = rng;
	this.input = i;
	this.outputs = o;
	this.s = init;

	this.sentThisPeriod = 0;
	this.totalSent = 0;

	// new items from the socket are pushed into the inbox
	this.input.registerCallback((v) -> {
		inbox.push(v);
		update_state_received();
	    });

	//  we get a new message to send, try to deliver it if possible.
	this.outbox.registerCallback((v) -> {
		if(s == ACTIVE_READY) {
		    o[rng.pull().orElseThrow(()->new RuntimeException()) % o.length()].push(outbox.pull().orElse(v));
		    update_state_sent();
		} 
	    });	
    }

    public Optional<T> pull() {
	return inbox.pull();
    }

    public void push(T v) {
	outbox.push(v);
    }

    public void registerCallback((T)->void cb) {
	inbox.registerCallback(cb);
    }

    // update the state when a message is sent
    private void update_state_sent() {
	sentThisPeriod++;
	totalSent++;

	// if this is the limit for what we can send, enter passive mode.
	if(sentThisPeriod >= toSend)
	    s = PASSIVE;
	else {
	    s = ACTIVE_SLEEP;

	    timer.callbackIn(MIN_SEND_DELAY, () -> {
		    s = ACTIVE_READY;
		    outbox.pull().map((msg) -> {
			    outputs[rng.pull().orElseThrow(() -> new RuntimeException()) % outputs.length].push(msg);
			    update_state_sent();
			});
		});
	}
    }

	// update state when a message is received
    private void update_state_received() {
	// if passive and less than max sent, switch to active asleep on receiving a message
	if(s == PASSIVE && totalSent < MAX_SENT) {
	    // init data for this
	    sentThisPeriod = 0;
	    toSend = MIN_PER_ACTIVE + rng.pull().orElseThrow(() -> new RuntimeException()) % (MAX_PER_ACTIVE - MIN_PER_ACTIVE + 1);
	    s = ACTIVE_SLEEP;

	    // register a callback for when the min send delay has passed.
	    // it'll change the state to active ready and then try to deliver a message fi there is one.
	    timer.callbackIn(MIN_SEND_DELAY, () -> {
		    s = ACTIVE_READY;
		    outbox.pull().map((msg) -> {
			    outputs[rng.pull().orElseThrow(() -> new RuntimeException()) % outputs.length].push(msg);
			    update_state_sent();
			});
		});
	}
    }
}
