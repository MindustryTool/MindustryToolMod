package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Graphics;
import arc.Application;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.struct.Seq;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.test.SolimEnv;

class PopupMenuTest extends SolimEnv {

    private static void ensureScene() {
        Core.scene = new Scene();
    }

    @Test
    void placeOpensAboveAnchorWhenItFits() {
        Vec2 pos = Popup.place(100f, 100f, 120f, 80f, 800f, 600f);
        assertEquals(100f, pos.x, 0.001f);
        assertEquals(100f, pos.y, 0.001f);
    }

    @Test
    void placeFlipsBelowAnchorOnOverflow() {
        Vec2 pos = Popup.place(100f, 550f, 120f, 80f, 800f, 600f);
        assertEquals(100f, pos.x, 0.001f);
        assertEquals(470f, pos.y, 0.001f);
    }

    @Test
    void placeClampsHorizontalOverflow() {
        Vec2 pos = Popup.place(750f, 100f, 120f, 80f, 800f, 600f);
        assertEquals(680f, pos.x, 0.001f);
        assertEquals(100f, pos.y, 0.001f);
    }

    @Test
    void placeClampsNegativeAnchor() {
        Vec2 pos = Popup.place(-10f, -20f, 120f, 80f, 800f, 600f);
        assertEquals(0f, pos.x, 0.001f);
        assertEquals(0f, pos.y, 0.001f);
    }

    @Test
    void placeClampsMenuLargerThanStage() {
        Vec2 pos = Popup.place(400f, 300f, 900f, 700f, 800f, 600f);
        assertEquals(0f, pos.x, 0.001f);
        assertEquals(0f, pos.y, 0.001f);
    }

    @Test
    void fluentChainReturnsSingleInstance() {
        Scene saved = Core.scene;
        try {
            Core.scene = null;
            AtomicInteger builds = new AtomicInteger();
            Popup<String> menu = new Popup<>();
            Popup<String> chained = menu
                    .children(data -> {
                        builds.incrementAndGet();
                        return emptyContent();
                    })
                    .rounded(2)
                    .border(1f, Color.gray)
                    .show("data", 10f, 20f)
                    .hide();
            assertSame(menu, chained);
            assertEquals(0, builds.get(), "No scene means show is a no-op and provider never runs");
        } finally {
            Core.scene = saved;
        }
    }

    @Test
    void showRendersProviderContentAndHideDetaches() {
        Scene savedScene = Core.scene;
        Application savedApp = Core.app;
        Graphics savedGraphics = Core.graphics;
        Popup<String> menu = new Popup<>();
        try {
            ensureScene();
            AtomicInteger builds = new AtomicInteger();
            AtomicBoolean disposed = new AtomicBoolean(false);
            menu.children(data -> {
                builds.incrementAndGet();
                return trackedContent(disposed);
            });

            menu.show("first", 50f, 60f);
            assertEquals(1, builds.get());
            assertNotNull(menu.table().parent, "Menu table must be attached to the scene after show");

            menu.show("second", 10f, 20f);
            assertEquals(2, builds.get(), "Provider must rebuild on every show");
            assertTrue(disposed.get(), "Previous content must be disposed on rebuild");

            menu.hide();
            assertNull(menu.table().parent, "Menu table must be detached after hide");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
            Core.app = savedApp;
            Core.graphics = savedGraphics;
        }
    }

    @Test
    void nullDataShowHidesInstead() {
        Scene savedScene = Core.scene;
        Application savedApp = Core.app;
        Graphics savedGraphics = Core.graphics;
        Popup<String> menu = new Popup<>();
        try {
            ensureScene();
            menu.children(data -> emptyContent());
            menu.show("visible", 10f, 10f);
            assertNotNull(menu.table().parent);
            menu.show(null, 10f, 10f);
            assertNull(menu.table().parent, "Null-data show must hide the menu");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
            Core.app = savedApp;
            Core.graphics = savedGraphics;
        }
    }

