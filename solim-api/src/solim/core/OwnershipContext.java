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
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        return null;
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
