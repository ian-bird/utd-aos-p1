package utd.aos.p1.timer;

import java.util.PriorityQueue;

import utd.aos.p1.utils.Pair;

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
