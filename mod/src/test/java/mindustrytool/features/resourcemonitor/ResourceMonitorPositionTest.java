package mindustrytool.features.resourcemonitor;

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
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.Seq;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.ctype.Content;
import mindustry.ctype.MappableContent;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class ResourceMonitorPositionTest extends MindustryTestEnv {

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
        Core.scene.addStyle(ScrollPaneStyle.class,
                new ScrollPaneStyle());

        Drawable emptyDrawable = new TextureRegionDrawable(new TextureRegion());
        Styles.outlineLabel = labelStyle;
        Styles.clearNonei = new ImageButtonStyle();
        Styles.clearTogglei = new ImageButtonStyle();
        Styles.flatBordert = textBtnStyle;
        Styles.black3 = emptyDrawable;
        Styles.black6 = emptyDrawable;
    }

    @AfterEach
    void tearDown() {

        flushEffects();
        Vars.ui = null;
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
        ResourceMonitorFeature feature = new ResourceMonitorFeature();

        float expectedX = (1000f - 1000f * 0.28f) / 2f;
        assertEquals(expectedX, feature.x(), 1.0f, "Default position must be centered horizontally");
        assertEquals(400f, feature.y(), 1.0f, "Default Y should be screenHeight / 2");
    }

    @Test
    void testResetPositionCentersOverlay() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
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

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        assertEquals(440f, feature.x(), 1.0f, "Old setting key should be migrated");
        assertEquals(520f, feature.y(), 1.0f, "Old setting key should be migrated");
    }

    @Test
    void testDragAndToggleRetainsPosition() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
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

        feature.onDisable();
        feature.onEnable();

        float afterReEnableX = feature.x();
        assertEquals(draggedX, afterReEnableX, 1.0f, "Position must be retained after re-enabling");
    }

    @Test
    void testPositionOnTeamSelectAndItemChanges() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

        Seq<Team> teams = new Seq<>();
        teams.add(Team.sharded);
        teams.add(Team.crux);
        state.validTeamsSignal.set(teams);

        root.validate();
        assertEquals(initialX, feature.x(), 1.0f, "Position must not change when teams are updated");
        assertEquals(initialX, root.x, 1.0f, "Root X must match feature X");

        state.setSelectedTeam(Team.crux);
        root.validate();
        assertEquals(initialX, feature.x(), 1.0f, "Position must not change when team is selected");
        assertEquals(initialX, root.x, 1.0f, "Root X must match feature X");

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

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

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

        Seq<Team> teams = new Seq<>();
        teams.add(Team.sharded);
        teams.add(Team.crux);
        state.validTeamsSignal.set(teams);

        root.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable with items");
        assertEquals(initialX, root.x, 1.0f, "Root X must remain stable with items");

        feature.onDisable();
        feature.onEnable();
        Element root2 = feature.getHudView().element();
        root2.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable after toggle with items");
        assertEquals(initialX, root2.x, 1.0f, "Root2 X must remain stable after toggle with items");

        state.setSelectedTeam(Team.crux);
        root2.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable after team selection with items");
        assertEquals(initialX, root2.x, 1.0f, "Root2 X must remain stable after team selection with items");
    }

    @Test
    void testTransientLayoutDoesNotCorruptSignal() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        Element root = feature.getHudView().element();

        feature.x(100f);
        feature.y(150f);
        root.validate();

        assertEquals(100f, feature.x(), 1.0f);
        assertEquals(150f, feature.y(), 1.0f);

        root.setSize(1200f, 600f);
        root.validate();

        assertEquals(100f, feature.x(), 1.0f, "boundXSignal must not be overwritten during layout validation");
        assertEquals(150f, feature.y(), 1.0f, "boundYSignal must not be overwritten during layout validation");
    }

    @Test
    void testLongTextExpandsPrefWidth() {
        mockGraphics.width = 1000;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        float initialX = feature.x();

        feature.showPowerConfig.set(true);
        feature.showStoredPowerConfig.set(true);

        PowerGraph graph = new PowerGraph();
        try {
            Field capField = PowerGraph.class.getDeclaredField("lastCapacity");
            capField.setAccessible(true);
            capField.set(graph, 99999999f);
        } catch (Exception ignored) {
        }
        state.getTeamGraphs().add(graph);
        state.tickSignal.set(state.tickSignal.get() + 1);

        flushEffects();
        root.validate();

        assertEquals(initialX, feature.x(), 1.0f, "Feature X must remain stable with power stats");
        assertEquals(initialX, root.x, 1.0f, "Root X must remain stable with power stats");

        feature.onDisable();
        flushEffects();
    }

    @Test
    void testHideHeaderAndDragHandleConfigs() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();

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

        feature.onDisable();
        flushEffects();
    }

    @Test
    void testResourceAmountChangesDoNotShiftLayout() throws Exception {
        mockGraphics.width = 1000;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.alwaysShowFlowRateConfig.set(true);
        feature.onEnable();
        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Field nameField = MappableContent.class.getDeclaredField("name");
        nameField.setAccessible(true);

        Seq<Item> items = new Seq<>();
        for (int i = 0; i < 6; i++) {
            Item it = (Item) allocateInstance.invoke(unsafe, Item.class);
            nameField.set(it, "item-" + i);
            it.id = (short) i;
            it.uiIcon = new TextureRegion();
            items.add(it);
        }
        state.usedItemsSignal.set(items);
        flushEffects();
        root.validate();

        float widthBefore = root.getWidth();
        float heightBefore = root.getHeight();
        float xBefore = root.x;
        float yBefore = root.y;

        state.setItemRate(items.get(0), 12500);
        state.setItemRate(items.get(1), -9800);

        for (Element child : ((Group) root).getChildren()) {
            child.act(0.5f);
        }
        root.validate();

        assertEquals(widthBefore, root.getWidth(), 0.01f, "HUD width must not change when item flow rates fluctuate");
        assertEquals(heightBefore, root.getHeight(), 0.01f, "HUD height must not change when item flow rates fluctuate");
        assertEquals(xBefore, root.x, 0.01f, "HUD X must remain stable");
        assertEquals(yBefore, root.y, 0.01f, "HUD Y must remain stable");

        float topYBefore = root.y + root.getHeight();
        Seq<Item> moreItems = new Seq<>(items);
        for (int i = 6; i < 12; i++) {
            Item it = (Item) allocateInstance.invoke(unsafe, Item.class);
            nameField.set(it, "item-" + i);
            it.id = (short) i;
            it.uiIcon = new TextureRegion();
            moreItems.add(it);
        }
        state.usedItemsSignal.set(moreItems);
        flushEffects();
        root.validate();

        assertEquals(widthBefore, root.getWidth(), 0.01f, "HUD width must remain locked when more items are discovered");
        assertEquals(xBefore, root.x, 0.01f, "HUD X must remain stable when more items are discovered");
        assertEquals(topYBefore, root.y + root.getHeight(), 0.5f, "HUD top edge must remain anchored when new rows are added");

        feature.onDisable();
        flushEffects();
    }

    @Test
    void testColumnsConfigChangesRowCount() throws Exception {
        mockGraphics.width = 1000;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Field nameField = MappableContent.class.getDeclaredField("name");
        nameField.setAccessible(true);

        Seq<Item> items = new Seq<>();
        for (int i = 0; i < 8; i++) {
            Item it = (Item) allocateInstance.invoke(unsafe, Item.class);
            nameField.set(it, "item-" + i);
            it.id = (short) i;
            it.uiIcon = new TextureRegion();
            items.add(it);
        }
        state.usedItemsSignal.set(items);
        flushEffects();
        root.validate();

        float heightAuto = root.getHeight();

        feature.columnsConfig.set(8);
        flushEffects();
        root.validate();

        float height8Cols = root.getHeight();
        assertTrue(height8Cols < heightAuto, "8 fixed columns with 8 items must produce a shorter 1-row layout than Auto (2 cols)");

        feature.resetToDefaults();
        assertEquals(1, feature.columnsConfig.get(), "resetToDefaults resets columnsConfig back to Auto (1)");

        feature.onDisable();
        flushEffects();
    }

    @Test
    void testPreviewViewReactsToConfigChanges() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        ResourceMonitorPreviewView preview = new ResourceMonitorPreviewView(feature);
        Element stage = preview.element();
        assertNotNull(stage, "Preview stage must build cleanly");
        assertTrue(stage instanceof Group);

        Group stageGroup = (Group) stage;
        assertEquals(1, stageGroup.getChildren().size, "Preview stage must contain the mock HUD");
        Element hudBefore = stageGroup.getChildren().first();
        assertEquals(1f, hudBefore.color.a, 0.01f);

        feature.opacityConfig.set(0.45f);
        feature.showUnitsConfig.set(true);
        feature.showStoredPowerConfig.set(true);
        feature.hideHeaderConfig.set(true);
        feature.columnsConfig.set(4);
        flushEffects();

        Element hudAfter = stageGroup.getChildren().first();
        assertEquals(0.45f, hudAfter.color.a, 0.01f, "Preview HUD must reflect updated opacity");

        preview.dispose();
        flushEffects();
    }

    @Test
    void testAutoWidthConfigScalesWithColumns() {
        mockGraphics.width = 1600;
        mockGraphics.height = 900;
        Scl.setProduct(1.0f);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.onEnable();
        Element root = feature.getHudView().element();
        root.validate();

        feature.autoWidthConfig.set(true);
        feature.columnsConfig.set(2);
        flushEffects();
        root.validate();
        float width2Cols = root.getWidth();

        feature.columnsConfig.set(4);
        flushEffects();
        root.validate();
        float width4Cols = root.getWidth();

        assertTrue(width4Cols > width2Cols + 100f,
                "Auto-Fit Width must expand HUD width proportionally when columns increase from 2 to 4");

        feature.resetToDefaults();
        assertFalse(feature.autoWidthConfig.get(), "resetToDefaults must reset autoWidthConfig to false");

        feature.onDisable();
        flushEffects();
    }

    @Test
    void testUnitColumnsAndCompactUnitsConfig() throws Exception {
        mockGraphics.width = 1200;
        mockGraphics.height = 800;
        Scl.setProduct(1.0f);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.showItemsConfig.set(false);
        feature.showPowerConfig.set(false);
        feature.showUnitsConfig.set(true);
        feature.columnsConfig.set(2);
        feature.onEnable();

        ResourceMonitorState state = feature.getState();
        Element root = feature.getHudView().element();

        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Field nameField = MappableContent.class.getDeclaredField("name");
        nameField.setAccessible(true);

        Seq<UnitType> units = new Seq<>();
        for (int i = 0; i < 6; i++) {
            UnitType ut = (UnitType) allocateInstance.invoke(unsafe, UnitType.class);
            nameField.set(ut, "unit-" + i);
            ut.id = (short) i;
            ut.uiIcon = new TextureRegion();
            units.add(ut);
        }
        state.usedUnitsSignal.set(units);
        flushEffects();
        root.validate();

        float height2Cols = root.getHeight();

        feature.unitColumnsConfig.set(6);
        flushEffects();
        root.validate();
        float height6Cols = root.getHeight();
        assertTrue(height6Cols < height2Cols,
                "Setting unitColumnsConfig=6 with 6 units must produce a shorter 1-row unit section than 2 columns");

        feature.compactUnitsConfig.set(true);
        flushEffects();
        root.validate();
        assertTrue(feature.compactUnitsConfig.get());

        feature.resetToDefaults();
        assertEquals(1, feature.unitColumnsConfig.get(), "resetToDefaults must reset unitColumnsConfig to 1");
        assertFalse(feature.compactUnitsConfig.get(), "resetToDefaults must reset compactUnitsConfig to false");

        feature.onDisable();
        flushEffects();
    }
}
