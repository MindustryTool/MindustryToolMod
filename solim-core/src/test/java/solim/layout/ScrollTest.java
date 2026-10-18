package solim.layout;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Table;
import solim.overlay.Hud;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class ScrollTest extends SolimEnv {


    @Test
    void createsWithDefaultNames() {
        Scroll s = new Scroll();
        assertEquals("solim-scroll-pane-outer", s.outer().name);
        assertEquals("solim-scroll-pane-content", s.content().name);
    }

    @Test
    void addAddsChildToContent() {
        Scroll s = new Scroll();
        Element e = new Element();
        s.add(e);

        assertEquals(1, s.content().getChildren().size);
        assertSame(e, s.content().getChildren().get(0));
    }

    @Test
    void multipleChildrenPreserveOrder() {
        Scroll s = new Scroll();
        Element first = new Element();
        Element second = new Element();
        Element third = new Element();

        s.add(first);
        s.add(second);
        s.add(third);

        assertEquals(3, s.content().getChildren().size);
        assertSame(first, s.content().getChildren().get(0));
        assertSame(second, s.content().getChildren().get(1));
        assertSame(third, s.content().getChildren().get(2));
    }

    @Test
    void childrenRunnableAddsToContent() {
        Scroll s = new Scroll();
        Element first = new Element();
        Element second = new Element();

        s.children(() -> {
            AttachmentStack.add(first);
            AttachmentStack.add(second);
        });

        assertEquals(2, s.content().getChildren().size);
        assertSame(first, s.content().getChildren().get(0));
        assertSame(second, s.content().getChildren().get(1));
    }

    @Test
    void visibleModifierChangesOuterVisibility() {
        Scroll s = new Scroll();

        s.props(visible(false));
        assertFalse(s.outer().visible);

        s.props(visible(true));
        assertTrue(s.outer().visible);
    }

    @Test
    void positionSetsOuterCoordinates() {
        Scroll s = new Scroll();

        s.props(x(10f));
        assertEquals(10f, s.outer().x, 0.01f);

        s.props(y(20f));
        assertEquals(20f, s.outer().y, 0.01f);

        s.props(position(30f, 40f));
        assertEquals(30f, s.outer().x, 0.01f);
        assertEquals(40f, s.outer().y, 0.01f);
    }

    @Test
    void nameModifierUpdatesOuterName() {
        Scroll s = new Scroll();
        s.name("my-scroll");
        assertEquals("my-scroll", s.outer().name);
    }

    @Test
    void outerIsSameAsElement() {
        Scroll s = new Scroll();
        assertSame(s.outer(), s.element());
    }

    @Test
    void cellConfigReturnsNonNull() {
        Scroll s = new Scroll();
        assertNotNull(s.cellConfig());
    }

    @Test
    void scrollDisabledConfiguration() {
        Scroll scroll = new Scroll();
        scroll.props(scrollingDisabled(false, true));
        assertFalse(scroll.isScrollingDisabledX());
        assertTrue(scroll.isScrollingDisabledY());

        scroll.props(scrollX(true));
        assertFalse(scroll.isScrollingDisabledX());

        scroll.props(scrollX(false));
        assertTrue(scroll.isScrollingDisabledX());

        scroll.props(scrollY(true));
        assertFalse(scroll.isScrollingDisabledY());

        scroll.props(scrollY(false));
        assertTrue(scroll.isScrollingDisabledY());
    }

    @Test
    void scrollPercentagesAndDirections() {
        Scroll scroll = new Scroll();
        assertDoesNotThrow(() -> {
            scroll.scrollToBottom();
            scroll.scrollToTop();
            scroll.scrollPercentX(0.5f);
            scroll.scrollPercentY(0.75f);
        });
    }

    @Test
    void hudToFrontOnTouchAndMount() {
        Hud hud = new Hud();
        hud.toFrontOnTouch();

        InputListener listener = null;
        for (EventListener l : hud.root().getListeners()) {
            if (l instanceof InputListener) {
                listener = (InputListener) l;
                break;
            }
        }
        assertNotNull(listener, "toFrontOnTouch should register an InputListener on hud.root()");

        Table parent = new Table();
        hud.mount(parent);
        assertTrue(parent.getChildren().contains(hud.root()), "HUD root should be added to parent group");

        hud.dispose();
    }
}
