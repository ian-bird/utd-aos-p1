private enum NodeState {
    PASSIVE,
    ACTIVE_READY,
    ACTIVE_SLEEP
}

const MIN_SEND_DELAY = 50;

const MAX_PER_ACTIVE = 1;
const MIN_PER_ACTIVE = 1;
const MAX_SENT = 5;

public class MapProtocol<T> implements Chan<T> {
    private Chan<T> inbox;
    private Chan<T> outbox;
    private Timer timer;
    private Puller<Integer> rng;
    private List<(T)->void> subscribers;

    private Pusher<T> input;
    private List<Pusher<T>> outputs;
    private NodeState s;
    
    private int toSend;
    private int sentThisPeriod;
    private int totalSent;
    

    MapProtocol(Pusher<T> i, List<Pusher<T>> o, Timer timer, Puller<Integer> rng, NodeState init) {
        this.inbox = new BufferedChan<T>();
        this.outbox = new BufferedChan<T>();
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
		updateStateReceived();
	    });

	//  we get a new message to send, try to deliver it if possible.
	this.outbox.registerCallback((v) -> synchronized {
		if(s == NodeState.ACTIVE_READY) {
		    o[rng.pull().orElseThrow(()->new RuntimeException()) % o.length()].push(outbox.pull().orElse(v));
		    updateStateSent();
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
    private synchronized void updateStateSent() {
	sentThisPeriod++;
	totalSent++;

	// if this is the limit for what we can send, enter passive mode.
	if(sentThisPeriod >= toSend) {
	    s = NodeState.PASSIVE;
	    return;
	}

	enterSleep();
    }

    // register a callback for when the min send delay has passed.
    // it'll change the state to active ready and then try to deliver a message fi there is one.
    private synchronized void enterSleep(){
	s = NodeState.ACTIVE_SLEEP;

	timer.callbackIn(MIN_SEND_DELAY, () -> synchronized {
		s = NodeState.ACTIVE_READY;
		outbox.pull().map((msg) -> {
			outputs[rng.pull().orElseThrow(() -> new RuntimeException()) % outputs.length].push(msg);
			updateStateSent();
		    });
	    });
    }

	// update state when a message is received
    private synchronized void updateStateReceived() {
	// if passive and less than max sent, switch to active asleep on receiving a message
	if(s != NodeState.PASSIVE || totalSent >= MAX_SENT)
	    return;
	
	// init data for this
	sentThisPeriod = 0;
	toSend = MIN_PER_ACTIVE + rng.pull().orElseThrow(() -> new RuntimeException()) % (MAX_PER_ACTIVE - MIN_PER_ACTIVE + 1);

	enterSleep();
    }
}
