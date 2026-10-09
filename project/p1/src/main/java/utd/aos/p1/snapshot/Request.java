package utd.aos.p1.snapshot;

import utd.aos.p1.clock.VectorClock;

public final class Request<T> implements Message<T> {
    public int id;
    public int source;
    public VectorClock timeSent;

    public Request(int id, int source, VectorClock timeSent) {
        this.id = id;
        this.source = source;
        this.timeSent = timeSent;
    }
}
