package utd.aos.p1.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utd.aos.p1.clock.VectorClock;

public final class Response<T> implements Message<T> {
    public List<InFlight<T>> inFlightMessages;
    public int id;
    public int source;
    public int sendingTo;
    public Map<Integer, VectorClock> states;
    private List<Integer> waitingFor;

    public Response(int id, int source, int sendingTo, VectorClock state, List<Integer> waitingFor) {
        this.id = id;
        this.source = source;
        this.sendingTo = sendingTo;
        this.inFlightMessages = new ArrayList<>();
        this.waitingFor = waitingFor;
        this.states = new HashMap<>();
        this.states.put(source, state);
    }

    public void addMessage(InFlight<T> message) {
        synchronized (this) {
            inFlightMessages.add(message);
        }
    }

    public boolean isDone() {
        return waitingFor.isEmpty();
    }

    // if this is a response that we're waiting for, add all the new info we've
    // received
    // and then remove it from the list of processes we're waiting to hear from.
    public synchronized void ack(Response<T> r) {
        if (r.id != this.id)
            return;

        if (!waitingFor.contains(r.source))
            return;

        this.waitingFor = this.waitingFor.stream().filter((i) -> i != r.source).toList();

        states.putAll(r.states);
        inFlightMessages.addAll(r.inFlightMessages);
    }
}
