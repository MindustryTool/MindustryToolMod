package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.math.Interp;
import arc.scene.Element;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class SolimCollapserTest extends SolimEnv {

    @Test
    void defaultStateIsCollapsed() {
        SolimCollapser collapser = new SolimCollapser();
        assertTrue(collapser.isCollapsed(), "Default SolimCollapser should be collapsed");
        assertEquals(0f, collapser.element().getPrefHeight(), 0.001f, "PrefHeight should be 0 when collapsed");
        collapser.dispose();
    }

    @Test
    void explicitExpandedState() {
        SolimCollapser collapser = new SolimCollapser(false);
        assertFalse(collapser.isCollapsed(), "SolimCollapser(false) should not be collapsed");
        collapser.dispose();
    }

    @Test
    void childrenAreAddedToContentTable() {
        SolimCollapser collapser = new SolimCollapser();
        Element child1 = new Element();
        Element child2 = new Element();

        collapser.children(() -> {
            AttachmentStack.add(child1);
            AttachmentStack.add(child2);
        });

        assertEquals(2, collapser.content().getChildren().size);
        assertSame(child1, collapser.content().getChildren().get(0));
        assertSame(child2, collapser.content().getChildren().get(1));
        collapser.dispose();
    }

    @Test
    void reactiveExpandedSignalTogglesCollapsedState() {
        Signal<Boolean> expanded = Signal.of(false);
        SolimCollapser collapser = new SolimCollapser().expanded(expanded);

        assertTrue(collapser.isCollapsed());

        expanded.set(true);
        SignalDispatcher.flush();
        assertFalse(collapser.isCollapsed());

        expanded.set(false);
        SignalDispatcher.flush();
        assertTrue(collapser.isCollapsed());

        collapser.dispose();
    }

    @Test
    void reactiveCollapsedSignalTogglesState() {
        Signal<Boolean> collapsed = Signal.of(true);
        SolimCollapser c = new SolimCollapser().collapsed(collapsed);

        assertTrue(c.isCollapsed());

        collapsed.set(false);
        SignalDispatcher.flush();
        assertFalse(c.isCollapsed());

        c.dispose();
    }

    @Test
    void disposalStopsReactiveUpdates() {
        Signal<Boolean> expanded = Signal.of(false);
        SolimCollapser collapser = new SolimCollapser().expanded(expanded);

        assertTrue(collapser.isCollapsed());
        collapser.dispose();
        assertTrue(collapser.isDisposed());

        expanded.set(true);
        SignalDispatcher.flush();

        assertTrue(collapser.isCollapsed());
    }

    @Test
    void horizontalDirectionCollapsesWidthAndAnimatesWithInterp() {
        Signal<Boolean> expanded = Signal.of(false);
        SolimCollapser collapser = new SolimCollapser()
                .direction(Direction.HORIZONTAL)
                .duration(0.3f, Interp.pow3Out)
                .expanded(expanded)
                .padding(10f);

        assertEquals(Direction.HORIZONTAL, collapser.direction());
        assertEquals(0f, collapser.collapser().getPrefWidth(), 0.001f);

        expanded.set(true);
        SignalDispatcher.flush();
        collapser.collapser().act(0.1f);
        float midWidth = collapser.collapser().getPrefWidth();
        assertTrue(midWidth > 0f && midWidth < 20f);

        collapser.collapser().act(0.3f);
        assertEquals(20f, collapser.collapser().getPrefWidth(), 0.001f);

        collapser.dispose();
    }
}
