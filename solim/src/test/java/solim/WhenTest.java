package solim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import solim.core.BaseComponent;
import solim.layout.Column;
import solim.reactive.Signal;
import solim.reactive.When;
import solim.runtime.AttachmentStack;
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

    private Probe lastThen;
    private Probe lastElse;

    private Probe makeThen() {
        lastThen = new Probe();
        return lastThen;
    }

    private Probe makeElse() {
        lastElse = new Probe();
        return lastElse;
    }

    @AfterEach
    void clear() {
        AttachmentStack.clear();
    }

    @Test
    void trueConditionMountsThenBranch() {
        Signal<Boolean> visible = Signal.of(true);
        Probe[] created = new Probe[1];
        When when = UI.when(visible).thenDo(() -> {
            Probe probe = new Probe();
            created[0] = probe;
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        when.dispose();
    }

    @Test
    void falseConditionMountsElseBranch() {
        Signal<Boolean> visible = Signal.of(false);
        Probe[] elseCreated = new Probe[1];
        When when = UI.when(visible)
                .thenDo(() -> new Probe())
                .elseDo(() -> {
                    Probe probe = new Probe();
                    elseCreated[0] = probe;
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
        When when = UI.when(visible).thenDo(() -> {
            Probe probe = new Probe();
            created[0] = probe;
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

    @Test
    void conditionFlipSwitchesBranches() {
        Signal<Boolean> visible = Signal.of(false);
        Probe[] thenCreated = new Probe[1];
        Probe[] elseCreated = new Probe[1];
        When when = UI.when(visible)
                .thenDo(() -> {
                    Probe probe = new Probe();
                    thenCreated[0] = probe;
                })
                .elseDo(() -> {
                    Probe probe = new Probe();
                    elseCreated[0] = probe;
                });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);

        visible.set(true);
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(elseCreated[0].disposed);

        visible.set(false);
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(thenCreated[0].disposed);
        when.dispose();
    }

    @Test
    void facadeRendersWhenInsideParentColumn() {
        Signal<Boolean> visible = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When[] whenBox = new When[1];
        Column col = UI.column().children(() -> {
            whenBox[0] = UI.when(visible)
                    .thenDo(() -> {
                        thenBox[0] = new Probe();
                    })
                    .elseDo(() -> {
                        elseBox[0] = new Probe();
                    });
        });
        assertTrue(col.table().getChildren().contains(whenBox[0].container(), true));
        assertEquals(1, whenBox[0].container().getChildren().size);
        assertTrue(!thenBox[0].disposed);

        visible.set(false);
        SignalDispatcher.flush();
        assertTrue(thenBox[0].disposed);
        assertEquals(1, whenBox[0].container().getChildren().size);
        assertTrue(!elseBox[0].disposed);

        whenBox[0].dispose();
        col.dispose();
    }

    @Test
    void facadeChainedModifiersRender() {
        Signal<Boolean> visible = Signal.of(false);
        Probe[] elseBox = new Probe[1];
        When when = UI.when(visible)
                .thenDo(() -> new Probe())
                .elseDo(() -> {
                    elseBox[0] = new Probe();
                })
                .growX();
        when.element();
        SignalDispatcher.flush();
        assertTrue(when.cellConfig().growX);
        assertEquals(1, when.container().getChildren().size);
        assertTrue(!elseBox[0].disposed);
        when.dispose();
    }

    @Test
    void facadeDisposeStopsUpdates() {
        Signal<Boolean> visible = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        When when = UI.when(visible)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> new Probe());
        when.element();
        SignalDispatcher.flush();
        when.dispose();
        assertTrue(thenBox[0].disposed);

        visible.set(false);
        SignalDispatcher.flush();
        assertEquals(0, when.container().getChildren().size);
    }

    @Test
    void facadeMethodReferenceBranches() {
        lastThen = null;
        lastElse = null;
        Signal<Boolean> visible = Signal.of(true);
        When when = UI.when(visible).thenDo(this::makeThen).elseDo(this::makeElse);
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(lastThen != null && !lastThen.disposed);
        assertTrue(lastElse == null);

        visible.set(false);
        SignalDispatcher.flush();
        assertTrue(lastThen.disposed);
        assertTrue(lastElse != null && !lastElse.disposed);
        assertEquals(1, when.container().getChildren().size);
        when.dispose();
        lastThen = null;
        lastElse = null;
    }
}
