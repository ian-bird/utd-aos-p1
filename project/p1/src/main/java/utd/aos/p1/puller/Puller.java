package utd.aos.p1.puller;

import java.util.Optional;

// abstract interface for anything that can have values pulled from it.
public interface Puller<T> {
    Optional<T> pull();
}
