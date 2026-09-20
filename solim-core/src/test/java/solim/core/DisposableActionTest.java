package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class DisposableActionTest extends SolimEnv {

	@Test
	void runsActionOnceAndUpdatesDisposedStatus() {
		AtomicInteger count = new AtomicInteger(0);
		DisposableAction action = DisposableAction.of(count::incrementAndGet);

		assertFalse(action.isDisposed());
		assertEquals(0, count.get());

		action.dispose();
		assertTrue(action.isDisposed());
		assertEquals(1, count.get());

		// Idempotent: repeated calls do nothing
		action.dispose();
		assertTrue(action.isDisposed());
		assertEquals(1, count.get());
	}

	@Test
	void emptyDisposableActionIsSafe() {
		// DisposableAction.empty() is a shared singleton; in a reused test JVM it
		// may already be disposed by another class. The contract under test is
		// that dispose() is safe and marks the action disposed.
		DisposableAction empty = DisposableAction.empty();

		assertDoesNotThrow(empty::dispose);
		assertTrue(empty.isDisposed());
	}

	@Test
	void nullActionReturnsEmpty() {
		DisposableAction action = DisposableAction.of(null);
		assertSame(DisposableAction.empty(), action);
	}
}
