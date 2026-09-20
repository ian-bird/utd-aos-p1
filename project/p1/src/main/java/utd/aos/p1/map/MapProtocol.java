package utd.aos.p1.map;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import utd.aos.p1.MapConfig;
import utd.aos.p1.chan.BufferedChan;
import utd.aos.p1.chan.Chan;
import utd.aos.p1.puller.Puller;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.timer.Timer;


public class MapProtocol<T> implements Chan<T> {
	private Chan<T> inbox;
	private Chan<T> outbox;
	private Timer timer;
	private Puller<Integer> rng;

	private Pusher<T> input;
	private List<Pusher<T>> outputs;
	private NodeState s;

	private int toSend;
	private int sentThisPeriod;
	private int totalSent;

	public MapProtocol(Pusher<T> i, List<Pusher<T>> o, Timer timer, Puller<Integer> rng, NodeState init) {
		this.inbox = new BufferedChan<T>();
		this.outbox = new BufferedChan<T>();
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

		// if we start in sleep we need to enter it properly.
		if(init == NodeState.ACTIVE_SLEEP)
			enterSleep();

		// we get a new message to send, try to deliver it if possible.
		this.outbox.registerCallback((v) -> {
			synchronized (this) {
				// if we're ready, then we're waiting for a message to come in. This is it!
				if (s == NodeState.ACTIVE_READY) {
					o.get(rng.pull().orElseThrow(() -> new RuntimeException()) % o.size())
							.push(outbox.pull().orElse(v));
					updateStateSent();
					
					// that element has been consumed; remove it.
					outbox.pull();
				}
			}

		});
	}

	public Optional<T> pull() {
		return inbox.pull();
	}

	public void push(T v) {
		outbox.push(v);
	}

	public void registerCallback(Consumer<T> cb) {
		inbox.registerCallback(cb);
	}

	// update the state when a message is sent
	private synchronized void updateStateSent() {
		sentThisPeriod++;
		totalSent++;

		// if this is the limit for what we can send, enter passive mode.
		if (sentThisPeriod >= toSend) {
			s = NodeState.PASSIVE;
			return;
		}

		enterSleep();
	}

	// register a callback for when the min send delay has passed.
	// it'll change the state to active ready and then try to deliver a message fi
	// there is one.
	private synchronized void enterSleep() {
		s = NodeState.ACTIVE_SLEEP;

		timer.callbackIn(MapConfig.MIN_SEND_DELAY, () -> {
			synchronized (this) {
				s = NodeState.ACTIVE_READY;
				outbox.pull().map((msg) -> {
					outputs.get(rng.pull().orElseThrow(() -> new RuntimeException()) % outputs.size()).push(msg);
					updateStateSent();

					return null;
				});
			}
		});
	}

	// update state when a message is received
	private synchronized void updateStateReceived() {
		// if passive and less than max sent, switch to active asleep on receiving a
		// message
		if (s != NodeState.PASSIVE || totalSent >= MapConfig.MAX_SENT)
			return;

		// init data for this
		sentThisPeriod = 0;
		toSend = MapConfig.MIN_PER_ACTIVE + rng.pull().orElseThrow(() -> new RuntimeException())
				% (MapConfig.MAX_PER_ACTIVE - MapConfig.MIN_PER_ACTIVE + 1);

		enterSleep();
	}
}
