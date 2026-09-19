package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SolimAssertTest {

	@AfterEach
	void tearDown() {
		SolimAssert.setMainThread(null);
	}

	@Test
	void checkMainThreadIsNoOpWhenMainThreadUnset() {
		SolimAssert.setMainThread(null);
		assertDoesNotThrow(SolimAssert::checkMainThread);
	}

	@Test
	void checkMainThreadSucceedsOnRegisteredMainThread() {
		SolimAssert.setMainThread(Thread.currentThread());
		assertDoesNotThrow(SolimAssert::checkMainThread);
	}

	@Test
	void checkMainThreadThrowsOnDifferentThread() throws InterruptedException {
		SolimAssert.setMainThread(Thread.currentThread());

		AtomicReference<Throwable> thrown = new AtomicReference<>();
		Thread background = new Thread(() -> {
			try {
				SolimAssert.checkMainThread();
			} catch (Throwable t) {
				thrown.set(t);
			}
		}, "TestBackgroundThread");

		background.start();
		background.join();

		assertNotNull(thrown.get());
		assertInstanceOf(IllegalStateException.class, thrown.get());
		assertTrue(thrown.get().getMessage().contains("TestBackgroundThread"));
	}
}
