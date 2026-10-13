package solim.reactive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class DisposalPrimitivesTest extends SolimEnv {

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void disposedEffectNeverRerunsAndDetaches() {
		Signal<Integer> source = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);
		Effect effect = Effect.of(() -> {
			source.get();
			runs.incrementAndGet();
		});
		assertEquals(1, runs.get());
		assertFalse(effect.isDisposed());

		source.set(1);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());

		effect.dispose();
		assertTrue(effect.isDisposed());

		source.set(2);
		SignalDispatcher.flush();
		assertEquals(2, runs.get(), "Disposed effect must not re-run");
		assertEquals(0, source.observerCount(), "Disposed effect must detach from signal");
		assertEquals(0, effect.dependencyCount(), "Disposed effect must clear dependencies");

		assertDoesNotThrow(effect::dispose, "Double dispose must be safe");
		assertTrue(effect.isDisposed());
		assertEquals(2, runs.get());
	}

	@Test
	void disposedEffectRunsCleanup() {
		AtomicBoolean cleaned = new AtomicBoolean(false);
		Effect effect = Effect.ofSupplier(() -> () -> cleaned.set(true));
		assertFalse(cleaned.get());
		assertFalse(effect.isDisposed());

		effect.dispose();
		assertTrue(cleaned.get(), "Dispose must run effect cleanup");
		assertTrue(effect.isDisposed());
	}

	@Test
	void signalBindingStopsAndIsIdempotent() {
		Signal<String> source = Signal.of("a");
		AtomicInteger calls = new AtomicInteger(0);
		Effect sub = Effect.of(source, v -> calls.incrementAndGet());
		assertEquals(1, calls.get());
		assertFalse(sub.isDisposed());

		source.set("b");
		SignalDispatcher.flush();
		assertEquals(2, calls.get());

		sub.dispose();
		assertTrue(sub.isDisposed());

		source.set("c");
		SignalDispatcher.flush();
		assertEquals(2, calls.get(), "Disposed effect must stay silent");
		assertEquals(0, source.observerCount());

		assertDoesNotThrow(sub::dispose, "Double dispose must be safe");
		assertTrue(sub.isDisposed());
	}

	@Test
	void computedBindingStopsAndIsIdempotent() {
		Signal<Integer> source = Signal.of(1);
		Computed<Integer> doubled = source.map(v -> v * 2);
		AtomicInteger calls = new AtomicInteger(0);
		Effect sub = Effect.of(doubled, v -> calls.incrementAndGet());
		assertFalse(sub.isDisposed());
		assertEquals(1, calls.get(), "First evaluation notifies the effect");

		source.set(2);
		SignalDispatcher.flush();
		assertEquals(2, calls.get());

		sub.dispose();
		assertTrue(sub.isDisposed());

		source.set(3);
		SignalDispatcher.flush();
		assertEquals(2, calls.get(), "Disposed computed effect must stay silent");
		assertEquals(0, doubled.observerCount());

		assertDoesNotThrow(sub::dispose, "Double dispose must be safe");
		doubled.dispose();
		assertTrue(doubled.isDisposed());
	}

	@Test
	void signalGraphDrainsAfterDownstreamDisposal() {
		Signal<Integer> source = Signal.of(0);
		Effect first = Effect.of(source, v -> {
		});
		Effect second = Effect.of(source, v -> {
		});
		Computed<Integer> derived = source.map(v -> v + 1);
		Effect effect = Effect.of(() -> derived.get());

		assertTrue(source.observerCount() > 0);

		first.dispose();
		second.dispose();
		effect.dispose();
		derived.dispose();

		assertEquals(0, source.observerCount());
	}

	@Test
	void repeatedMountUnmoutLeavesNoObservers() {
		Signal<Integer> source = Signal.of(0);
		for (int i = 0; i < 25; i++) {
			Effect sub = Effect.of(source, v -> {
			});
			Computed<Integer> derived = source.map(v -> v);
			Effect effect = Effect.of(() -> derived.get());
			sub.dispose();
			effect.dispose();
			derived.dispose();
		}
		SignalDispatcher.flush();
		assertEquals(0, source.observerCount(), "Repeated mount/unmount must not accumulate observers");
	}

	@Test
	void disposedComputedUnsubscribesAndClears() {
		Signal<Integer> source = Signal.of(1);
		Computed<Integer> doubled = source.map(v -> v * 2);
		assertEquals(2, doubled.peek());

		Effect watcher = Effect.of(() -> doubled.get());

		doubled.dispose();
		assertTrue(doubled.isDisposed());

		source.set(5);
		SignalDispatcher.flush();
		assertEquals(0, source.observerCount());
		assertEquals(0, doubled.observerCount());

		assertDoesNotThrow(doubled::dispose, "Double dispose must be safe");
		assertTrue(doubled.isDisposed());
		watcher.dispose();
	}
}
