class LogicalTimer implements Timer {
    private PriorityQueue<Pair<Int,Runnable>> waiting;
    private int simulatedTime;
    
	
    Timer() {
        waiting = new PriorityQueue<>((a,b)->a.getKey()-b.getKey());
        simulatedTime = 0;
    }

    public synchronized boolean emptyP() {
    	return waiting.peek() == null;
    }

    public synchronized void force() {
        Pair<Int,Runnable> p = waiting.poll();
        simulatedTime += p.getKey();
        p.getValue().run();
    }
	

    public synchronized void callbackIn(int ms, Runnable cb) {
    	waiting.add(new pair<>(ms + simulatedTime, cb));
    }
}

