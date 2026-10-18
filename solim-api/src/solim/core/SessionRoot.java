package solim.core;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class SessionRoot implements DisposableOwner {

    private static final SessionRoot INSTANCE = new SessionRoot();

    private final List<Component> children = new ArrayList<>();
    private final List<Disposable> disposables = new ArrayList<>();
    private final Set<Disposable> disposableIndex = Collections
            .newSetFromMap(new IdentityHashMap<Disposable, Boolean>());
    private boolean disposed = false;

    private SessionRoot() {
    }

    public static SessionRoot get() {
        return INSTANCE;
    }

    @Override
    public void adopt(@Nullable Disposable resource) {
        SolimAssert.checkMainThread();
        if (resource == null || disposed) {
            return;
        }
        if (resource instanceof Component) {
            Component child = (Component) resource;
            if (!children.contains(child)) {
                children.add(child);
            }
            return;
        }
        pruneDisposed();
        OwnershipGuard.rejectDuplicate(resource, this);
        if (disposableIndex.add(resource)) {
            disposables.add(resource);
        }
    }

    public boolean remove(@Nullable Component component) {
        SolimAssert.checkMainThread();
        if (component == null) {
            return false;
        }
        return children.remove(component);
    }

    public List<Component> children() {
        return Collections.unmodifiableList(new ArrayList<>(children));
    }

    public List<Disposable> disposables() {
        pruneDisposed();
        return Collections.unmodifiableList(new ArrayList<>(disposables));
    }

    private void pruneDisposed() {
        for (int i = disposables.size() - 1; i >= 0; i--) {
            Disposable resource = disposables.get(i);
            if (resource.isDisposed()) {
                disposables.remove(i);
                disposableIndex.remove(resource);
            }
        }
    }

    public boolean contains(@Nullable Component component) {
        return component != null && children.contains(component);
    }

    public void clear() {
        SolimAssert.checkMainThread();
        children.clear();
        disposables.clear();
        disposableIndex.clear();
    }

    public void resetForTests() {
        clear();
        disposed = false;
    }

    @Override
    public void dispose() {
        SolimAssert.checkMainThread();
        if (disposed) {
            return;
        }
        disposed = true;
        Throwable firstFailure = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            try {
                children.get(i).dispose();
            } catch (Throwable t) {
                if (firstFailure == null) {
                    firstFailure = t;
                }
            }
        }
        children.clear();
        for (int i = disposables.size() - 1; i >= 0; i--) {
            try {
                disposables.get(i).dispose();
            } catch (Throwable t) {
                if (firstFailure == null) {
                    firstFailure = t;
                }
            }
        }
        disposables.clear();
        disposableIndex.clear();
        if (firstFailure != null) {
            if (firstFailure instanceof RuntimeException) {
                throw (RuntimeException) firstFailure;
            }
            if (firstFailure instanceof Error) {
                throw (Error) firstFailure;
            }
            throw new RuntimeException("Error disposing SessionRoot", firstFailure);
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
