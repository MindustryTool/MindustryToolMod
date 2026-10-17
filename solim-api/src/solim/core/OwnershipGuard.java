package solim.core;

import arc.util.Nullable;

import java.util.Collections;

/**
 * Detects a resource that is already held by a live owner.
 *
 * <p>The check walks the ownership tree from {@link SessionRoot}. There is no registry of
 * adoptions to maintain and invalidate: the tree itself is the single source of truth, so a
 * disposed owner automatically stops hiding anything. The walk descends only through
 * {@link Component} children and the resources they hold; a {@code Disposable} exposes no
 * children, so the reconciler transfer path in {@code OwnershipScope.adoptInto} — whose
 * intermediate owner is a plain {@code Disposable} — is never treated as a second owner.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    /**
     * Returns the live owner currently holding {@code resource}, or {@code null} when nothing
     * holds it.
     */
    public static @Nullable DisposableOwner findHolder(@Nullable Disposable resource) {
        if (resource == null) {
            return null;
        }
        return search(SessionRoot.get(), resource, 0);
    }

    private static @Nullable DisposableOwner search(DisposableOwner owner, Disposable resource,
            int depth) {
        if (depth > 64 || owner == resource) {
            return null;
        }
        if (holds(owner, resource)) {
            return owner;
        }
        for (Component child : childrenOf(owner)) {
            DisposableOwner found = search(child, resource, depth + 1);
            if (found != null) {
                return found;
            }
        }
        for (Disposable held : disposablesOf(owner)) {
            if (held == resource) {
                return owner;
            }
        }
        return null;
    }

    private static boolean holds(DisposableOwner owner, Disposable resource) {
        for (Disposable held : disposablesOf(owner)) {
            if (held == resource) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<Component> childrenOf(DisposableOwner owner) {
        return owner instanceof Component ? ((Component) owner).children()
                : owner instanceof SessionRoot ? ((SessionRoot) owner).children()
                : Collections.<Component>emptyList();
    }

    private static Iterable<Disposable> disposablesOf(DisposableOwner owner) {
        if (owner instanceof Component) {
            return ((Component) owner).disposables();
        }
        if (owner instanceof SessionRoot) {
            return ((SessionRoot) owner).disposables();
        }
        return Collections.<Disposable>emptyList();
    }

    public static void rejectDuplicate(@Nullable Disposable resource, DisposableOwner newOwner) {
        if (resource == null) {
            return;
        }
        DisposableOwner holder = findHolder(resource);
        if (holder == null || holder == newOwner) {
            return;
        }
        throw new IllegalStateException(resource.getClass().getSimpleName()
                + " is already held by " + describe(holder)
                + " and cannot also be owned by " + describe(newOwner));
    }

    private static String describe(DisposableOwner owner) {
        if (owner instanceof SessionRoot) {
            return "SessionRoot";
        }
        if (owner instanceof Component) {
            return owner.getClass().getSimpleName();
        }
        return owner.toString();
    }
}