package utd.aos.p1.map;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import utd.aos.p1.chan.BufferedChan;
import utd.aos.p1.chan.Chan;
import utd.aos.p1.config.MapConfig;
import utd.aos.p1.puller.Puller;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.timer.Timer;

enum Operation {
    MSG_TO_SEND,
    MSG_RECEIVED,
    TIME_UP
}

// This is an implementation of the map protocol that conforms to the channel
// abstraction, allowing
// for testing of components independently of this implementation.
// The protocol is defined as follows:
//
// ‹ While a node is active, it sends anywhere from minPerActive to maxPerActive
// messages, and
// then turns passive. For each message, it makes a uniformly random selection
// of one of its
// neighbors as the destination. Also, if the node stays active after sending a
// message, then it
// waits for at least minSendDelay time units before sending the next message.
// ‹ Only an active node can send a message.
// ‹ A passive node, on receiving a message, becomes active if it has sent fewer
// than maxNumber
// messages (summed over all active intervals). Otherwise, it stays passive.
//
public class MapProtocol<T> implements Chan<T> {
    private Chan<T> inbox;
    private Chan<T> outbox;
    private Timer timer;
    private Puller<Integer> rng;

    private BufferedChan<Operation> workQueue;

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

        this.workQueue = new BufferedChan<Operation>();

        this.sentThisPeriod = 0;
        this.totalSent = 0;
        this.toSend = MapConfig.MIN_PER_ACTIVE + rng.pull().orElseThrow(() -> new RuntimeException())
                            % (MapConfig.MAX_PER_ACTIVE - MapConfig.MIN_PER_ACTIVE + 1);

        // new items from the socket are pushed into the inbox
        this.input.registerCallback((v) -> {
            inbox.push(v);
            workQueue.push(Operation.MSG_RECEIVED);
        });

        // if we start in sleep we need to enter it properly.
        if (init == NodeState.ACTIVE_SLEEP)
            timer.callbackIn(MapConfig.MIN_SEND_DELAY, () -> workQueue.push(Operation.TIME_UP));

        // we get a new message to send, try to deliver it if possible.
        this.outbox.registerCallback((v) -> {
            workQueue.push(Operation.MSG_TO_SEND);
        });

        // set up the state machine.
        workQueue.registerCallback((op) -> {
            workQueue.pull(); // discard this item to keep the buffer empty

            switch (op) {
                case TIME_UP:
                    if (s != NodeState.ACTIVE_SLEEP)
                        break;

                    this.outbox.pull().ifPresentOrElse(this::send, () -> {
                        s = NodeState.ACTIVE_READY;
                    });
                    break;

                case MSG_TO_SEND:
                    if (s != NodeState.ACTIVE_READY)
                        break;

                    send(this.outbox.pull().get());
                    break;

                case MSG_RECEIVED:
                    if (s != NodeState.PASSIVE || totalSent >= MapConfig.MAX_NUMBER)
                        break;

                    sentThisPeriod = 0;
                    toSend = MapConfig.MIN_PER_ACTIVE + rng.pull().orElseThrow(() -> new RuntimeException())
                            % (MapConfig.MAX_PER_ACTIVE - MapConfig.MIN_PER_ACTIVE + 1);
                    s = NodeState.ACTIVE_SLEEP;
                    timer.callbackIn(MapConfig.MIN_SEND_DELAY, () -> workQueue.push(Operation.TIME_UP));
                    break;
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

    // sends a message, update counts, changes state appropriately.
    private void send(T what) {
        int r = rng.pull().get() % outputs.size();
        outputs.get(r).push(what);
        sentThisPeriod++;
        totalSent++;

        if (sentThisPeriod >= toSend) {
            s = NodeState.PASSIVE;
            return;
        }

        s = NodeState.ACTIVE_SLEEP;
        timer.callbackIn(MapConfig.MIN_SEND_DELAY, () -> workQueue.push(Operation.TIME_UP));
    }
}