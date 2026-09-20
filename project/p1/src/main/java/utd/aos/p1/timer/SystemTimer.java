package utd.aos.p1.timer;

import java.util.concurrent.CompletableFuture;

public class SystemTimer implements Timer {
   public SystemTimer() {
    }

    public void callbackIn(int ms, Runnable cb) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(ms);
                cb.run();
            } catch (InterruptedException _ex) {
                System.err.println("sleep was interrupted.\n");
            }
        });
    }
}
