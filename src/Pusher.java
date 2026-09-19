interface Pusher<T> {
    void Push(T);
    void registerCallback((T)->void);
}
