package utd.aos.p1.puller;

import java.util.Optional;

// Generate an infinite sequence of random numbers.
// This generator takes a seed value and will always produce the same
// sequence for a given seed. This ensures deterministic behavior during testing.
public class Rng implements Puller<Integer> {
    private int currentVal;

    public Rng(int seed) {
        this.currentVal = seed;
    }

    public synchronized Optional<Integer> pull() {
        int result = currentVal;

        // linear congruential generator: ANSI C recommended values
        currentVal = (currentVal * 1103515245 + 12345) % (1 << 31);

        return Optional.of(Math.abs(result));
    }
}
