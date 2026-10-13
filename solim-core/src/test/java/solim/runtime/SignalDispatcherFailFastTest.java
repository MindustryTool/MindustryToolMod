package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Effect;
import solim.reactive.Signal;

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
        int[] runs = new int[] { 0 };
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
