package utd.aos.p1.timer;

import java.util.PriorityQueue;

import utd.aos.p1.utils.Pair;

// a logical timer is a deterministic model of a timer that works without reference to the system clock.
// it tracks when each incoming task needs to run and does callbacks in the order of earliest to latest.
//
// it keeps an internal logical time that is incremented whenever there is a callback waiting for time to pass.
public class LogicalTimer implements Timer {
    private PriorityQueue<Pair<Integer, Runnable>> waiting;
    private int simulatedTime;

    public LogicalTimer() {
        waiting = new PriorityQueue<>((a, b) -> a.getKey() - b.getKey());
        simulatedTime = 0;
    }

    public synchronized boolean emptyP() {
        return waiting.peek() == null;
    }

    public synchronized void force() {
        Pair<Integer, Runnable> p = waiting.poll();
        simulatedTime += p.getKey();
        p.getValue().run();
    }

    public synchronized void callbackIn(int ms, Runnable cb) {
        waiting.add(new Pair<>(ms + simulatedTime, cb));
    }
}
