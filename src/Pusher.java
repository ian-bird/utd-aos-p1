interface Pusher<T> {
    void Push(T v);

    void registerCallback(Consumer<T> cb);
}
