package solim.reactive;

import static org.junit.jupiter.api.Assertions.*;

import arc.Events;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.game.EventType.Trigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import java.util.ArrayList;
import java.util.List;
import solim.test.SolimEnv;

class SignalDispatcherTest extends SolimEnv {

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void multipleChangesToSingleSignalExecutesEffectOnceOnFlush() {
		// Test A: Multiple changes -> one execution
		Signal<Integer> signal = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);
		AtomicInteger lastValue = new AtomicInteger(-1);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			lastValue.set(signal.get());
		});

		assertEquals(1, runs.get());
		assertEquals(0, lastValue.get());
		runs.set(0);

		signal.set(1);
		signal.set(2);
		signal.set(3);

		// Before flush: effect is queued and has not re-executed yet
		assertEquals(0, runs.get());
		assertEquals(1, SignalDispatcher.size());

		SignalDispatcher.flush();

		// After flush: executed exactly once with latest value
		assertEquals(1, runs.get());
		assertEquals(3, lastValue.get());
		assertEquals(0, SignalDispatcher.size());

		effect.dispose();
	}

	@Test
	void multipleDependenciesChangedInOneFrameExecutesEffectOnceOnFlush() {
		// Test B: Multiple dependencies -> one execution
		Signal<Integer> sigA = Signal.of(1);
		Signal<Integer> sigB = Signal.of(2);
		Signal<Integer> sigC = Signal.of(3);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			sigA.get();
			sigB.get();
			sigC.get();
		});

		assertEquals(1, runs.get());
		runs.set(0);

		sigA.set(10);
		sigB.set(20);
		sigC.set(30);

		assertEquals(0, runs.get());
		assertEquals(1, SignalDispatcher.size());

		SignalDispatcher.flush();

		assertEquals(1, runs.get());
		assertEquals(0, SignalDispatcher.size());

		effect.dispose();
	}

	@Test
	void signalChangesAcrossSeparateFramesExecutesEffectOncePerFrame() {
		// Test C: Separate frames -> separate executions
		Signal<Integer> signal = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			signal.get();
		});

		assertEquals(1, runs.get());
		runs.set(0);

		// Frame 1
		signal.set(1);
		assertEquals(0, runs.get());
		SignalDispatcher.flush();
		assertEquals(1, runs.get());

		// Frame 2
		signal.set(2);
		assertEquals(1, runs.get());
		SignalDispatcher.flush();
		assertEquals(2, runs.get());

		effect.dispose();
	}

	@Test
	void cascadingEffectDirtiedDuringFlushIsProcessedInSameFlush() {
		// Test D: Effect scheduled during flush is not lost
		Signal<Integer> sigA = Signal.of(0);
		Signal<Integer> sigB = Signal.of(0);
		AtomicInteger runsA = new AtomicInteger(0);
		AtomicInteger runsB = new AtomicInteger(0);

		Effect effectA = Effect.of(() -> {
			runsA.incrementAndGet();
			int a = sigA.get();
			if (a > 0) {
				sigB.set(a * 10);
			}
		});

		Effect effectB = Effect.of(() -> {
			runsB.incrementAndGet();
			sigB.get();
		});

		assertEquals(1, runsA.get());
		assertEquals(1, runsB.get());
		runsA.set(0);
		runsB.set(0);

		sigA.set(5);
		assertEquals(0, runsA.get());
		assertEquals(0, runsB.get());

		SignalDispatcher.flush();

		assertEquals(1, runsA.get());
		assertEquals(1, runsB.get());
		assertEquals(50, sigB.get());

		effectA.dispose();
		effectB.dispose();
	}

	@Test
	void disposedEffectDoesNotExecuteDuringFlush() {
		// Test E: Disposed effect skipped
		Signal<Integer> signal = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			signal.get();
		});

		assertEquals(1, runs.get());
		runs.set(0);

		signal.set(100);
		assertEquals(1, SignalDispatcher.size());

		// Dispose while queued
		effect.dispose();

		SignalDispatcher.flush();

		assertEquals(0, runs.get(), "Disposed effect must not execute during flush");
		assertEquals(0, SignalDispatcher.size());
	}

	@Test
	void frameLifecycleRegistrationIsIdempotent() {
		// Test F: Registration idempotency
		Signal<Integer> signal = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			signal.get();
		});

		runs.set(0);

		// Register repeatedly
		SignalDispatcher.register();
		SignalDispatcher.register();
		SignalDispatcher.ensureRegistered();
		assertTrue(SignalDispatcher.isRegistered());

		signal.set(42);
		assertEquals(0, runs.get());

		// Firing Trigger.update triggers flush
		Events.fire(Trigger.update);
		assertEquals(1, runs.get());

		// Subsequent trigger should not fire extra times
		signal.set(99);
		Events.fire(Trigger.update);
		assertEquals(2, runs.get());

		effect.dispose();
	}

	@Test
	void computedValuesRemainStrictlyLazyAndAreNotScheduled() {
		Signal<Integer> signal = Signal.of(10);
		AtomicInteger computes = new AtomicInteger(0);

		Computed<Integer> comp = signal.map(v -> {
			computes.incrementAndGet();
			return v * 2;
		});

		assertEquals(0, computes.get());

		signal.set(20);
		assertEquals(0, computes.get(), "Computed must not evaluate on signal change");
		assertEquals(0, SignalDispatcher.size(), "Computed must not be queued in SignalDispatcher");

		SignalDispatcher.flush();
		assertEquals(0, computes.get(), "Flush must not evaluate unread computeds");

		assertEquals(40, comp.get());
		assertEquals(1, computes.get());
	}

	@Test
	void largeBatchOfNonCyclicalEffectsExecutesCompletelyInSingleFlush() {
		int effectCount = 200;
		Signal<Integer> source = Signal.of(0);
		AtomicInteger executedCount = new AtomicInteger(0);
		List<Effect> effects = new ArrayList<>();

		for (int i = 0; i < effectCount; i++) {
			effects.add(Effect.of(() -> {
				source.get();
				executedCount.incrementAndGet();
			}));
		}

		// Initial run on creation
		assertEquals(effectCount, executedCount.get());
		executedCount.set(0);

		// Dirty all effects
		source.set(1);
		assertEquals(effectCount, SignalDispatcher.size());
		assertEquals(0, executedCount.get());

		// Flush should drain all 200 effects in a single pass without false infinite loop detection
		SignalDispatcher.flush();

		assertEquals(effectCount, executedCount.get());
		assertEquals(0, SignalDispatcher.size());

		for (Effect effect : effects) {
			effect.dispose();
		}
	}

	@Test
	void cyclicEffectsTerminateSafelyWithoutHanging() {
		Signal<Integer> loopSignal = Signal.of(0);

		Effect effect = Effect.of(() -> {
			int val = loopSignal.get();
			if (val < 200) {
				loopSignal.set(val + 1);
			}
		});

		// Flush should hit max iterations guard and break gracefully
		assertDoesNotThrow(SignalDispatcher::flush);
		assertEquals(0, SignalDispatcher.size());

		effect.dispose();
	}
}
