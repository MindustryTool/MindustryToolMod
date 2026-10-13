package solim.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

public class SolimEnv extends ArcTestEnv {

    @BeforeEach
    public void setUpSolimEnv() {
        resetAmbientState();
        resetModuleState();
    }

    @AfterEach
    public void verifyAndTearDownSolimEnv() {
        int parentStackSize = AttachmentStack.size();
        int componentContextSize = OwnershipContext.size();
        int reactiveContextSize = ReactiveContext.size();
        int pendingEffects = SignalDispatcher.size();
        boolean isFlushing = SignalDispatcher.isFlushing();

        resetAmbientState();
        resetModuleState();

        assertEquals(0, parentStackSize, "AttachmentStack must be empty at teardown");
        assertNull(AttachmentStack.current(), "AttachmentStack.current() must be null at teardown");

        assertEquals(0, componentContextSize, "OwnershipContext must be empty at teardown");
        assertNull(OwnershipContext.current(), "OwnershipContext.current() must be null at teardown");

        assertEquals(0, reactiveContextSize, "ReactiveContext must be empty at teardown");
        assertNull(ReactiveContext.current(), "ReactiveContext.current() must be null at teardown");

        assertFalse(isFlushing, "SignalDispatcher must not be mid-flush at teardown");
        assertEquals(0, pendingEffects, "SignalDispatcher must have no pending effects at teardown");
    }

    protected void resetModuleState() {
    }

    public static void flushEffects() {
        SignalDispatcher.flush();
    }

    public static void resetAmbientState() {
        AttachmentStack.clear();
        OwnershipContext.clear();
        ReactiveContext.clear();
        SignalDispatcher.resetForTests();
        solim.core.SessionRoot.get().clear();
        solim.core.SolimAssert.setMainThread(null);
    }
}
