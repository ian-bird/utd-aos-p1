package utd.aos.p1.snapshot;

public sealed interface Message<T> permits InFlight, Request, Response {
}