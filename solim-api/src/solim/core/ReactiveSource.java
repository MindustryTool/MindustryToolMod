package solim.core;

public interface ReactiveSource {

    void addObserver(ReactiveObserver observer);

    void removeObserver(ReactiveObserver observer);
}
