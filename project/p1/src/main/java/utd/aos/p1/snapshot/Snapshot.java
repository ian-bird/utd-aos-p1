package utd.aos.p1.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import utd.aos.p1.chan.BufferedChan;
import utd.aos.p1.clock.VectorClock;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.pusher.SubscriberManager;

public class Snapshot<T> {
    public Pusher<T> input;
    public List<Pusher<T>> outputs;

    private List<Pusher<Message<T>>> trueOutputs;
    private int mostRecentRequest;
    private List<Response<T>> inProgressSnapshots;
    private int pid;
    private VectorClock clock;
    private Map<Integer, Pusher<Response<T>>> collectors;
    private List<Integer> neighbors;

    public Snapshot(Pusher<Message<T>> in, List<Pusher<Message<T>>> o, List<Integer> neighbors, int pid,
            int numProcesses) {
        this.mostRecentRequest = 0;
        this.inProgressSnapshots = new ArrayList<>();
        this.clock = new VectorClock(pid, numProcesses);
        this.trueOutputs = o;
        this.pid = pid;
        this.neighbors = neighbors;
        this.collectors = new HashMap<>();

        this.input = new BufferedChan<>();
        in.registerCallback((v) -> {
            switch (v) {
                // when we receive an in-flight message, we need to append it to all the
                // responses that are queued.
                case InFlight<T> m:
                    // record this message for all in progress snapshots.
                    synchronized (this) {
                        for (Response<T> recorder : inProgressSnapshots) {
                            recorder.addMessage(m);
                        }

                        this.clock = this.clock.receive(m.timeSent);
                    }

                    // and then propagate the contents to clients
                    input.push(m.message);
                    break;

                // when we receive a message request we:
                // 1. if its new, we propagate it to everything but the process that sent us the
                // message.
                // then we set up an incomplete response to collect in-flight messages.
                //
                // 2. if its one we've seen before, then we give an empty response to it, so
                // that process knows
                // that we've heard it. It's critical to give an empty response, since giving a
                // populated one
                // would duplicate all the inflight messages that we're aware of when we later
                // pass this on
                // to the process that originally made us aware of the snapshot.
                case Request<T> r:
                    int mostRecent;
                    synchronized (this) {
                        mostRecent = this.mostRecentRequest;
                    }

                    // if this is a new request, start recording for it.
                    if (r.id > mostRecent) {

                        VectorClock c;
                        int mr;
                        synchronized (this) {
                            this.mostRecentRequest = r.id;
                            c = new VectorClock(numProcesses, this.clock.vector);
                            mr = r.id;
                        }

                        // and create the collector for in flight messsages. Note, we're not requesting
                        // from or expecting
                        // a response from the source.
                        synchronized (this) {
                            this.clock = this.clock.receive(r.timeSent);
                            this.inProgressSnapshots.add(new Response<T>(mr, pid, r.source, c,
                                    neighbors.stream().filter((n) -> n != r.source).toList()));
                        }

                        // forward the request to everyone but the person that informed us, and ACK
                        // them.
                        for (int i = 0; i < o.size(); i++) {
                            o.get(i).push(new Acknowledge<>(mr, pid, i));
                            if (neighbors.get(i) == r.source)
                                continue;

                            o.get(i).push(new Request<>(mr, pid, c));
                        }

                    } else {
                        // if it's a request we're aware of, give an empty response.
                        boolean ok = false;
                        for (int i = 0; i < neighbors.size(); i++) {
                            if (neighbors.get(i) == r.source) {
                                VectorClock c;
                                synchronized (this) {
                                    c = new VectorClock(pid, this.clock.vector);
                                }

                                o.get(i).push(new Response<>(r.id, pid, r.source, c, new ArrayList<>()));
                                ok = true;
                                break;
                            }
                        }
                        if (!ok)
                            throw new RuntimeException();
                    }
                    break;

                // when we receive a response, we need to append all the in-flight messages to
                // the appropriate
                // in-progress snapshot, and then check if we've received the final expected
                // response, in which
                // case we can finally respond to our parent.
                case Response<T> res:
                    List<Response<T>> completed;

                    synchronized (this) {
                        inProgressSnapshots.forEach((snap) -> snap.ack(res));

                        completed = inProgressSnapshots.stream().filter(Response::isDone).toList();
                        inProgressSnapshots = inProgressSnapshots.stream().filter((r) -> !r.isDone())
                                .collect(Collectors.toCollection(ArrayList::new));
                    }

                    completed.forEach((r) -> {
                        // if the snapshot request originated here and we've collected everything,
                        // then push it to the appropriate collector and remove it.
                        if (r.sendingTo < 0) {
                            Pusher<Response<T>> p;
                            synchronized (this) {
                                p = this.collectors.get(r.id);
                                this.collectors.remove(r.id);
                            }

                            p.push(r);

                            // otherwise, send it to the observer.
                        } else {
                            for (int i = 0; i < o.size(); i++) {
                                if (neighbors.get(i) == r.sendingTo)
                                    o.get(i).push(r);
                            }
                        }
                    });

                    break;
                case Acknowledge<T> ack:
                    synchronized (this) {
                        inProgressSnapshots.forEach((ss) -> ss.ack(ack));
                    }
                    break;
            }
        });

        this.outputs = new ArrayList<>();
        int i = 0;
        for (Pusher<Message<T>> output : o) {
            int dest = i++;
            Pusher<T> nextOutput = new SubscriberManager<>();
            nextOutput.registerCallback((v) -> {
                synchronized (this) {
                    this.clock = this.clock.send();
                }
                output.push(new InFlight<T>(pid, dest, clock, v));
            });
            this.outputs.add(nextOutput);
        }
    }

    public void capture(Consumer<Response<T>> cb) {
        int num;
        synchronized (this) {
            this.mostRecentRequest++;
            num = this.mostRecentRequest;
        }

        Pusher<Response<T>> collectResult = new SubscriberManager<>();
        collectResult.registerCallback(cb);

        synchronized (this) {
            this.collectors.put(num, collectResult);
            inProgressSnapshots.add(new Response<T>(num, pid, -1, this.clock, this.neighbors));
        }

        // fake broadcast by sending the same timestamp to everyone.
        VectorClock c;
        synchronized (this) {
            c = new VectorClock(this.clock.vector.size(), this.clock.vector);
        }

        for (Pusher<Message<T>> output : this.trueOutputs) {

            output.push(new Request<>(num, this.pid, c));
        }
    }

}
