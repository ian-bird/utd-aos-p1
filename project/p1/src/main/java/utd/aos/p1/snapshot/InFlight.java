package utd.aos.p1.snapshot;

import utd.aos.p1.clock.VectorClock;

public final class InFlight<T> implements Message<T> {
    public int source;
    public int dest;
    public VectorClock timeSent;
    public T message;

    public InFlight(int source, int dest, VectorClock timeSent, T message) {
        this.source = source;
        this.dest = dest;
        this.timeSent = timeSent;
        this.message = message;
    }

    public String toString() {
        return Integer.valueOf(source).toString() + " -> " + Integer.valueOf(dest).toString() + ": " + timeSent.toString();
    }
}
