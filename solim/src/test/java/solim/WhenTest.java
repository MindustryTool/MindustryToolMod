package solim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import solim.core.BaseComponent;
import solim.reactive.Dynamic;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class WhenTest extends SolimEnv {

    static class Probe extends BaseComponent {
        boolean disposed = false;

        @Override
        protected Element build() {
            return new Element();
        }

        @Override
        protected void onDispose() {
            disposed = true;
        }
    }

    @AfterEach
    void clear() {
        ParentStack.clear();
    }

    @Test
    void trueConditionMountsContent() {
        Signal<Boolean> visible = Signal.of(true);
        Probe[] created = new Probe[1];
        Dynamic<Boolean> when = UI.when(visible, () -> {
            Probe probe = new Probe();
            created[0] = probe;
            return probe;
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        when.dispose();
    }

    @Test
    void falseConditionUnmountsAndDisposes() {
        Signal<Boolean> visible = Signal.of(true);
        Probe[] created = new Probe[1];
        Dynamic<Boolean> when = UI.when(visible, () -> {
            Probe probe = new Probe();
            created[0] = probe;
            return probe;
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);

        visible.set(false);
        SignalDispatcher.flush();
        assertEquals(0, when.container().getChildren().size);
        assertTrue(created[0].disposed);
        when.dispose();
    }
}
