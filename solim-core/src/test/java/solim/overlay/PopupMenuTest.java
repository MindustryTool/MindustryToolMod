package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Graphics;
import arc.Application;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.ui.Styles;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.core.Ui;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;

class PopupMenuTest {

    private static void ensureScene() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
        Core.scene = new Scene();
        if (Styles.black6 == null) {
            Styles.black6 = new TextureRegionDrawable(new TextureRegion());
        }
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
            popup.children(data -> Ui.card().margin(10f, 15f, 20f, 25f));
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
}
