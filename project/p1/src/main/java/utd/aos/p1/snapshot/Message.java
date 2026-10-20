package utd.aos.p1.snapshot;

import java.io.Serializable;

public sealed interface Message<T>  extends Serializable permits InFlight, Request, Response, Acknowledge {
}