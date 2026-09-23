package mindustrytool.features.teamresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.Font.Glyph;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.Scene;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.Seq;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.ctype.Content;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class TeamResourcePositionTest extends MindustryTestEnv {

    static class ResizableMockGraphics extends MockGraphics {
        int width = 1000;
        int height = 800;

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private ResizableMockGraphics mockGraphics;

    @BeforeEach
    void setUp() throws Exception {
        mockGraphics = new ResizableMockGraphics();
        Core.graphics = mockGraphics;
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Vars.ui = (UI) allocateInstance.invoke(unsafe, UI.class);
        Vars.ui.hudGroup = new WidgetGroup();
        Core.scene = new Scene();
        Core.scene.resize(mockGraphics.width, mockGraphics.height);
        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }

            @Override
            public Glyph getGlyph(char ch) {
                Glyph g = super.getGlyph(ch);
                if (g == null) {
                    g = new Glyph();
                    g.id = ch;
                    g.width = 8;
                    g.height = 12;
                    g.xadvance = 8;
                    setGlyph(ch, g);
                }
                return g;
            }
        };
        fontData.lineHeight = 18f;
        fontData.capHeight = 14f;
        Font font = new Font(fontData, new TextureRegion(), false);
        Fonts.def = font;

        LabelStyle labelStyle = new LabelStyle(font, Color.white);
        Core.scene.addStyle(LabelStyle.class, labelStyle);
        Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
        Core.scene.addStyle(ImageButtonStyle.class, new ImageButtonStyle());
        TextButtonStyle textBtnStyle = new TextButtonStyle();
        textBtnStyle.font = font;
        Core.scene.addStyle(TextButtonStyle.class, textBtnStyle);

        Drawable emptyDrawable = new TextureRegionDrawable(new TextureRegion());
        Styles.outlineLabel = labelStyle;
        Styles.clearNonei = new ImageButtonStyle();
        Styles.clearTogglei = new ImageButtonStyle();
        Styles.flatBordert = textBtnStyle;
        Styles.black3 = emptyDrawable;
        Styles.black6 = emptyDrawable;
    }

    @AfterEach
    void drainPendingEffects() {
        // Tests here build reactive components without disposing them; flush so
        // the env teardown sees an empty dispatcher.
        flushEffects();
    }

    @AfterEach
    void tearDown() {
        // Undo Vars.ui allocation so it cannot leak into other test classes
        // sharing this JVM.
        Vars.ui = null;
        flushEffects();
    }

    private static Button findFirstButton(Element element) {
        if (element instanceof Button) {
            return (Button) element;
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                Button b = findFirstButton(child);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    @Test
    void testDefaultPositionIsCentered() {
        TeamResourceFeature feature = new TeamResourceFeature();

        // With screen width = 1000 and overlay-width = 0.28 (280px),
        // centered X should be (1000 - 280) / 2 = 360px
        float expectedX = (1000f - 1000f * 0.28f) / 2f;
        assertEquals(expectedX, feature.x(), 1.0f, "Default position must be centered horizontally");
        assertEquals(400f, feature.y(), 1.0f, "Default Y should be screenHeight / 2");
    }

    @Test
    void testResetPositionCentersOverlay() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.x(120f);
        feature.y(250f);
        assertEquals(120f, feature.x(), 1.0f);

        feature.resetPosition();

        float expectedX = (1000f - 1000f * 0.28f) / 2f;
        assertEquals(expectedX, feature.x(), 1.0f, "resetPosition() must center horizontally");
        assertEquals(400f, feature.y(), 1.0f, "resetPosition() must reset Y");
    }

    @Test
    void testMigrationFromOldSettings() {
        Core.settings.put("mindustrytool.team-resource.x.landscape", 440f);
        Core.settings.put("mindustrytool.team-resource.y.landscape", 520f);

        TeamResourceFeature feature = new TeamResourceFeature();
        assertEquals(440f, feature.x(), 1.0f, "Old setting key should be migrated");
        assertEquals(520f, feature.y(), 1.0f, "Old setting key should be migrated");
    }

    @Test
    void testDragAndToggleRetainsPosition() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.onEnable();
        Element root = feature.getHudView().element();

        Button moveButton = findFirstButton(root);
        assertNotNull(moveButton, "Move button must exist in HUD");

        InputListener dragListener = null;
        for (EventListener l : moveButton.getListeners()) {
            if (l instanceof InputListener && !(l instanceof ClickListener)) {
                dragListener = (InputListener) l;
                break;
            }
        }

        assertNotNull(dragListener, "Move button must have a drag listener");

        float startX = root.x;

        // Drag to the left by 150px
        InputEvent ev = new InputEvent();
        ev.stageX = 300f;
        ev.stageY = 300f;
        dragListener.touchDown(ev, 10f, 10f, 0, KeyCode.mouseLeft);

        ev.stageX = 150f;
        ev.stageY = 300f;
        dragListener.touchDragged(ev, 10f, 10f, 0);
        dragListener.touchUp(ev, 10f, 10f, 0, KeyCode.mouseLeft);

        float draggedX = feature.x();
        assertEquals(startX - 150f, draggedX, 1.0f);

        // Now toggle off and on
        feature.onDisable();
        feature.onEnable();

        float afterReEnableX = feature.x();
        assertEquals(draggedX, afterReEnableX, 1.0f, "Position must be retained after re-enabling");
    }

    @Test
    void testPositionOnTeamSelectAndItemChanges() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.onEnable();
        TeamResourceState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

        // Simulate teams existing
        Seq<Team> teams = new Seq<>();
        teams.add(Team.sharded);
        teams.add(Team.crux);
        state.validTeamsSignal.set(teams);

        root.validate();
        assertEquals(initialX, feature.x(), 1.0f, "Position must not change when teams are updated");
        assertEquals(initialX, root.x, 1.0f, "Root X must match feature X");

        // Select team (previously reported as causing position change)
        state.setSelectedTeam(Team.crux);
        root.validate();
        assertEquals(initialX, feature.x(), 1.0f, "Position must not change when team is selected");
        assertEquals(initialX, root.x, 1.0f, "Root X must match feature X");

        // Toggle feature off and on
        feature.onDisable();
        feature.onEnable();
        Element root2 = feature.getHudView().element();
        root2.validate();
        assertEquals(initialX, feature.x(), 1.0f, "Position must not snap after re-enabling");
        assertEquals(initialX, root2.x, 1.0f, "New root X must match feature X");
    }

    @Test
    void testLayoutPrefWidthWithItems() {
        mockGraphics.width = 1000;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        TeamResourceFeature feature = new TeamResourceFeature();
        feature.onEnable();
        TeamResourceState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

        // Add 16 items
        Seq<Item> items = new Seq<>();
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            Field nameField = Content.class.getDeclaredField("name");
            nameField.setAccessible(true);
            for (int i = 0; i < 16; i++) {
                Item it = (Item) allocateInstance.invoke(unsafe, Item.class);
                nameField.set(it, "item-" + i);
                it.uiIcon = new TextureRegion();
                items.add(it);
            }
        } catch (Exception ignored) {
        }
        state.usedItemsSignal.set(items);

        // Add teams
        Seq<Team> teams = new Seq<>();
        teams.add(Team.sharded);
        teams.add(Team.crux);
        state.validTeamsSignal.set(teams);

        root.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable with items");
        assertEquals(initialX, root.x, 1.0f, "Root X must remain stable with items");

        // Toggle off and on
        feature.onDisable();
        feature.onEnable();
        Element root2 = feature.getHudView().element();
        root2.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable after toggle with items");
        assertEquals(initialX, root2.x, 1.0f, "Root2 X must remain stable after toggle with items");

        // Select team
        state.setSelectedTeam(Team.crux);
        root2.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable after team selection with items");
        assertEquals(initialX, root2.x, 1.0f, "Root2 X must remain stable after team selection with items");
    }

    @Test
    void testTransientLayoutDoesNotCorruptSignal() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.onEnable();
        Element root = feature.getHudView().element();

        feature.x(100f);
        feature.y(150f);
        root.validate();

        assertEquals(100f, feature.x(), 1.0f);
        assertEquals(150f, feature.y(), 1.0f);

        // Manually expand root width beyond screen boundary to simulate transient large layout
        root.setSize(1200f, 600f);
        root.validate();

        // The signal must NOT be corrupted to sw - w (which would be 0 or clamped right-aligned)
        assertEquals(100f, feature.x(), 1.0f, "boundXSignal must not be overwritten during layout validation");
        assertEquals(150f, feature.y(), 1.0f, "boundYSignal must not be overwritten during layout validation");
    }

    @Test
    void testLongTextExpandsPrefWidth() {
        mockGraphics.width = 1000;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        TeamResourceFeature feature = new TeamResourceFeature();
        feature.onEnable();
        TeamResourceState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

        // Enable power and stored power
        feature.showPowerConfig.set(true);
        feature.showStoredPowerConfig.set(true);

        // Add a mock power graph with huge stored power
        PowerGraph graph = new PowerGraph();
        try {
            Field capField = PowerGraph.class.getDeclaredField("lastCapacity");
            capField.setAccessible(true);
            capField.set(graph, 99999999f);
        } catch (Exception ignored) {
        }
        state.getTeamGraphs().add(graph);
        state.tickSignal.set(state.tickSignal.get() + 1);

        root.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable with power stats");
        assertEquals(initialX, root.x, 1.0f, "Root X must remain stable with power stats");
    }

    @Test
    void testHideHeaderAndDragHandleConfigs() {
        TeamResourceFeature feature = new TeamResourceFeature();

        assertFalse(feature.hideDragHandleConfig.get(), "hideDragHandleConfig defaults to false");
        assertFalse(feature.hideHeaderConfig.get(), "hideHeaderConfig defaults to false");

        feature.hideDragHandleConfig.set(true);
        feature.hideHeaderConfig.set(true);
        assertTrue(feature.hideDragHandleConfig.get());
        assertTrue(feature.hideHeaderConfig.get());

        feature.onEnable();
        Element root = feature.getHudView().element();
        assertNotNull(root, "HUD root must build cleanly when hideHeader is enabled");
        root.validate();

        feature.resetToDefaults();
        assertFalse(feature.hideDragHandleConfig.get(), "resetToDefaults resets hideDragHandleConfig");
        assertFalse(feature.hideHeaderConfig.get(), "resetToDefaults resets hideHeaderConfig");

        flushEffects();
    }
}
