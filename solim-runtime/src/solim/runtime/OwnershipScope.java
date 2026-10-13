package solim.runtime;

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

/**
 * Collects every {@link Disposable} registered through {@link OwnershipContext}
 * while the scope
 * is open, regardless of its type, and either transfers them to an owner or
 * destroys them.
 *
 * <p>
 * This replaces clearing the {@code OwnershipContext} stack to "suspend"
 * ownership. Clearing
 * the stack discarded whatever a factory registered; a scope keeps it and
 * decides what happens to
 * it. Usage is always the same shape:
 *
 * <pre>{@code
 * OwnershipScope scope = OwnershipScope.open();
 * try {
 *     C created = factory.get(item);
 *     scope.adoptInto(created);
 * } finally {
 *     scope.close();
 * }
 * }</pre>
 *
 * <p>
 * {@code close()} is strict: anything collected and not adopted is disposed. A
 * block that
 * throws therefore never leaves live resources behind.
 *
 * <p>
 * Instances are pooled and the collection list is allocated lazily, so opening
 * a scope on a
 * hot path (an effect body) allocates nothing once the pool is warm.
 */
public final class OwnershipScope implements DisposableOwner {

    private static final Deque<OwnershipScope> pool = new ArrayDeque<>();

    private @Nullable List<Disposable> collected;
    private boolean closed = false;

    private OwnershipScope() {
    }

    public static OwnershipScope open() {
        OwnershipScope scope = pool.pollFirst();
        if (scope == null) {
            scope = new OwnershipScope();
        }
        scope.closed = false;
        OwnershipContext.push(scope);
        return scope;
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

    /**
     * Transfers everything collected so far to {@code owner}.
     *
     * <p>
     * The owner is never transferred into itself. Resources are left unadopted when
     * {@code owner} is {@code null}, which means {@link #close()} destroys them.
     */
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

    /**
     * Closes the scope: restores the previous registrar and disposes every
     * collected resource
     * that was not adopted. Idempotent, and always safe to call from a
     * {@code finally} block.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        OwnershipContext.pop();
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
            collected.clear();
        }
        pool.push(this);
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
