class SystemTimer implements Timer {
    Timer() {
    }

    public void callbackIn(int ms, ()->void cb) {
	CompletableFuture::runAsync(()->{
		System.sleep(ms);
		cb();
	    });
    }
}

