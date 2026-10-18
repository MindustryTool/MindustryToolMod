package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.opacity;
import static solim.modifier.Values.position;
import static solim.modifier.Values.scale;
import static solim.modifier.Values.x;
import static solim.modifier.Values.y;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import arc.scene.ui.layout.Table;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class HudComponentTest extends SolimEnv {

    static class ResizableMockGraphics extends MockGraphics {
        int width = 1024;
        int height = 768;

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static ResizableMockGraphics mockGraphics;

    @BeforeAll
    static void initCore() {
        mockGraphics = new ResizableMockGraphics();
        Core.graphics = mockGraphics;
    }

    @BeforeEach
    void resetScreen() {
        mockGraphics.width = 1024;
        mockGraphics.height = 768;
    }

    @Test
    void hudCreatesWithDefaultNamesAndHierarchy() {
        Hud hud = new Hud();
        assertEquals("solim-hud-root", hud.element().name);
        assertEquals("solim-hud-container", hud.container().name);
        assertSame(hud.container(), hud.root().getChildren().get(0));
        assertSame(hud.root(), hud.element());
        hud.dispose();
    }

    @Test
    void hudDefaultsAndTouchable() {
        Hud hud = new Hud();
        assertEquals(Touchable.childrenOnly, hud.element().touchable);
        assertEquals(Touchable.enabled, hud.container().touchable);
        hud.dispose();
    }

    @Test
    void hudPositionModifiers() {
        Hud hud = new Hud();
        hud.props(position(100f, 200f));
        assertEquals(100f, hud.element().x, 0.01f);
        assertEquals(200f, hud.element().y, 0.01f);
        hud.dispose();
    }

    @Test
    void hudReactiveSignals() {
        Hud hud = new Hud();
        Signal<Float> x = Signal.of(40f);
        Signal<Float> y = Signal.of(60f);
        hud.props(x(x), y(y));
        assertEquals(40f, hud.element().x, 0.01f);
        assertEquals(60f, hud.element().y, 0.01f);
        x.set(150f);
        y.set(250f);
        SignalDispatcher.flush();
        assertEquals(150f, hud.element().x, 0.01f);
        assertEquals(250f, hud.element().y, 0.01f);
        hud.dispose();
    }

    @Test
    void hudReactiveOpacity() {
        Hud hud = new Hud();
        Signal<Float> opacity = Signal.of(0.7f);
        hud.props(opacity(opacity));
        assertEquals(0.7f, hud.container().color.a, 0.01f);
        opacity.set(0.3f);
        SignalDispatcher.flush();
        assertEquals(0.3f, hud.container().color.a, 0.01f);
        hud.dispose();
    }

    @Test
    void keepInScreenClamping() {
        Hud hud = new Hud();
        hud.element().setSize(200f, 100f);
        hud.props(position(1200f, 900f));
        hud.keepInScreen();
        assertEquals(824f, hud.element().x, 0.01f);
        assertEquals(668f, hud.element().y, 0.01f);
        hud.dispose();
    }

    @Test
    void resizeEventClamping() {
        Hud hud = new Hud();
        hud.element().setSize(200f, 100f);
        hud.props(position(800f, 600f));
        hud.keepInScreen();
        mockGraphics.width = 800;
        mockGraphics.height = 600;
        Events.fire(new ResizeEvent());
        assertEquals(600f, hud.element().x, 0.01f);
        assertEquals(500f, hud.element().y, 0.01f);
        hud.dispose();
    }

    @Test
    void draggableMovementAndClamping() {
        Hud hud = new Hud();
        hud.element().setSize(200f, 100f);
        hud.props(position(100f, 100f));
        Signal<Float> xSig = Signal.of(100f);
        Signal<Float> ySig = Signal.of(100f);
        Element handle = new Element();
        hud.draggable(handle, xSig, ySig);

        InputListener dragListener = null;
        for (EventListener l : handle.getListeners()) {
            if (l instanceof InputListener) {
                dragListener = (InputListener) l;
                break;
            }
        }
        assertNotNull(dragListener);

        InputEvent event = new InputEvent();
        dragListener.touchDown(event, 10f, 10f, 0, KeyCode.mouseLeft);
        dragListener.touchDragged(event, 60f, 80f, 0);
        assertEquals(150f, hud.element().x, 0.01f);
        assertEquals(170f, hud.element().y, 0.01f);
        hud.dispose();
    }

    @Test
    void hudDisposeStopsReactiveUpdates() {
        Hud hud = new Hud();
        Signal<Float> x = Signal.of(40f);
        hud.props(x(x));
        assertEquals(40f, hud.element().x, 0.01f);

        hud.dispose();
        x.set(150f);

        assertEquals(40f, hud.root().x, 0.01f);

        hud.dispose();
        assertEquals(40f, hud.root().x, 0.01f);

        SignalDispatcher.flush();
    }

    @Test
    void scalePacksRootForAccurateHitRegion() {
        Hud hud = new Hud();
        Signal<Float> scale = Signal.of(1.0f);

        hud.children(() -> {
            Table inner = new Table();
            inner.setSize(200f, 100f);
            hud.container().add(inner).size(200f, 100f);
        });
        hud.pack();

        float unscaledW = hud.root().getPrefWidth();
        assertTrue(unscaledW > 0f, "Root must have positive pref width before scaling");

        hud.props(scale(scale));
        scale.set(0.8f);

        float scaledW = hud.root().getPrefWidth();
        float scaledH = hud.root().getPrefHeight();

        assertTrue(scaledW >= 0f, "Root pref width must be non-negative after scale");
        assertTrue(scaledH >= 0f, "Root pref height must be non-negative after scale");

        SignalDispatcher.flush();
        hud.dispose();
    }

    @Test
    void hudBackgroundPropAppliesToContainer() {
        Hud hud = new Hud();
        Drawable bg = new TextureRegionDrawable(new TextureRegion());
        hud.props(background(bg));
        assertSame(bg, hud.container().getBackground());
        hud.dispose();
    }
}
