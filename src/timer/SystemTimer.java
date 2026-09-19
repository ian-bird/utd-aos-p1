class SystemTimer implements Timer {
    Timer() {
    }

    public void callbackIn(int ms, Runnable cb) {
        CompletableFuture.runAsync(()->{
            System.sleep(ms);
                cb.run();
            });
    }
}

