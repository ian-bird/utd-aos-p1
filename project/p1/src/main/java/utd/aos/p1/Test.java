package utd.aos.p1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

import utd.aos.p1.config.MapConfig;
import utd.aos.p1.map.MapProtocol;
import utd.aos.p1.map.NodeState;
import utd.aos.p1.puller.Rng;
import utd.aos.p1.pusher.Listener;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.pusher.Sender;
import utd.aos.p1.pusher.SubscriberManager;
import utd.aos.p1.timer.LogicalTimer;
import utd.aos.p1.timer.SystemTimer;
import utd.aos.p1.timer.Timer;
import utd.aos.p1.utils.FileUtil;
import utd.aos.p1.utils.Pair;

class Incrementable {
    private int i;

    public Incrementable(int i) {
        this.i = i;
    }

    public synchronized Incrementable incf() {
        i++;
        return this;
    }

    public synchronized int get() {
        return i;
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        // testMap(NodeState.ACTIVE_SLEEP, 3);

        // testListenerAndSender();

        // testConfig();

        // System.out.println("logical clock:");
        // logicalIntegrationTest(2);
        // System.out.println("wall clock:");
        //wallClockIntegrationTest(2);
        try {
        fullLocalIntegrationTest(Integer.parseInt(args[0]));
        } catch (Exception _e) {
            System.out.println("encounted fatal error.");
        } finally {
            System.out.println("exiting.");
        }
    }

    private static void fullLocalIntegrationTest(int myNodeNum) throws IOException, InterruptedException {
        MapConfig.loadConfig(FileUtil.slurp("testconfig.txt"));

		// set up my input channel
        String vName = MapConfig.ADDRESSES_BY_NODE_NUM.get(myNodeNum).getKey();
		int port = MapConfig.NODE_AND_PORT_BY_HOST.get(vName).getValue();
		Listener<Integer> incoming = new Listener<>(port, Integer::parseInt);

		// set up my output channels
		List<Pusher<Integer>> outgoing = new ArrayList<>();
		for (int neighborNode : MapConfig.NEIGHBORS.get(myNodeNum)) {
			Pair<String, Integer> p = MapConfig.ADDRESSES_BY_NODE_NUM.get(neighborNode);

            int retries = 0;
            while(true) {
                try {
                    Sender<Integer> s = new Sender<>(InetAddress.getByName("localhost"), p.getValue(), (i) -> i.toString());                    
        			outgoing.add(s);
                    break;
                } catch (IOException ie) {
                    if(retries++ > 10) {
                        System.out.printf("failed to acquire socket for node %\n", neighborNode);
                        throw ie;
                    }
                    Thread.sleep(100);
                }
            }
		}

		// set up the protocol
		MapProtocol<Integer> proto = new MapProtocol<Integer>(incoming, outgoing, new SystemTimer(),
				new Rng((int) System.currentTimeMillis()), myNodeNum == 0 ? NodeState.ACTIVE_SLEEP : NodeState.PASSIVE);

		// register a callback to log when we receive stuff
		proto.registerCallback((i) -> {
                if(i % 1000 + 1 == MapConfig.MAX_NUMBER)
				    System.out.printf("%d finished\n", i / 1000);
		});

		// saturate the queue of stuff to send out
		for (int i = 0; i < MapConfig.MAX_NUMBER; i++)
			proto.push(myNodeNum * 1000 + i);

		
		// wait until we're killed externally
		Thread.sleep(5_000);
    }

    private static void wallClockIntegrationTest(int rngSeed)
            throws FileNotFoundException, IOException, RuntimeException {
        // load the config
        MapConfig.loadConfig(FileUtil.slurp("config.txt"));

        // rng
        Rng rng = new Rng(rngSeed);

        // set up the channels and timers
        List<Pusher<Integer>> inputChannels = new ArrayList<>();
        List<Timer> timers = new ArrayList<>();
        for (int i = 0; i < MapConfig.NUM_NODES; i++) {
            inputChannels.add(new SubscriberManager<>());
            timers.add(new SystemTimer());
        }

        List<MapProtocol<Integer>> nodes = new ArrayList<>();
        for (int i = 0; i < MapConfig.NUM_NODES; i++) {
            // outputs to its neighbors
            List<Pusher<Integer>> outputs = MapConfig.NEIGHBORS.get(i).stream().map((n) -> inputChannels.get(n))
                    .toList();

            MapProtocol<Integer> p = new MapProtocol<>(inputChannels.get(i), outputs, timers.get(i), rng,
                    i == 0 ? NodeState.ACTIVE_SLEEP : NodeState.PASSIVE);

            // fill the mailbox
            for (int j = 0; j < MapConfig.MAX_NUMBER; j++)
                p.push(i * 100 + j);

            p.registerCallback((v) -> {
                if (v % 100 + 1 == MapConfig.MAX_NUMBER)
                    System.out.printf("node %d completed.\n", v / 100);
            });

            nodes.add(p);
        }

        try {
            Thread.sleep(3_500);
        } catch (InterruptedException _ie) {
        }
    }

