package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.util.Align;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;
import solim.runtime.ParentStack;

class CardTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @Test
    void createsWithDefaultNames() {
        Card c = new Card();
        assertEquals("solim-card-cardButton", c.cardButton().name);
        assertEquals("solim-card-container", c.container().name);
    }

    @Test
    void childrenAreAddedToContainer() {
        Card c = new Card();
        Element first = new Element();
        Element second = new Element();

        c.children(() -> {
            ParentStack.add(first);
            ParentStack.add(second);
        });

        assertEquals(2, c.container().getChildren().size);
        assertSame(first, c.container().getChildren().get(0));
        assertSame(second, c.container().getChildren().get(1));
        c.dispose();
    }

    @Test
    void onClickExecutesCallback() {
        boolean[] clicked = { false };
        Card c = new Card().onClick(() -> clicked[0] = true);

        InputEvent event = new InputEvent();
        c.cardButton().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        });

        assertTrue(clicked[0]);
        c.dispose();
    }

    @Test
    void onClickDoesNotExecuteWhenEventStopped() {
        boolean[] clicked = { false };
        Card c = new Card().onClick(() -> clicked[0] = true);

        InputEvent stoppedEvent = new InputEvent();
        stoppedEvent.stop();
        c.cardButton().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(stoppedEvent, 0f, 0f);
            }
        });

        assertFalse(clicked[0]);
        c.dispose();
    }

    @Test
    void colorModifierUpdatesButtonColor() {
        Card c = new Card();
        c.color(Color.scarlet);
        assertEquals(Color.scarlet, c.cardButton().color);
        c.dispose();
    }

    @Test
    void reactiveColorUpdatesButtonColor() {
        Signal<Color> colorSig = Signal.of(Color.green);
        Card c = new Card().color(colorSig);
        assertEquals(Color.green, c.cardButton().color);

        colorSig.set(Color.blue);
        SignalDispatcher.flush();
        assertEquals(Color.blue, c.cardButton().color);
        c.dispose();
    }

    @Test
    void reactiveWidthUpdatesButtonPrefWidth() {
        Signal<Float> widthSig = Signal.of(200f);
        Card c = new Card();
        c.width(widthSig);
        assertEquals(200f, c.cardButton().getWidth(), 0.01f);
        assertEquals(200f, c.cellConfig().prefWidth.get(), 0.01f);

        widthSig.set(300f);
        SignalDispatcher.flush();
        assertEquals(300f, c.cardButton().getWidth(), 0.01f);
        assertEquals(300f, c.cellConfig().prefWidth.get(), 0.01f);
        c.dispose();
    }

    @Test
    void reactiveHeightUpdatesButtonPrefHeight() {
        Signal<Float> heightSig = Signal.of(150f);
        Card c = new Card();
        c.height(heightSig);
        assertEquals(150f, c.cardButton().getHeight(), 0.01f);
        assertEquals(150f, c.cellConfig().prefHeight.get(), 0.01f);

        heightSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, c.cardButton().getHeight(), 0.01f);
        assertEquals(200f, c.cellConfig().prefHeight.get(), 0.01f);
        c.dispose();
    }

    @Test
    void visibleModifierChangesButtonVisibility() {
        Card c = new Card();

        c.visible(false);
        assertFalse(c.cardButton().visible);

        c.visible(true);
        assertTrue(c.cardButton().visible);
        c.dispose();
    }

    @Test
    void reactiveVisibleUpdatesButtonVisibility() {
        Signal<Boolean> vis = Signal.of(true);
        Card c = new Card().visible(vis);
        assertTrue(c.cardButton().visible);

        vis.set(false);
        SignalDispatcher.flush();
        assertFalse(c.cardButton().visible);
        c.dispose();
    }

    @Test
    void positionSetsButtonCoordinates() {
        Card c = new Card();

        c.x(10f);
        assertEquals(10f, c.cardButton().x, 0.01f);

        c.y(20f);
        assertEquals(20f, c.cardButton().y, 0.01f);

        c.position(30f, 40f);
        assertEquals(30f, c.cardButton().x, 0.01f);
        assertEquals(40f, c.cardButton().y, 0.01f);
        c.dispose();
    }

    @Test
    void nameModifierUpdatesButtonName() {
        Card c = new Card();
        c.name("my-card");
        assertEquals("my-card", c.cardButton().name);
        c.dispose();
    }

    @Test
    void tableIsSameAsCardButton() {
        Card c = new Card();
        assertSame(c.cardButton(), c.element());
        c.dispose();
    }

    @Test
    void cellConfigReturnsNonNull() {
        Card c = new Card();
        assertNotNull(c.cellConfig());
        c.dispose();
    }

    @Test
    void bareCardDefaultsToTopLeft() {
        Card c = new Card();
        Element first = new Element();
        Element second = new Element();

        c.children(() -> {
            ParentStack.add(first);
            ParentStack.add(second);
        });

        Cell<?> c1 = c.container().getCell(first);
        Cell<?> c2 = c.container().getCell(second);

        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));
        c.dispose();
    }
}
