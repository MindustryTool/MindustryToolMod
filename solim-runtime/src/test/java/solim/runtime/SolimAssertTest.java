package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

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
}
