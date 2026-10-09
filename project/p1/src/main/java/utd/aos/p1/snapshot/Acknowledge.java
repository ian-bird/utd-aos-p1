package utd.aos.p1.snapshot;

public final class Acknowledge<T> implements Message<T> {
    public int id;
     public int source;
    public int dest;

    public Acknowledge(int id, int source, int dest) {
        this.id = id;
        this.source = source;
        this.dest = dest;
    }
}
