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

public class ReactiveContext {

    private static volatile ReactiveContext INSTANCE = new ReactiveContext();

    private final Deque<ReactiveObserver> stack = new ArrayDeque<>();

    private int suspendFloor = 0;

    protected ReactiveContext() {
    }

    public static ReactiveContext instance() {
        return INSTANCE;
    }

    public static void install(@Nullable ReactiveContext context) {
        INSTANCE = context != null ? context : new ReactiveContext();
    }

    public static void push(ReactiveObserver observer) {
        SolimAssert.checkMainThread();
        INSTANCE.stack.push(observer);
    }

    public static void pop() {
        INSTANCE.doPop();
    }

    public static ReactiveObserver current() {
        return INSTANCE.doCurrent();
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
        INSTANCE.doClear();
    }

    public static int size() {
        return INSTANCE.stack.size();
    }

    public static <T> T untracked(Prov<T> Prov) {
        return INSTANCE.doUntracked(Prov);
    }

    public static void untracked(Runnable runnable) {
        INSTANCE.doUntracked(runnable);
    }

    protected void doPop() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    protected ReactiveObserver doCurrent() {
        return stack.size() > suspendFloor ? stack.peek() : null;
    }

    protected void doClear() {
        stack.clear();
        suspendFloor = 0;
    }

    protected <T> T doUntracked(Prov<T> Prov) {
        int depth = stack.size();
        int floor = suspendFloor;
        suspendFloor = Math.max(floor, depth);
        try {
            return Prov.get();
        } finally {
            suspendFloor = floor;
            truncate(depth);
        }
    }

    protected void doUntracked(Runnable runnable) {
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

    private void truncate(int depth) {
        while (stack.size() > depth) {
            stack.pop();
        }
    }

    public static void signalCreated(@Nullable Object signal) {
        INSTANCE.onSignalCreated(signal);
    }

    public static void signalWriteStart(@Nullable Object signal, @Nullable Object oldValue,
            @Nullable Object newValue) {
        INSTANCE.onSignalWriteStart(signal, oldValue, newValue);
    }

    public static void signalWriteEnd(@Nullable Object signal) {
        INSTANCE.onSignalWriteEnd(signal);
    }

    public static void computedStart(@Nullable Object computed) {
        INSTANCE.onComputedStart(computed);
    }

    public static void computedEnd(@Nullable Object computed, boolean changed) {
        INSTANCE.onComputedEnd(computed, changed);
    }

    public static void effectStart(@Nullable Object effect) {
        INSTANCE.onEffectStart(effect);
    }

    public static void effectEnd(@Nullable Object effect) {
        INSTANCE.onEffectEnd(effect);
    }

    protected void onSignalCreated(@Nullable Object signal) {
    }

    protected void onSignalWriteStart(@Nullable Object signal, @Nullable Object oldValue,
            @Nullable Object newValue) {
    }

    protected void onSignalWriteEnd(@Nullable Object signal) {
    }

    protected void onComputedStart(@Nullable Object computed) {
    }

    protected void onComputedEnd(@Nullable Object computed, boolean changed) {
    }

    protected void onEffectStart(@Nullable Object effect) {
    }

    protected void onEffectEnd(@Nullable Object effect) {
    }
}
