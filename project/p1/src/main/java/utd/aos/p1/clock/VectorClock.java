package utd.aos.p1.clock;

import java.util.ArrayList;
import java.util.List;

import utd.aos.p1.utils.Pair;

public class VectorClock {
    public List<Integer> vector;
    public int associatedProcess;

    public VectorClock(int p, int cardinality) {
        this.associatedProcess = p;

        vector = new ArrayList<>();
        for (int i = 0; i < cardinality; i++) {
            vector.add(0);
        }
    }

    public VectorClock(int p, List<Integer> vector) {
        this.associatedProcess = p;
        this.vector = vector;
    }

    public VectorClock receive(VectorClock c) {
        List<Pair<Integer, Integer>> zipped = new ArrayList<>();

        for (int i = 0; i < vector.size(); i++)
            zipped.add(new Pair<>(vector.get(i), c.vector.get(i)));

        List<Integer> v = zipped.stream().map((p) -> p.getKey() > p.getValue() ? p.getKey() : p.getValue()).toList();
        return new VectorClock(associatedProcess, v).send();
    }

    public VectorClock send() {
        List<Pair<Integer, Integer>> withIndex = new ArrayList<>();

        for (int i = 0; i < vector.size(); i++)
            withIndex.add(new Pair<>(vector.get(i), i));

        List<Integer> v = withIndex.stream().map((p) -> associatedProcess == p.getValue() ? p.getKey() + 1 : p.getKey())
                .toList();
        return new VectorClock(associatedProcess, v);
    }

    public boolean covers(VectorClock c) {
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) >= c.vector.get(i))
                continue;

            if (i != c.associatedProcess)
                return false;

            if (c.vector.get(i) > vector.get(i) + 1)
                return false;
        }

        return true;
    }

    public Comparison compare(VectorClock c) {
        List<Pair<Integer, Integer>> zipped = new ArrayList<>();

        for(int i = 0; i < vector.size(); i++)
            zipped.add(new Pair<Integer,Integer>(vector.get(i), c.vector.get(i)));

        if(zipped.stream().allMatch((p)->p.getKey() == p.getValue()))
            return Comparison.EQUAL;

        if(zipped.stream().allMatch((p)->p.getKey() >= p.getValue()))
            return Comparison.AFTER;

        if(zipped.stream().allMatch((p)->p.getKey() <= p.getValue()))
            return Comparison.BEFORE;

        return Comparison.INCOMPARABLE;
    }

    public String toString() {
        return this.vector.toString();
    }

}