    @Test
    void popupAppliesMarginFromRootContent() {
        Scene savedScene = Core.scene;
        Application savedApp = Core.app;
        Graphics savedGraphics = Core.graphics;
        Popup<String> popup = new Popup<>();
        try {
            ensureScene();
            popup.children(data -> new Card().margin(10f, 15f, 20f, 25f));
            popup.show("test", 100f, 100f);
            Table table = popup.table();
            Cell<?> cell = table.getCells().first();
            assertEquals(10f, CellAccess.padTop(cell), 0.01f);
            assertEquals(15f, CellAccess.padLeft(cell), 0.01f);
            assertEquals(20f, CellAccess.padBottom(cell), 0.01f);
            assertEquals(25f, CellAccess.padRight(cell), 0.01f);
        } finally {
            try {
                popup.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
            Core.app = savedApp;
            Core.graphics = savedGraphics;
        }
    }

    @Test
    void layerBehindMountsBeforeAnchorInAnchorsParent() {
        Scene savedScene = Core.scene;
        Popup<String> menu = new Popup<>();
        try {
            Scene scene = new Scene();
            Core.scene = scene;
            menu.children(data -> emptyContent());
            Table anchor = new Table();
            scene.root.addChild(anchor);
            menu.layerBehind(anchor);

            menu.show("data", 10f, 20f);

            assertSame(scene.root, menu.table().parent, "Menu must mount into the anchor's parent");
            int anchorIndex = scene.root.getChildren().indexOf(anchor, true);
            int tableIndex = scene.root.getChildren().indexOf(menu.table(), true);
            assertTrue(anchorIndex >= 0 && tableIndex >= 0, "Both elements must be children of the anchor's parent");
            assertTrue(tableIndex < anchorIndex, "Menu must be inserted before the anchor so the anchor draws above it");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
        }
    }

    @Test
    void layerBehindFallsBackToSceneRootForDetachedAnchor() {
        Scene savedScene = Core.scene;
        Popup<String> menu = new Popup<>();
        try {
            ensureScene();
            menu.children(data -> emptyContent());
            menu.layerBehind(new Table());

            menu.show("data", 10f, 20f);

            assertSame(Core.scene.root, menu.table().parent, "Detached anchor must fall back to scene root mounting");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
        }
    }

    @Test
    void outsideTapDismissesWithoutConsuming() {
        Scene savedScene = Core.scene;
        Popup<String> menu = new Popup<>();
        try {
            ensureScene();
            menu.children(data -> emptyContent());
            menu.show("data", 10f, 10f);
            assertTrue(menu.isShowing());

            Seq<EventListener> captureListeners = Core.scene.root.getCaptureListeners().copy();
            boolean hid = false;
            boolean consumed = true;
            for (EventListener listener : captureListeners) {
                if (!(listener instanceof InputListener) || !menu.isShowing()) {
                    continue;
                }
                InputEvent event = new InputEvent();
                event.stageX = 9999f;
                event.stageY = 9999f;
                consumed = ((InputListener) listener).touchDown(event, 0f, 0f, 0, KeyCode.mouseLeft);
                if (!menu.isShowing()) {
                    hid = true;
                    break;
                }
            }

            assertTrue(hid, "An outside tap must dismiss the popup");
            assertFalse(consumed, "The dismissing tap must not be consumed so underlying elements receive it");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
        }
    }

    @Test
    void insideTapKeepsMenuOpenWithoutConsuming() {
        Scene savedScene = Core.scene;
        Popup<String> menu = new Popup<>();
        try {
            ensureScene();
            menu.children(data -> fixedContent(100f, 80f));
            menu.show("data", 10f, 10f);
            assertTrue(menu.isShowing());

            Seq<EventListener> captureListeners = Core.scene.root.getCaptureListeners().copy();
            boolean anyConsumed = false;
            for (EventListener listener : captureListeners) {
                if (!(listener instanceof InputListener)) {
                    continue;
                }
                InputEvent event = new InputEvent();
                event.stageX = 10f;
                event.stageY = 10f;
                anyConsumed |= ((InputListener) listener).touchDown(event, 0f, 0f, 0, KeyCode.mouseLeft);
            }

            assertFalse(anyConsumed, "An inside tap must not be consumed by the dismissal catcher");
            assertTrue(menu.isShowing(), "An inside tap must keep the menu open");
        } finally {
            try {
                menu.dispose();
            } catch (Throwable ignored) {
            }
            Core.scene = savedScene;
        }
    }

    private static BaseComponent emptyContent() {
        return trackedContent(new AtomicBoolean(false));
    }

    private static BaseComponent trackedContent(AtomicBoolean disposed) {
        return new BaseComponent() {
            @Override
            protected Element build() {
                return new Table();
            }

            @Override
            public void dispose() {
                super.dispose();
                disposed.set(true);
            }
        };
    }

    private static BaseComponent fixedContent(float width, float height) {
        return new BaseComponent() {
            @Override
            protected Element build() {
                Table content = new Table();
                Table leaf = new Table();
                leaf.touchable = Touchable.enabled;
                content.add(leaf).size(width, height);
                return content;
            }
        };
    }
}
