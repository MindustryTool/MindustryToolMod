package mindustrytool.features.teamresource;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.layout.WidgetGroup;
import mindustry.Vars;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamResourcePositionTest {

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
        Core.app = new MockApplication();
        mockGraphics = new ResizableMockGraphics();
        Core.graphics = mockGraphics;
        Core.settings = new MockSettings();
        if (Core.gl == null) {
            Core.gl = new arc.mock.MockGL20();
            Core.gl20 = (arc.mock.MockGL20) Core.gl;
        }
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        java.lang.reflect.Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Vars.ui = (mindustry.core.UI) allocateInstance.invoke(unsafe, mindustry.core.UI.class);
        Vars.ui.hudGroup = new WidgetGroup();
        Core.scene = new Scene();
        arc.graphics.g2d.Font.FontData fontData = new arc.graphics.g2d.Font.FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }

            @Override
            public arc.graphics.g2d.Font.Glyph getGlyph(char ch) {
                arc.graphics.g2d.Font.Glyph g = super.getGlyph(ch);
                if (g == null) {
                    g = new arc.graphics.g2d.Font.Glyph();
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
        arc.graphics.g2d.Font font = new arc.graphics.g2d.Font(fontData, new arc.graphics.g2d.TextureRegion(), false);
        mindustry.ui.Fonts.def = font;

        arc.scene.ui.Label.LabelStyle labelStyle = new arc.scene.ui.Label.LabelStyle(font, arc.graphics.Color.white);
        Core.scene.addStyle(arc.scene.ui.Label.LabelStyle.class, labelStyle);
        Core.scene.addStyle(arc.scene.ui.Button.ButtonStyle.class, new arc.scene.ui.Button.ButtonStyle());
        Core.scene.addStyle(arc.scene.ui.ImageButton.ImageButtonStyle.class, new arc.scene.ui.ImageButton.ImageButtonStyle());
        arc.scene.ui.TextButton.TextButtonStyle textBtnStyle = new arc.scene.ui.TextButton.TextButtonStyle();
        textBtnStyle.font = font;
        Core.scene.addStyle(arc.scene.ui.TextButton.TextButtonStyle.class, textBtnStyle);

        arc.scene.style.Drawable emptyDrawable = new arc.scene.style.TextureRegionDrawable(new arc.graphics.g2d.TextureRegion());
        mindustry.ui.Styles.outlineLabel = labelStyle;
        mindustry.ui.Styles.clearNonei = new arc.scene.ui.ImageButton.ImageButtonStyle();
        mindustry.ui.Styles.clearTogglei = new arc.scene.ui.ImageButton.ImageButtonStyle();
        mindustry.ui.Styles.flatBordert = textBtnStyle;
        mindustry.ui.Styles.black3 = emptyDrawable;
        mindustry.ui.Styles.black6 = emptyDrawable;
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

        // Find the drag handle button (first button in header)
        // Root table -> container table -> Column -> Row -> buttons
        arc.scene.ui.layout.Table container = (arc.scene.ui.layout.Table) ((arc.scene.ui.layout.Table) root).getChildren().first();
        arc.scene.ui.layout.Table column = (arc.scene.ui.layout.Table) container.getChildren().first();
        arc.scene.ui.layout.Table headerRow = (arc.scene.ui.layout.Table) column.getChildren().first();
        Element moveButton = headerRow.getChildren().first();

        // Find InputListener on moveButton
        arc.scene.event.InputListener dragListener = null;
        for (arc.scene.event.EventListener l : moveButton.getListeners()) {
            if (l instanceof arc.scene.event.InputListener && !(l instanceof arc.scene.event.ClickListener)) {
                dragListener = (arc.scene.event.InputListener) l;
                break;
            }
        }

        assertNotNull(dragListener, "Move button must have a drag listener");

        float startX = root.x;

        // Drag to the left by 150px
        arc.scene.event.InputEvent ev = new arc.scene.event.InputEvent();
        ev.stageX = 300f;
        ev.stageY = 300f;
        dragListener.touchDown(ev, 10f, 10f, 0, arc.input.KeyCode.mouseLeft);

        ev.stageX = 150f;
        ev.stageY = 300f;
        dragListener.touchDragged(ev, 10f, 10f, 0);
        dragListener.touchUp(ev, 10f, 10f, 0, arc.input.KeyCode.mouseLeft);

        float draggedX = feature.x();
        assertEquals(startX - 150f, draggedX, 1.0f);

        // Now toggle off and on!
        feature.onDisable();
        feature.onEnable();

        float afterReEnableX = feature.x();
        assertEquals(draggedX, afterReEnableX, 1.0f, "Position must be retained after re-enabling");
    }
}
