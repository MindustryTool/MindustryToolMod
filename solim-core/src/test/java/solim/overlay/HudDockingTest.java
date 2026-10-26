package solim.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static solim.modifier.Values.position;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.input.KeyCode;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.util.Nullable;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class HudDockingTest extends SolimEnv {

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

    private static final class TestStore implements DockAnchorStore {
        private @Nullable DockAnchor anchor;

        @Override
        public @Nullable DockAnchor load() {
            return anchor;
        }

        @Override
        public void save(@Nullable DockAnchor value) {
            anchor = value;
        }
    }

    private static final class TestTarget implements DockTarget {
        private final String id;
        private float x;
        private float y;
        private float width;
        private float height;
        private boolean active = true;

        private TestTarget(String id, float x, float y, float width, float height) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float x() {
            return x;
        }

        @Override
        public float y() {
            return y;
        }

        @Override
        public float width() {
            return width;
        }

        @Override
        public float height() {
            return height;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public boolean supports(DockSide side) {
            return side == DockSide.BOTTOM;
        }
    }

    private static ResizableMockGraphics graphics;

    @BeforeAll
    static void installGraphics() {
        graphics = new ResizableMockGraphics();
        Core.graphics = graphics;
    }

    @BeforeEach
    void resetViewport() {
        graphics.width = 1024;
        graphics.height = 768;
    }

    @Test
    void storedAnchorResolvesAgainstTargetOnInitialLayout() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f));
        TestStore store = new TestStore();
        store.anchor = new DockAnchor("minimap", DockSide.BOTTOM, 50f, 150f, 46f);

        Hud hud = new Hud();
        hud.element().setSize(100f, 50f);
        Signal<Float> x = Signal.of(500f);
        Signal<Float> y = Signal.of(300f);
        hud.dockable(new Element(), engine, store, x, y, null);
        hud.props(position(x, y));
        hud.keepInScreen();

        assertEquals(150f, hud.element().x, 0.01f, "Docked HUD follows the target's horizontal offset");
        assertEquals(46f, hud.element().y, 0.01f, "Docked HUD sits below the target with the configured gap");
        assertEquals(500f, x.get(), 0.01f, "The dock position must not overwrite the stored free position");
        assertEquals(300f, y.get(), 0.01f, "The dock position must not overwrite the stored free position");

        hud.dispose();
    }

    @Test
    void dragSnapSavesAnchorOnRelease() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f));
        TestStore store = new TestStore();

        Hud hud = new Hud();
        hud.element().setSize(100f, 50f);
        Signal<Float> x = Signal.of(500f);
        Signal<Float> y = Signal.of(300f);
        Element handle = new Element();
        hud.dockable(handle, engine, store, x, y, null);
        hud.props(position(x, y));
        hud.keepInScreen();

        InputListener drag = dragListener(handle);
        InputEvent event = new InputEvent();
        drag.touchDown(event, 10f, 10f, 0, KeyCode.mouseLeft);
        drag.touchDragged(event, -340f, -244f, 0);

        assertEquals(150f, hud.element().x, 0.01f, "Dragging within the snap threshold locks onto the target");
        assertEquals(46f, hud.element().y, 0.01f, "Dragging within the snap threshold locks onto the target");
        assertNull(store.anchor, "An anchor is only persisted once the drag is released");

        drag.touchUp(event, -340f, -244f, 0, KeyCode.mouseLeft);

        assertNotNull(store.anchor, "Release after snapping must persist the anchor");
        assertEquals("minimap", store.anchor.targetId());
        assertEquals(DockSide.BOTTOM, store.anchor.side());
        assertEquals(50f, store.anchor.offset(), 0.01f);
        assertEquals(150f, store.anchor.x(), 0.01f);
        assertEquals(46f, store.anchor.y(), 0.01f);
        assertEquals(150f, x.get(), 0.01f);
        assertEquals(46f, y.get(), 0.01f);

        SignalDispatcher.flush();
        hud.dispose();
    }

    @Test
    void dragInsideHysteresisKeepsAnchorAndBreakawayClearsIt() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f));
        TestStore store = new TestStore();
        store.anchor = new DockAnchor("minimap", DockSide.BOTTOM, 50f, 150f, 46f);

        Hud hud = new Hud();
        hud.element().setSize(100f, 50f);
        Signal<Float> x = Signal.of(500f);
        Signal<Float> y = Signal.of(300f);
        Element handle = new Element();
        hud.dockable(handle, engine, store, x, y, null);
        hud.props(position(x, y));
        hud.keepInScreen();

        InputListener drag = dragListener(handle);

        InputEvent smallDrag = new InputEvent();
        drag.touchDown(smallDrag, 10f, 10f, 0, KeyCode.mouseLeft);
        drag.touchDragged(smallDrag, 20f, 20f, 0);
        drag.touchUp(smallDrag, 20f, 20f, 0, KeyCode.mouseLeft);

        assertEquals(150f, hud.element().x, 0.01f, "Movement inside the breakaway distance stays docked");
        assertEquals(46f, hud.element().y, 0.01f, "Movement inside the breakaway distance stays docked");
        assertNotNull(store.anchor, "Small movement must keep the anchor");

        InputEvent breakaway = new InputEvent();
        drag.touchDown(breakaway, 10f, 10f, 0, KeyCode.mouseLeft);
        drag.touchDragged(breakaway, 110f, 110f, 0);
        drag.touchUp(breakaway, 110f, 110f, 0, KeyCode.mouseLeft);

        assertEquals(250f, hud.element().x, 0.01f, "Dragging past the breakaway distance frees the HUD");
        assertEquals(146f, hud.element().y, 0.01f, "Dragging past the breakaway distance frees the HUD");
        assertNull(store.anchor, "Breaking away must clear the persisted anchor");

        SignalDispatcher.flush();
        hud.dispose();
    }

    @Test
    void unavailableTargetFallsBackToSavedFreeCoordinates() {
        DockEngine engine = new DockEngine();
        TestTarget target = new TestTarget("minimap", 100f, 100f, 200f, 100f);
        engine.register(target);
        TestStore store = new TestStore();
        store.anchor = new DockAnchor("minimap", DockSide.BOTTOM, 50f, 150f, 46f);

        Hud hud = new Hud();
        hud.element().setSize(100f, 50f);
        Signal<Float> x = Signal.of(500f);
        Signal<Float> y = Signal.of(300f);
        hud.dockable(new Element(), engine, store, x, y, null);
        hud.props(position(x, y));
        target.active = false;
        hud.keepInScreen();

        assertEquals(150f, hud.element().x, 0.01f, "A hidden target falls back to the saved coordinates");
        assertEquals(46f, hud.element().y, 0.01f, "A hidden target falls back to the saved coordinates");
        assertEquals(500f, x.get(), 0.01f, "Fallback must not rewrite the stored free position");
        assertEquals(300f, y.get(), 0.01f, "Fallback must not rewrite the stored free position");

        hud.dispose();
    }

    private static InputListener dragListener(Element handle) {
        for (EventListener listener : handle.getListeners()) {
            if (listener instanceof InputListener) {
                return (InputListener) listener;
            }
        }
        throw new AssertionError("Handle must carry a drag listener");
    }
}
