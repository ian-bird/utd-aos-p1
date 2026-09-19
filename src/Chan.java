interface Chan<T> {
    Optional<T> pull();
    void push(T);
    void registerCallback((T)->void);
}
