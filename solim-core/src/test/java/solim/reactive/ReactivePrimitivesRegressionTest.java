package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.runtime.ReactiveContext;
import solim.test.SolimTestHarness;
import solim.test.TestScheduler;

public class ReactivePrimitivesRegressionTest extends SolimTestHarness {

	// ==========================================
	// Phase 5: Signal Correctness
	// ==========================================

	@Test
	void signalInitialValueGetPeekUpdate() {
		Signal<String> signal = Signal.of("initial");
		assertEquals("initial", signal.get());
		assertEquals("initial", signal.peek());

		signal.update(s -> s + "-updated");
		assertEquals("initial-updated", signal.get());
		assertEquals("initial-updated", signal.peek());
	}

	@Test
	void signalEqualitySuppression() {
		Signal<String> signal = Signal.of("value");
		AtomicInteger subscriberRuns = new AtomicInteger(0);
		Subscription sub = signal.subscribe(v -> subscriberRuns.incrementAndGet());

		// Initial subscription does not eagerly fire callback
		assertEquals(0, subscriberRuns.get());

		// Setting equal value: MUST be suppressed
		signal.set("value");
		signal.set(new String("value"));
		assertEquals(0, subscriberRuns.get(), "Equal values must not trigger notification");

		// Setting new value
		signal.set("new-value");
		assertEquals(1, subscriberRuns.get());

		// Null equality
		Signal<String> nullSignal = Signal.of(null);
		AtomicInteger nullRuns = new AtomicInteger(0);
		Subscription nullSub = nullSignal.subscribe(v -> nullRuns.incrementAndGet());

		nullSignal.set(null);
		assertEquals(0, nullRuns.get(), "Equal nulls must not trigger notification");

		nullSignal.set("not-null");
		assertEquals(1, nullRuns.get());

		nullSignal.set(null);
		assertEquals(2, nullRuns.get());

		nullSignal.set(null);
		assertEquals(2, nullRuns.get());

		sub.dispose();
		nullSub.dispose();
	}

	@Test
	void signalMutationDuringEffectExecution() {
		Signal<Integer> sigA = Signal.of(1);
		Signal<Integer> sigB = Signal.of(100);

		List<Integer> bObserved = new ArrayList<>();

		Effect effectA = Effect.of(() -> {
			int a = sigA.get();
			sigB.set(a * 10);
		});

		Effect effectB = Effect.of(() -> {
			bObserved.add(sigB.get());
		});

		assertEquals(Arrays.asList(10), bObserved);

		sigA.set(2);
		TestScheduler.flush();

		assertEquals(Arrays.asList(10, 20), bObserved);

		effectA.dispose();
		effectB.dispose();
	}

	// ==========================================
	// Phase 6: Computed Correctness
	// ==========================================

	@Test
	void computedLazyMemoizationAndInvalidation() {
		Signal<Integer> count = Signal.of(5);
		AtomicInteger calculations = new AtomicInteger(0);

		Computed<Integer> doubled = Signal.computed(() -> {
			calculations.incrementAndGet();
			return count.get() * 2;
		});

		// Lazy: supplier must not execute until get()
		assertEquals(0, calculations.get());

		assertEquals(10, (int) doubled.get());
		assertEquals(1, calculations.get());

		// Repeated get() returns memoized value without re-evaluation
		assertEquals(10, (int) doubled.get());
		assertEquals(10, (int) doubled.get());
		assertEquals(1, calculations.get());

		// Mutating source invalidates, but does not eagerly recompute
		count.set(6);
		assertEquals(1, calculations.get());

		// Next get() recomputes lazily
		assertEquals(12, (int) doubled.get());
		assertEquals(2, calculations.get());

		doubled.dispose();
	}

