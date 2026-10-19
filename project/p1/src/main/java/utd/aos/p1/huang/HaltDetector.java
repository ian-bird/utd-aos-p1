package utd.aos.p1.huang;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import utd.aos.p1.chan.BufferedChan;
import utd.aos.p1.map.NodeState;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.utils.Pair;

public class HaltDetector<T> {
    private Rational state;
    private List<Pusher<Pair<Rational, Optional<T>>>> outputs;
    private boolean source;
    private Runnable onHalt;

    public List<Pusher<T>> outs;
    public Pusher<T> in;

    public Supplier<NodeState> clientState;

    public HaltDetector(List<Pusher<Pair<Rational, Optional<T>>>> outputs, Pusher<Pair<Rational, Optional<T>>> input,
            boolean source, Runnable onHalt) {
        state = new Rational(source ? 1 : 0, 1);

        this.in = new BufferedChan<>();
        this.onHalt = onHalt;
        this.outputs = outputs;

        input.registerCallback((p) -> {
            p.getValue().ifPresent(in::push);

            if(clientState.get() == NodeState.PASSIVE && !source){
                outputs.get(0).push(new Pair<>(p.getKey(), Optional.empty()));
            } else {
                Rational stateVal;
                synchronized (this) {
                    state = state.add(p.getKey());
                    stateVal = state;
                }

                if (stateVal.numerator.intValue() == 1 && stateVal.denominator.intValue() == 1)
                    onHalt.run();
            }
        });

        this.outs = new ArrayList<>();
        for (var output : outputs) {
            var nextOutput = new BufferedChan<T>();
            nextOutput.registerCallback((v) -> {
                Rational half;
                nextOutput.pull();
                synchronized (this) {
                    half = state.multiply(new Rational(1, 2));
                    state = half;
                }
                output.push(new Pair<>(half, Optional.of(v)));
            });
            this.outs.add(nextOutput);
        }
    }

    public void setSleepSource(Pusher<Object> sleepSource) {
        if (source) {
            sleepSource.registerCallback((_v) -> {
                boolean isDone;
                synchronized (this) {
                    isDone = state.numerator.intValue() == 1 && state.denominator.intValue() == 1;
                }
                if (isDone)
                    onHalt.run();
            });
        } else {
            sleepSource.registerCallback((_v) -> {
                Rational s;
                synchronized(this) {
                    s = state;
                    state = new Rational(0, 1);
                }
                outputs.get(0).push(new Pair<>(s, Optional.empty()));
            });
        }
    }
}
