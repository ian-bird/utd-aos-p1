class LogicalTimer implements Timer {
    private PriorityQueue<Pair<int,()->void>> waiting;
    private int simulatedTime;
    
	
    Timer() {
	waiting = new PriorityQueue<>((a,b)->a.getKey()-b.getKey());
	simulatedTime = 0;
    }

    public synchronized boolean emptyP() {
	return waiting.peek() == null;
    }

    public synchronized void force() {
	Pair<int,()->void> p = waiting.poll();
	simulatedTime += p.getKey();
	p.getValue()();
    }
	

    public synchronized void callbackIn(int ms, ()->void cb) {
	waiting.add(new pair<>(ms + simulatedTime, cb));
    }
}

