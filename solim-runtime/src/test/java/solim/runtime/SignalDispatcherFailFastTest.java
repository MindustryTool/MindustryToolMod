package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.SchedulableEffect;

class SignalDispatcherFailFastTest {

	@BeforeEach
	@AfterEach
	void reset() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void flushPropagatesUnhandledExceptionFromEffect() {
		RuntimeException failure = new RuntimeException("Simulated effect failure");
		SchedulableEffect badEffect = new SchedulableEffect() {
			@Override
			public void runPending() {
				throw failure;
			}

			@Override
			public void clearPending() {
			}

			@Override
			public boolean isDisposed() {
				return false;
			}
		};

		SignalDispatcher.enqueue(badEffect);

		RuntimeException thrown = assertThrows(RuntimeException.class, SignalDispatcher::flush);
		assertEquals("Simulated effect failure", thrown.getMessage());
		assertFalse(SignalDispatcher.isFlushing());
	}
}
