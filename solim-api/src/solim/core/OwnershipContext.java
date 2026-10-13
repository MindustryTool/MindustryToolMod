package solim.core;

import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public final class OwnershipContext {
    private static final Deque<DisposableOwner> stack = new ArrayDeque<>();

    private OwnershipContext() {
    }

    public static void push(@Nullable DisposableOwner owner) {
        SolimAssert.checkMainThread();
        if (owner != null) {
            stack.push(owner);
        }
    }

    public static @Nullable DisposableOwner pop() {
        SolimAssert.checkMainThread();
        return stack.isEmpty() ? null : stack.pop();
    }

    public static @Nullable DisposableOwner pop(@Nullable DisposableOwner expected) {
        SolimAssert.checkMainThread();
        DisposableOwner popped = stack.isEmpty() ? null : stack.pop();
        SolimAssert.checkLifo(popped, expected);
        return popped;
    }

    public static @Nullable DisposableOwner current() {
        return stack.peek();
    }

    public static void clear() {
        stack.clear();
    }

    public static int size() {
        return stack.size();
    }

    public static <T extends Disposable> T register(@Nullable T disposable) {
        SolimAssert.checkMainThread();
        if (disposable == null) {
            return null;
        }
        DisposableOwner current = stack.peek();
        if (current != null) {
            current.adopt(disposable);
        }
        return disposable;
    }

    public static <T extends Component> T registerChild(@Nullable T child) {
        SolimAssert.checkMainThread();
        if (child == null) {
            return null;
        }
        DisposableOwner current = stack.peek();
        if (current != null) {
            current.adopt(child);
        } else {
            SessionRoot.get().adopt(child);
        }
        return child;
    }
}
