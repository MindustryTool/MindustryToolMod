package solim.runtime;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import solim.core.OwnershipContext;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.SolimAssert;
import arc.func.Prov;

public final class ReactiveContext {

    private static final Deque<ReactiveObserver> stack = new ArrayDeque<>();

    private static int suspendFloor = 0;

    private ReactiveContext() {
    }

    public static void push(ReactiveObserver observer) {
        SolimAssert.checkMainThread();
        stack.push(observer);
    }

    public static void pop() {
        SolimAssert.checkMainThread();
        stack.pop();
    }

    public static @Nullable ReactiveObserver pop(@Nullable ReactiveObserver expected) {
        SolimAssert.checkMainThread();
        ReactiveObserver popped = stack.isEmpty() ? null : stack.pop();
        SolimAssert.checkLifo(popped, expected);
        return popped;
    }

    public static @Nullable ReactiveObserver current() {
        return stack.size() > suspendFloor ? stack.peek() : null;
    }

    public static void track(@Nullable ReactiveSource observable) {
        ReactiveObserver cur = current();
        if (cur != null && observable != null) {
            SolimAssert.checkMainThread();
            cur.addDependency(observable);
        }
    }

    public static void trackWithWarning(ReactiveSource source, String warning) {
        if (current() == null && OwnershipContext.current() != null) {
            Log.debug(warning);
        }
        track(source);
    }

    public static void clear() {
        stack.clear();
        suspendFloor = 0;
    }

    public static int size() {
        return stack.size();
    }

    public static <T> T untracked(Prov<T> supplier) {
        int depth = stack.size();
        int floor = suspendFloor;
        suspendFloor = Math.max(floor, depth);
        try {
            return supplier.get();
        } finally {
            suspendFloor = floor;
            truncate(depth);
        }
    }

    public static void untracked(Runnable runnable) {
        int depth = stack.size();
        int floor = suspendFloor;
        suspendFloor = Math.max(floor, depth);
        try {
            runnable.run();
        } finally {
            suspendFloor = floor;
            truncate(depth);
        }
    }

    private static void truncate(int depth) {
        while (stack.size() > depth) {
            stack.pop();
        }
    }
}