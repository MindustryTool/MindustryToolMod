package solim.core;

import java.util.List;

public interface DebugReactiveNode {

    String debugLabel();

    String debugValue();

    List<ReactiveSource> debugDependencies();

    List<ReactiveObserver> debugObservers();

    int debugListenerCount();

    boolean debugDirty();
}