	@Test
	void computedDynamicDependencyBranchSwitching() {
		Signal<Boolean> usePrimary = Signal.of(true);
		Signal<String> primary = Signal.of("primary-val");
		Signal<String> fallback = Signal.of("fallback-val");

		AtomicInteger evaluations = new AtomicInteger(0);
		Computed<String> choice = Signal.computed(() -> {
			evaluations.incrementAndGet();
			return usePrimary.get() ? primary.get() : fallback.get();
		});

		assertEquals("primary-val", choice.get());
		assertEquals(1, evaluations.get());

		// Mutating inactive branch does NOT invalidate choice
		fallback.set("new-fallback");
		assertEquals("primary-val", choice.get());
		assertEquals(1, evaluations.get(), "Mutating inactive branch must not re-evaluate");

		// Switch branch
		usePrimary.set(false);
		assertEquals("new-fallback", choice.get());
		assertEquals(2, evaluations.get());

		// Now primary is inactive: mutating primary must not invalidate choice
		primary.set("ignored-primary");
		assertEquals("new-fallback", choice.get());
		assertEquals(2, evaluations.get(), "Former branch must be pruned from dependencies");

		choice.dispose();
	}

	@SuppressWarnings("unchecked")
    @Test
	void computedCycleDetectionDirectAndTransitive() {
		// Direct self-reference
		Computed<Integer>[] self = new Computed[1];
		self[0] = Signal.computed(() -> self[0].get() + 1);

		assertThrows(IllegalStateException.class, () -> self[0].get());

		// Transitive cycle: A -> B -> A
		Computed<Integer>[] a = new Computed[1];
		Computed<Integer>[] b = new Computed[1];
		a[0] = Signal.computed(() -> b[0] != null ? b[0].get() + 1 : 0);
		b[0] = Signal.computed(() -> a[0].get() + 1);

		assertThrows(IllegalStateException.class, () -> a[0].get());
	}

	@Test
	void computedExceptionRestoresReactiveContext() {
		Signal<Integer> sig = Signal.of(42);
		Computed<Integer> faulty = Signal.computed(() -> {
			sig.get();
			throw new RuntimeException("Simulated computed failure");
		});

		assertNull(faulty.get(), "When computed supplier throws, it safely catches, logs, and returns null");

		// Context must not be left dirty
		assertEquals(0, ReactiveContext.size(), "ReactiveContext stack must be clean after exception");
		assertNull(ReactiveContext.current());

		faulty.dispose();
	}

	// ==========================================
	// Phase 7: Effect Lifecycle and Cleanup
	// ==========================================

	@Test
	void effectImmediateExecutionAndDeduplication() {
		Signal<Integer> sig = Signal.of(1);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			sig.get();
		});

		assertEquals(1, runs.get(), "Effect must execute immediately on construction");

		// Mutate multiple times in single batch
		sig.set(2);
		sig.set(3);
		sig.set(4);
		assertEquals(1, TestScheduler.pendingCount(), "Multiple mutations must deduplicate to single queue entry");

		TestScheduler.flush();
		assertEquals(2, runs.get(), "Effect must execute exactly once per flush pass");

		effect.dispose();
	}

	@Test
	void effectDisposedWhileQueuedIsSkipped() {
		Signal<Integer> sig = Signal.of(10);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			sig.get();
		});

		assertEquals(1, runs.get());

		sig.set(20);
		assertEquals(1, TestScheduler.pendingCount());

		// Dispose while queued
		effect.dispose();
		assertTrue(effect.isDisposed());

		TestScheduler.flush();
		assertEquals(1, runs.get(), "Disposed effect queued in dispatcher must be skipped during flush");
	}

	@Test
	void effectMultipleCleanupsRunBeforeRerunAndOnDisposal() {
		Signal<Integer> sig = Signal.of(1);
		List<String> logs = new ArrayList<>();

		Effect effect = Effect.of(cleanup -> {
			int val = sig.get();
			logs.add("run:" + val);
			cleanup.add(() -> logs.add("cleanA:" + val));
			cleanup.add(() -> logs.add("cleanB:" + val));
		});

		assertEquals(Arrays.asList("run:1"), logs);

		sig.set(2);
		TestScheduler.flush();

		assertEquals(
				Arrays.asList("run:1", "cleanA:1", "cleanB:1", "run:2"),
				logs,
				"Cleanups must run before subsequent execution"
		);

		effect.dispose();

		assertEquals(
				Arrays.asList("run:1", "cleanA:1", "cleanB:1", "run:2", "cleanA:2", "cleanB:2"),
				logs,
				"Cleanups must run upon effect disposal"
		);
	}
}
