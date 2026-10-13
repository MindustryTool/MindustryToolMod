package solim.layout;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.CellAccess;
import solim.core.OwnershipContext;
import solim.display.Text;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class SolimCollapserDisposalTest extends SolimEnv {

    @BeforeEach
    void setUp() {
        newScene();
    }

    @Test
    void collapsedBindingStopsOnDispose() {
        Signal<Boolean> collapsed = Signal.of(true);
        SolimCollapser collapser = new SolimCollapser().collapsed(collapsed);
        assertTrue(collapser.isCollapsed());

        collapsed.set(false);
        flushEffects();
        assertFalse(collapser.isCollapsed(), "Live collapsed binding must drive the widget");

        collapser.dispose();
        assertDisposed(collapser, collapsed);

        collapsed.set(true);
        flushEffects();
        assertFalse(collapser.isCollapsed(), "Disposed collapsed binding must freeze");
    }

    @Test
    void expandedBindingStopsOnDispose() {
        Signal<Boolean> expanded = Signal.of(false);
        SolimCollapser collapser = new SolimCollapser().expanded(expanded);
        assertTrue(collapser.isCollapsed());

        expanded.set(true);
        flushEffects();
        assertFalse(collapser.isCollapsed(), "Live expanded binding must drive the widget");

        collapser.dispose();
        assertDisposed(collapser, expanded);

        expanded.set(false);
        flushEffects();
        assertFalse(collapser.isCollapsed(), "Disposed expanded binding must freeze");
    }

    @Test
    void zeroDurationCollapseAppliesImmediatelyInsteadOfAnimating() {

        SolimCollapser instant = new SolimCollapser().duration(0f);
        instant.children(() -> AttachmentStack.add(new Element()));
        instant.collapsed(false);
        instant.collapsed(true);

        assertTrue(instant.isCollapsed(), "A zero-duration collapse must apply immediately");
        assertEquals(0f, instant.collapser().getPrefHeight(), 0.001f);

        instant.dispose();
        assertTrue(instant.isDisposed());
        assertDoesNotThrow(instant::dispose, "Double dispose must be safe");
        assertEquals(0f, instant.collapser().getPrefHeight(), 0.001f,
                "Disposal must not disturb the widget it no longer drives");
    }

    @Test
    void childComponentsDisposeWithTheCollapser() {
        Signal<String> label = Signal.of("body");
        Text[] child = new Text[1];
        SolimCollapser collapser = new SolimCollapser();
        collapser.children(() -> {
            child[0] = Text.of(label);
            AttachmentStack.add(child[0].label());
        });

        assertEquals("body", child[0].label().getText().toString());
        assertFalse(child[0].isDisposed());

        collapser.dispose();
        assertDisposed(collapser, label);
        assertTrue(child[0].isDisposed(), "A declared child must dispose with the collapser");
        assertEquals(0, label.observerCount(), "A disposed child must stop observing its signal");

        label.set("gone");
        flushEffects();
        assertEquals("body", child[0].label().getText().toString(),
                "A disposed child must stop tracking its signal");
    }

    @Test
    void gapRespaceStopsOnDispose() {
        Signal<Float> gap = Signal.of(4f);
        Element first = new Element();
        Element second = new Element();
        SolimCollapser collapser = new SolimCollapser().gap(gap);
        collapser.children(() -> {
            AttachmentStack.add(first);
            AttachmentStack.add(second);
        });
        assertEquals(4f, CellAccess.padTop(collapser.content().getCell(second)), 0.01f);

        collapser.dispose();
        assertDisposed(collapser, gap);

        gap.set(20f);
        flushEffects();
        assertEquals(4f, CellAccess.padTop(collapser.content().getCell(second)), 0.01f,
                "Disposed gap must not re-space the content");
    }

    @Test
    void disposeIsIdempotent() {
        Signal<Boolean> collapsed = Signal.of(true);
        SolimCollapser collapser = new SolimCollapser().collapsed(collapsed);

        collapser.dispose();
        assertDoesNotThrow(collapser::dispose, "Double dispose must be safe");
        assertDoesNotThrow(collapser::dispose, "Repeated dispose must stay safe");
        assertTrue(collapser.isDisposed());
        assertEquals(0, OwnershipContext.size(), "No ownership scope may leak");
    }

    private void assertDisposed(SolimCollapser collapser, Signal<?> signal) {
        assertTrue(collapser.isDisposed(), "The collapser must report itself disposed");
        assertEquals(0, signal.observerCount(), "Disposal must drain the bound signal's observers");
        assertDoesNotThrow(collapser::dispose, "Double dispose must be safe");
        assertEquals(0, OwnershipContext.size(), "No ownership scope may leak");
    }
}
