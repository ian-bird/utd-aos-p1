package utd.aos.p1.timer;

// the timer interface. Timers expose one method, that allows a client to register a lambda
// to be performed after a certain amount of time. The caller providdes the delay,
// and the timer calls it when the appropriate amount of time has passed.
// the timer should support an arbitrary number of tasks waiting to run concurrently.
public interface Timer {
    public void callbackIn(int time, Runnable cb);
}
