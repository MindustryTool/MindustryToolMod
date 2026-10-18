package solim.test;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.core.DisposableOwner;

public final class CollectingOwner implements DisposableOwner {

    private final List<Disposable> collected = new ArrayList<>();
    private boolean disposed = false;

    public List<Disposable> collected() {
        return collected;
    }

    @Override
    public void adopt(@Nullable Disposable resource) {
        if (resource != null && !disposed) {
            collected.add(resource);
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = collected.size() - 1; i >= 0; i--) {
            collected.get(i).dispose();
        }
        collected.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
