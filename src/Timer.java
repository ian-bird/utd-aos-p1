class Timer {
    private List<()->void> jobs;
    private List<Integer> restartTimes;

    Timer() {
	jobs = Arrays.new<()->void>();
	restartTimes = Arrays.new<Integer>;
    }

    public void callbackIn(int ms, ()->void cb) {
	jobs.add(cb);
	restartTimes.add(ms + System.currentTimeMillis());
    }

    public void attend() {
	int t = System.currentTimeMillis();

	List<()->void> incompleteJobs = Arrays.new<()->void>();
	List<Integer> nextTimes = Arrays.new<Integer>;
	for(int i = 0; i < restartTimes.length; i++) {
	    if(t < restartTimes[i]) {
		incompleteJobs.add(jobs[i]);
		nextTimes.add(restartTimes[i]);
	    } else {
		jobs[i]();
	    }
	}
	jobs = incompleteJobs;
	restartTimes = nextTimes;
    }
}
