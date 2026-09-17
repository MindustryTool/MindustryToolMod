package solim.signal;

import solim.runtime.SignalDispatcher;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EffectTest {

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void autoTrackSignalsAndComputeds() {
		Signal<Integer> s1 = Signal.of(1);
		Computed<Integer> c1 = s1.map(v -> v * 2);
		AtomicInteger runs = new AtomicInteger(0);
		AtomicInteger last = new AtomicInteger(-1);
		Effect e = Effect.of(() -> {
			runs.incrementAndGet();
			last.set(c1.get());
		});
		assertEquals(1, runs.get());
		assertEquals(2, last.get());
		s1.set(2);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());
		assertEquals(4, last.get());
		e.dispose();
	}

	@Test
	void dynamicDependencyBranchSwitch() {
		Signal<Boolean> enabled = Signal.of(true);
		Signal<String> username = Signal.of("Player");
		Signal<String> email = Signal.of("player@example.com");
		AtomicReference<String> last = new AtomicReference<>("");
		AtomicInteger runs = new AtomicInteger(0);
		Effect e = Effect.of(() -> {
			runs.incrementAndGet();
			if (enabled.get()) {
				last.set(username.get());
			} else {
				last.set(email.get());
			}
		});
		assertEquals("Player", last.get());
		assertEquals(1, runs.get());
		assertEquals(2, e.dependencyCount()); // enabled + username
		enabled.set(false);
		SignalDispatcher.flush();
		assertEquals("player@example.com", last.get());
		assertEquals(2, runs.get());
		assertEquals(2, e.dependencyCount()); // enabled + email
		// old dep should not trigger
		username.set("NewPlayer");
		SignalDispatcher.flush();
		assertEquals(2, runs.get(), "Old dependency should not trigger");
		assertEquals("player@example.com", last.get());
		email.set("new@example.com");
		SignalDispatcher.flush();
		assertEquals(3, runs.get());
		assertEquals("new@example.com", last.get());
		e.dispose();
	}

	@Test
	void oldDepsRemovedBeforeNew() {
		Signal<Boolean> cond = Signal.of(true);
		Signal<Integer> a = Signal.of(1);
		Signal<Integer> b = Signal.of(2);
		Effect e = Effect.of(() -> {
			if (cond.get()) a.get();
			else b.get();
		});
		assertEquals(2, e.dependencyCount());
		cond.set(false);
		SignalDispatcher.flush();
		assertEquals(2, e.dependencyCount());
		// a should no longer be tracked
		// we already tested via previous dynamic test, but check counts
		a.set(10);
		SignalDispatcher.flush();
		// should not rerun effect
		// Use a new effect to verify
		AtomicInteger runs = new AtomicInteger(0);
		Effect e2 = Effect.of(() -> {
			runs.incrementAndGet();
			if (cond.get()) a.get();
			else b.get();
		});
		runs.set(0);
		a.set(20);
		SignalDispatcher.flush();
		// cond false, so a not dependency, so no run
		assertEquals(0, runs.get());
		b.set(30);
		SignalDispatcher.flush();
		assertEquals(1, runs.get());
		e.dispose();
		e2.dispose();
	}

	@Test
	void disposeStopsReacting() {
		Signal<Integer> s = Signal.of(1);
		AtomicInteger runs = new AtomicInteger(0);
		Effect e = Effect.of(() -> {
			runs.incrementAndGet();
			s.get();
		});
		assertEquals(1, runs.get());
		e.dispose();
		assertTrue(e.isDisposed());
		s.set(2);
		SignalDispatcher.flush();
		assertEquals(1, runs.get());
		assertDoesNotThrow(e::dispose);
	}

	@Test
	void cleanupRunsBeforeRerunAndOnDispose() {
		Signal<Integer> s = Signal.of(1);
		AtomicInteger cleanupRuns = new AtomicInteger(0);
		AtomicInteger effectRuns = new AtomicInteger(0);
		Effect e = Effect.of(cleanup -> {
			effectRuns.incrementAndGet();
			s.get();
			cleanup.add(() -> cleanupRuns.incrementAndGet());
		});
		assertEquals(1, effectRuns.get());
		assertEquals(0, cleanupRuns.get());
		s.set(2);
		SignalDispatcher.flush();
		assertEquals(2, effectRuns.get());
		assertEquals(1, cleanupRuns.get(), "Cleanup should run before re-run");
		e.dispose();
		assertEquals(2, cleanupRuns.get(), "Cleanup should run on dispose");
	}

	@Test
	void cleanupWithSupplierReturn() {
		Signal<Integer> s = Signal.of(1);
		AtomicInteger cleanupRuns = new AtomicInteger(0);
		Effect e = Effect.of(() -> {
			s.get();
			return () -> cleanupRuns.incrementAndGet();
		});
		assertEquals(0, cleanupRuns.get());
		s.set(2);
		SignalDispatcher.flush();
		assertEquals(1, cleanupRuns.get());
		e.dispose();
		assertEquals(2, cleanupRuns.get());
	}

	@Test
	void cleanupFailureDoesNotBlockOthers() {
		Signal<Integer> s = Signal.of(1);
		AtomicInteger goodCleanup = new AtomicInteger(0);
		Effect e = Effect.of(cleanup -> {
			s.get();
			cleanup.add(() -> {
				throw new RuntimeException("fail");
			});
			cleanup.add(() -> goodCleanup.incrementAndGet());
		});
		s.set(2);
		SignalDispatcher.flush();
		// second cleanup should still run despite first failing
		assertEquals(1, goodCleanup.get());
		e.dispose();
		assertEquals(2, goodCleanup.get());
	}

	@Test
	void errorsLoggedSafely() {
		Signal<Integer> s = Signal.of(1);
		AtomicInteger runs = new AtomicInteger(0);
		Effect e = Effect.of(() -> {
			runs.incrementAndGet();
			s.get();
			if (runs.get() == 2) throw new RuntimeException("test error");
		});
		assertEquals(1, runs.get());
		// second run throws but should not break
		s.set(2);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());
		s.set(3);
		SignalDispatcher.flush();
		assertEquals(3, runs.get(), "Effect should continue after error");
		e.dispose();
	}

	@Test
	void noLeakAfterDispose() {
		Signal<Integer> s = Signal.of(1);
		Effect e = Effect.of(() -> s.get());
		assertEquals(1, s.observerCount());
		assertEquals(1, e.dependencyCount());
		e.dispose();
		assertEquals(0, s.observerCount());
		assertEquals(0, e.dependencyCount());
	}

	@Test
	void disposeIsIdempotent() {
		Signal<Integer> s = Signal.of(1);
		Effect e = Effect.of(() -> s.get());
		e.dispose();
		assertDoesNotThrow(e::dispose);
		assertTrue(e.isDisposed());
	}
}
