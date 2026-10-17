package solim.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import arc.func.Prov;
import arc.scene.Element;
import arc.util.Nullable;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SessionRoot;
import solim.core.SolimToken;
import solim.modifier.PendingCellConfig;
import solim.runtime.AttachmentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

public class SolimEnv extends ArcTestEnv {

    static final class Holder implements solim.core.DisposableOwner {
        final List<solim.core.Disposable> owned = new ArrayList<>();

        @Override
        public void adopt(solim.core.Disposable resource) {
            if (resource != null) {
                owned.add(resource);
            }
        }

        @Override
        public void dispose() {
            for (int i = owned.size() - 1; i >= 0; i--) {
                owned.get(i).dispose();
            }
            owned.clear();
        }

        @Override
        public boolean isDisposed() {
            return false;
        }
    }

    /**
     * Runs {@code block} with a throwaway ambient owner, for a test that creates a reactive resource
     * outside a component's {@code children(...)} or {@code build()} and still needs it owned.
     */
    public static void inOwnershipWindow(Runnable block) {
        inOwnershipWindow(() -> {
            block.run();
            return null;
        });
    }

    public static <T> T inOwnershipWindow(Prov<T> factory) {
        Holder holder = new Holder();
        OwnershipContext.push(holder);
        try {
            return factory.get();
        } finally {
            OwnershipContext.pop();
        }
    }

    /**
     * Whether the test body runs with a throwaway ambient owner. Suites whose subject is ownership
     * itself override this to {@code false} so they can assert the exact owner stack.
     */
    protected boolean opensOwnershipWindow() {
        return true;
    }

    @BeforeEach
    public void setUpSolimEnv() {
        resetAmbientState();
        resetModuleState();
        if (opensOwnershipWindow()) {
            OwnershipContext.push(new Holder());
        }
    }

    @AfterEach
    public void verifyAndTearDownSolimEnv() {
        if (opensOwnershipWindow()) {
            OwnershipContext.pop();
        }

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
        PendingCellConfig.install();
    }

    /**
     * The component that owns {@code element}. The env layer is the only place outside solim that
     * may reach the element binding, so a module's test asks here instead of naming the token.
     */
    public static @Nullable Component componentOf(@Nullable Element element) {
        return SolimToken.getComponent(element);
    }

    public static void flushEffects() {
        SignalDispatcher.flush();
    }

    public static void resetAmbientState() {
        AttachmentStack.clear();
        OwnershipContext.clear();
        ReactiveContext.clear();
        SignalDispatcher.resetForTests();
        SessionRoot.get().resetForTests();
        solim.core.SolimAssert.setMainThread(null);
    }
}
