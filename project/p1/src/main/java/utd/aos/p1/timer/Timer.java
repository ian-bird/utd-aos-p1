package utd.aos.p1.timer;

public interface Timer {
    public void callbackIn(int time, Runnable cb);
}
