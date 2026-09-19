import java.util.Optional;

interface Chan<T> {
    Optional<T> pull();

    void push(T t);

    void registerCallback(Consumer<T> cb);
}
