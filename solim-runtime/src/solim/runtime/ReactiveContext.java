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

/**
 * Stack-based dependency tracking for single-threaded UI. No ThreadLocal by design.
 *
 * <p>The tracking stack itself is the only state the base implementation owns; all static entry
 * points delegate to a singleton instance so a profiling-enabled subclass can be installed without
 * the base class carrying any monitoring logic. The lifecycle hooks
 * ({@link #onSignalCreated}, {@link #onSignalWriteStart}, {@link #onSignalWriteEnd},
 * {@link #onComputedStart}, {@link #onComputedEnd}, {@link #onEffectStart}, {@link #onEffectEnd})
 * are empty here and cost nothing; {@link DebugReactiveContext} overrides them.
 */
public class ReactiveContext {

	private static volatile ReactiveContext INSTANCE = new ReactiveContext();

	private final Deque<ReactiveObserver> stack = new ArrayDeque<>();
	/**
	 * Depth at which tracking is suspended: observers at or below it are invisible to
	 * {@link #current()}, observers pushed above it (inside the untracked block) still collect.
	 * Zero when nothing is suspended.
	 */
	private int suspendFloor = 0;

	protected ReactiveContext() {}

	/** Returns the active singleton instance. */
	public static ReactiveContext instance() {
		return INSTANCE;
	}

	/** Installs the active singleton instance, falling back to a fresh base instance for {@code null}. */
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

	/**
	 * Returns the observer currently collecting dependencies, or {@code null} when none is
	 * evaluating or every observer present was suspended by an enclosing {@code untracked} block.
	 */
	public static ReactiveObserver current() {
		return INSTANCE.doCurrent();
	}

	/**
	 * Called from reactive sources to register dependency with current observer.
	 *
	 * <p>The thread check is deliberately inside the registration branch rather than at the top:
	 * a background read that finds no active observer records nothing and is therefore harmless,
	 * while a background read that attaches itself to an observer mid-evaluation corrupts the
	 * tracking stack and must fail.
	 */
	public static void track(@Nullable ReactiveSource observable) {
		ReactiveObserver cur = current();
		if (cur != null && observable != null) {
			SolimAssert.checkMainThread();
			cur.addDependency(observable);
		}
	}

	/**
	 * Shared tracking path for reactive reads. Emits a build-time warning when called
	 * during {@code build()} without an active reactive context, then tracks the source.
	 */
	public static void trackWithWarning(ReactiveSource source, String warning) {
		if (current() == null && OwnershipContext.current() != null) {
			Log.debug(warning);
		}
		track(source);
	}

	/** For tests: clear stack */
	public static void clear() {
		INSTANCE.doClear();
	}

	/** For tests: size */
	public static int size() {
		return INSTANCE.stack.size();
	}

	/**
	 * Temporarily suspends active dependency tracking while executing the Prov.
	 *
	 * <p>Suspending raises a floor rather than emptying the stack, so reads are still attributed
	 * to any observer pushed <em>inside</em> the block — a component built inside a factory runs
	 * its own effects there, and those effects must still track their own sources. Restoration is
	 * by recorded depth rather than by copying the observer stack, so a block that pushes an
	 * observer without popping it still leaves the stack at its entry depth.
	 */
	public static <T> T untracked(Prov<T> Prov) {
		return INSTANCE.doUntracked(Prov);
	}

	/** Temporarily suspends active dependency tracking while executing the runnable. */
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

	/** Pops observers until the stack is exactly {@code depth} deep. */
	private void truncate(int depth) {
		while (stack.size() > depth) {
			stack.pop();
		}
	}

	// --- reactive lifecycle hooks, delegated to the installed instance --------------------------------
	//
	// The static entry points below are what the reactive primitives call. They exist separately from
	// the protected hooks so the same name is not overloaded across static and instance scope.

	/** Records that a {@code Signal} was constructed while this context is installed. */
	public static void signalCreated(@Nullable Object signal) {
		INSTANCE.onSignalCreated(signal);
	}

	/** Records the start of a signal write, before observers are notified. */
	public static void signalWriteStart(@Nullable Object signal, @Nullable Object oldValue,
			@Nullable Object newValue) {
		INSTANCE.onSignalWriteStart(signal, oldValue, newValue);
	}

	/** Records the end of a signal write, after observers have been notified. */
	public static void signalWriteEnd(@Nullable Object signal) {
		INSTANCE.onSignalWriteEnd(signal);
	}

	/** Records the start of a computed evaluation. */
	public static void computedStart(@Nullable Object computed) {
		INSTANCE.onComputedStart(computed);
	}

	/**
	 * Records the end of a computed evaluation.
	 *
	 * @param changed whether the recomputed value differed from the previously cached one
	 */
	public static void computedEnd(@Nullable Object computed, boolean changed) {
		INSTANCE.onComputedEnd(computed, changed);
	}

	/** Records the start of an effect body run. */
	public static void effectStart(@Nullable Object effect) {
		INSTANCE.onEffectStart(effect);
	}

	/** Records the end of an effect body run. */
	public static void effectEnd(@Nullable Object effect) {
		INSTANCE.onEffectEnd(effect);
	}

	/** Called once when a {@code Signal} is constructed, if it is constructed while this context is installed. */
	protected void onSignalCreated(@Nullable Object signal) {
	}

	/** Called immediately before a signal writes a new value, before observers are notified. */
	protected void onSignalWriteStart(@Nullable Object signal, @Nullable Object oldValue,
			@Nullable Object newValue) {
	}

	/** Called after a signal write has finished notifying its observers. */
	protected void onSignalWriteEnd(@Nullable Object signal) {
	}

	/** Called immediately before a computed evaluates its producer. */
	protected void onComputedStart(@Nullable Object computed) {
	}

	/**
	 * Called after a computed finishes evaluating.
	 *
	 * @param changed whether the recomputed value differed from the previous cached value
	 */
	protected void onComputedEnd(@Nullable Object computed, boolean changed) {
	}

	/** Called immediately before an effect body runs. */
	protected void onEffectStart(@Nullable Object effect) {
	}

	/** Called after an effect body finishes. */
	protected void onEffectEnd(@Nullable Object effect) {
	}
}