package utd.aos.p1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import utd.aos.p1.utils.Slurper;

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
    public static void main(String[] _args) throws Exception {
        // testMap(NodeState.ACTIVE_SLEEP, 0);

        // testListenerAndSender();
        
        // testConfig();
    }

    private static void testConfig() throws FileNotFoundException, IOException {
        MapConfig.loadConfig(Slurper.slurp("project/p1/src/main/resources/config.txt"));
        System.out.printf("neighbor 1 of node 4 (should be 0): %d\n", MapConfig.NEIGHBORS.get(4).get(0));
    }

    private static void testListenerAndSender() throws UnknownHostException, IOException, InterruptedException {
        InetAddress ip = InetAddress.getByName("localhost");
        int port = 9006;
        Listener<Integer> l = new Listener<>(port, Integer::parseInt);
        Sender<Integer> s = new Sender<>(ip, port, (i) -> i.toString());
        Phaser p = new Phaser(1);

        l.registerCallback((i) -> {
            System.out.printf("received %d\n", i);
            p.arrive();
        });

        System.out.println("sending 0");
        s.push(0);
        p.register();

        System.out.println("sending 1");
        s.push(1);
        p.register();

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
                int nextAction = rand.pull().orElseThrow(() -> new RuntimeException()) % 3;
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
