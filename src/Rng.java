public class Rng implements Puller<Integer> {
    private int currentVal;
    
    public Rng(int seed) {
	this.currentVal = seed;
    }

    public synchronized Optional<Integer> Pull() {
	int result = currentVal;
	
	// linear congruential generator: ANSI C recommended values
	currentVal = (currentVal * 1103515245 + 12345) % (1 << 31);
	
	return Optional.of(result);
    }
}
