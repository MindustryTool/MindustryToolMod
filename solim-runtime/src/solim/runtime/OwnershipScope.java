package solim.runtime;

import arc.func.Prov;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;
import solim.core.SolimAssert;

public final class OwnershipScope implements DisposableOwner {

    private static final int MAX_POOLED_BUFFERS = 32;

    private static final Deque<OwnershipScope> pool = new ArrayDeque<>();

    private @Nullable List<Disposable> collected;
    private boolean closed = false;

    private OwnershipScope() {
    }

    public static OwnershipScope open() {
        SolimAssert.checkMainThread();
        OwnershipScope scope = pool.pollFirst();
        if (scope == null) {
            scope = new OwnershipScope();
        }
        scope.closed = false;
        OwnershipContext.push(scope);
        return scope;
    }

    public static <T extends Component> T build(Prov<T> supplier) {
        OwnershipScope scope = open();
        try {
            T created = ReactiveContext.untracked(() -> AttachmentStack.isolate(() -> {
                T component = supplier.get();
                if (component != null) {
                    component.element();
                }
                return component;
            }));
            scope.adoptInto(created);
            return created;
        } finally {
            scope.close();
        }
    }

    @Override
    public void adopt(Disposable disposable) {
        if (disposable == null || closed) {
            return;
        }
        List<Disposable> collected = this.collected;
        if (collected == null) {
            collected = new ArrayList<>();
            this.collected = collected;
        }
        collected.add(disposable);
    }

    public void adoptInto(@Nullable DisposableOwner owner) {
        List<Disposable> collected = this.collected;
        if (collected == null || owner == null) {
            return;
        }
        Iterator<Disposable> iterator = collected.iterator();
        while (iterator.hasNext()) {
            Disposable disposable = iterator.next();
            iterator.remove();
            if (disposable != owner) {
                owner.adopt(disposable);
            }
        }
    }

    public void extractComponents(List<Component> roots) {
        List<Disposable> collected = this.collected;
        if (collected == null) {
            return;
        }
        Iterator<Disposable> iterator = collected.iterator();
        while (iterator.hasNext()) {
            Disposable disposable = iterator.next();
            if (disposable instanceof Component) {
                roots.add((Component) disposable);
                iterator.remove();
            }
        }
    }

    public void close() {
        SolimAssert.checkMainThread();
        if (closed) {
            return;
        }
        closed = true;
        OwnershipContext.pop(this);
        List<Disposable> collected = this.collected;
        this.collected = null;
        if (collected != null) {
            for (int i = collected.size() - 1; i >= 0; i--) {
                try {
                    collected.get(i).dispose();
                } catch (Throwable t) {
                    Log.err("Error disposing unadopted resource from ownership scope", t);
                }
            }
            if (collected.size() <= MAX_POOLED_BUFFERS) {
                collected.clear();
                pool.push(this);
            }
        }
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public boolean isDisposed() {
        return closed;
    }
}