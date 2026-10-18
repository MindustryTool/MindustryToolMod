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

        action.dispose();
        assertTrue(action.isDisposed());
        assertEquals(1, count.get());
    }

    @Test
    void emptyDisposableActionIsStateless() {
        DisposableAction empty = DisposableAction.empty();

        assertDoesNotThrow(empty::dispose);
        assertFalse(empty.isDisposed(), "EMPTY carries no per-instance disposed state to poison later holders");
        assertDoesNotThrow(empty::dispose);
        assertFalse(empty.isDisposed());
    }

    @Test
    void repeatedEmptyDisposalNeverPoisonsNewHolders() {
        DisposableAction.empty().dispose();

        AtomicInteger count = new AtomicInteger(0);
        DisposableAction action = DisposableAction.of(count::incrementAndGet);

        assertFalse(action.isDisposed());
        action.dispose();
        assertEquals(1, count.get());
    }

    @Test
    void nullActionReturnsEmpty() {
        DisposableAction action = DisposableAction.of(null);
        assertSame(DisposableAction.empty(), action);
    }
}
