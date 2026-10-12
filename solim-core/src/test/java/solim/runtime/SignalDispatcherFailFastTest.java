package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Effect;
import solim.reactive.Signal;

/**
 * Drives the dispatcher with a real {@link Effect}. The previous version used a hand-rolled
 * {@link SchedulableEffect} stub, which exercised the queue but never the {@code Effect}
 * exception path — which is why {@code Effect}'s {@code catch (Throwable)} survived the
 * fail-fast conversion unnoticed.
 *
 * <p>Lives in solim-core because {@link Effect} is a solim-core type and solim-core cannot be
 * depended on from solim-runtime.
 */
class SignalDispatcherFailFastTest {

	@BeforeEach
	@AfterEach
	void reset() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void flushPropagatesUnhandledExceptionFromEffect() {
		Signal<Integer> trigger = Signal.of(0);
		Effect effect = Effect.of(() -> {
			if (trigger.get() > 0) {
				throw new RuntimeException("Simulated effect failure");
			}
		});

		trigger.set(1);

		RuntimeException thrown = assertThrows(RuntimeException.class, SignalDispatcher::flush);
		assertEquals("Simulated effect failure", thrown.getMessage());
		assertFalse(SignalDispatcher.isFlushing());

		effect.dispose();
	}

	@Test
	void flushRunsAHealthyEffectNormally() {
		Signal<Integer> trigger = Signal.of(0);
		int[] runs = new int[]{0};
		Effect effect = Effect.of(() -> {
			trigger.get();
			runs[0]++;
		});

		trigger.set(1);
		SignalDispatcher.flush();

		assertEquals(2, runs[0]);
		effect.dispose();
	}
}
