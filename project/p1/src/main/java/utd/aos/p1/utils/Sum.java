package utd.aos.p1.utils;

import java.util.Optional;
import java.util.function.Consumer;

public class Sum<A,B> {
    protected Optional<A> a;
    protected Optional<B> b;

    public Sum(A a) {
        this.a = Optional.of(a);
        this.b = Optional.empty();
    }

    protected Sum() {
        this.a = Optional.empty();
        this.b = Optional.empty();
    }

    public Sum<A,B> setA(A a) {
        return new Sum<>(a);
    }

    public Sum<A,B> setB(B b) {
        Sum<A,B> result = new Sum<>();
        result.b = Optional.of(b);
        return result;
    }

    public void unwrap(Consumer<A> acon, Consumer<B> bcon) {
        a.ifPresent(acon);
        b.ifPresent(bcon);
    }




}