    private static void logicalIntegrationTest(int rngSeed)
            throws FileNotFoundException, IOException, RuntimeException {
        // load the config
        MapConfig.loadConfig(FileUtil.slurp("project/p1/src/main/resources/config.txt"));

        // rng
        Rng rng = new Rng(rngSeed);

        // set up the channels and timers
        List<Pusher<Integer>> inputChannels = new ArrayList<>();
        List<LogicalTimer> timers = new ArrayList<>();
        for (int i = 0; i < MapConfig.NUM_NODES; i++) {
            inputChannels.add(new SubscriberManager<>());
            timers.add(new LogicalTimer());
        }

        List<MapProtocol<Integer>> nodes = new ArrayList<>();
        for (int i = 0; i < MapConfig.NUM_NODES; i++) {
            // outputs to its neighbors
            List<Pusher<Integer>> outputs = MapConfig.NEIGHBORS.get(i).stream().map((n) -> inputChannels.get(n))
                    .toList();

            MapProtocol<Integer> p = new MapProtocol<>(inputChannels.get(i), outputs, timers.get(i), rng,
                    i == 0 ? NodeState.ACTIVE_SLEEP : NodeState.PASSIVE);

            // fill the mailbox
            for (int j = 0; j < MapConfig.MAX_NUMBER; j++)
                p.push(i * 100 + j);

            p.registerCallback((v) -> {
                if (v % 100 + 1 == MapConfig.MAX_NUMBER)
                    System.out.printf("node %d completed.\n", v / 100);
            });

            nodes.add(p);
        }

        for (int i = 0; i < 300; i++) {
            LogicalTimer t = timers.get(rng.pull().get() % timers.size());
            if (t.emptyP())
                continue;
            t.force();
        }
    }

    private static void testConfig() throws FileNotFoundException, IOException {
        MapConfig.loadConfig(FileUtil.slurp("project/p1/src/main/resources/config.txt"));
        System.out.printf("neighbor 1 of node 4 (should be 0): %d\n", MapConfig.NEIGHBORS.get(4).get(0));
    }

    private static void testListenerAndSender() throws UnknownHostException, IOException, InterruptedException {
        InetAddress ip = InetAddress.getByName("localhost");
        int port = 9006;
        Listener<Integer> l = new Listener<>(port, Integer::parseInt);
        Sender<Integer> s = new Sender<>(ip, port, (i) -> i.toString());
        Phaser p = new Phaser(3);

        l.registerCallback((i) -> {
            System.out.printf("received %d\n", i);
            p.arrive();
        });

        System.out.println("sending 0");
        s.push(0);

        System.out.println("sending 1");
        s.push(1);

        p.arriveAndAwaitAdvance();

        System.out.println("done");
    }

    // unit test for the map protocol. Uses deterministic rng, a deterministic
    // logical clock and a mocked socket to enable consistent, isolated testing.
    private static void testMap(NodeState initialState, int rngSeed) throws RuntimeException {
        List<Integer> actions = new ArrayList<Integer>();

        Incrementable numDelivered = new Incrementable(0);
        int numSentToInbox = 0;
        int numSocketPushed = 0;
        Incrementable numReceived = new Incrementable(0);

        LogicalTimer timer = new LogicalTimer();
        Rng rand = new Rng(rngSeed);

        Pusher<Integer> input = new SubscriberManager<>();

        List<Pusher<Integer>> outputs = new ArrayList<>();
        outputs.add(new SubscriberManager<>());

        outputs.getFirst().registerCallback((i) -> {
            if (i != numDelivered.get()) {
                System.out.printf("%dth sent value was not expected: %d\n", numDelivered.get(), i);
                throw new RuntimeException();
            }
            System.out.printf("sent %d\n", i);
            numDelivered.incf();
        });

        MapProtocol<Integer> mp = new MapProtocol<>(input, outputs, timer, rand, initialState);

        int numIterations = 0;

        try {
            while (true) {
                int nextAction = rand.pull().get() % 3;
                actions.add(nextAction);

                switch (nextAction) {
                    case 0:
                        // advance time if possible.
                        if (timer.emptyP())
                            break;

                        System.out.println("advancing time");
                        timer.force();
                        break;
                    case 1:
                        // push another value to send.
                        System.out.printf("requested to send %d\n", numSentToInbox);
                        mp.push(numSentToInbox++);
                        break;
                    case 2:
                        // push another value into the virtual socket.
                        input.push(numSocketPushed++);
                        break;
                }

                mp.pull().map((n) -> {
                    if (n != numReceived.get()) {
                        System.out.printf("%dth received value was not expected: %d\n", numReceived.get(), n);
                        throw new RuntimeException();
                    }
                    System.out.printf("received %d\n", n);
                    numReceived.incf();
                    return n;
                });

                if (numDelivered.get() == MapConfig.MAX_NUMBER)
                    return;

                if (++numIterations > MapConfig.MAX_NUMBER * 10) {
                    System.out.printf(
                            "mp appears to have stalled. successfully delivered %d and received %d messages.\n",
                            numDelivered.get(), numReceived.get());
                    throw new RuntimeException();
                }
            }
        } catch (RuntimeException re) {
            System.out.print("action sequence: [");

            for (int action : actions)
                System.out.printf("%d, ", action);

            System.out.print("]\n");
            throw re;
        }
    }
}
