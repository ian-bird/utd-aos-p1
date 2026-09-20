package utd.aos.p1.puller;

import java.util.Optional;

public interface Puller<T> {
    Optional<T> pull();
}
