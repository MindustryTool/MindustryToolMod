package solim.core;

public interface ReactiveObserver {

    void addDependency(ReactiveSource observable);

    void invalidate();
}
