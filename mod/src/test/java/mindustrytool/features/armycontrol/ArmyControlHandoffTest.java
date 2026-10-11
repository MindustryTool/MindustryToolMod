package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Scene;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.core.UI;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.input.DesktopInput;
import mindustry.ui.Fonts;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class ArmyControlHandoffTest extends MindustryTestEnv {

    private static Control createMockControl() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            return (Control) allocate.invoke(unsafe, Control.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UI savedUi;

    @BeforeEach
    void init() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        if (Groups.unit == null) {
            Groups.init();
        }
        if (Vars.control == null) {
            Vars.control = createMockControl();
        }
        Vars.control.input = new DesktopInput();
        Vars.control.input.selectedUnits.clear();
        Icon.map = new TextureRegionDrawable(new TextureRegion());
        Icon.zoom = new TextureRegionDrawable(new TextureRegion());
        Icon.exit = new TextureRegionDrawable(new TextureRegion());
        Icon.add = new TextureRegionDrawable(new TextureRegion());
        Icon.trash = new TextureRegionDrawable(new TextureRegion());
        Icon.move = new TextureRegionDrawable(new TextureRegion());
        Fonts.def = new Font(new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        }, new TextureRegion(), false);
        savedUi = Vars.ui;
        Vars.ui = stubUi();
        ArmyControlPopup.hide();
    }

    @AfterEach
    void restoreUi() {
        Vars.ui = savedUi;
        savedUi = null;
        ArmyControlPopup.hide();
    }

    private static UI stubUi() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            UI ui = (UI) allocate.invoke(unsafe, UI.class);
            ui.hudGroup = new WidgetGroup();
            return ui;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPopupModeWithoutQuickAccessFallsBackToHud() {
        ArmyControlFeature feature = new ArmyControlFeature();
        feature.setEnabled(true);
        assertNotNull(hudViewOf(feature));
        feature.setEnabled(false);
    }

    @Test
    void testSwitchToPopupModeRemovesHud() {
        ArmyControlFeature feature = new ArmyControlFeature();
        QuickAccessFeature quickAccess = new QuickAccessFeature();
        FeatureManager.register(quickAccess);
        quickAccess.enabled().set(false);
        feature.setEnabled(true);
        assertNotNull(hudViewOf(feature));

        quickAccess.enabled().set(true);
        feature.updateHud();
        assertNull(hudViewOf(feature));
        feature.setEnabled(false);
    }

    @Test
    void testSwitchToHudModeHidesPopupAndBuildsHud() {
        ArmyControlFeature feature = new ArmyControlFeature();
        QuickAccessFeature quickAccess = new QuickAccessFeature();
        FeatureManager.register(quickAccess);
        quickAccess.enabled().set(true);
        feature.setEnabled(true);
        feature.displayModeConfig.set(ArmyControlFeature.DISPLAY_POPUP);

        Core.scene = new Scene();
        ArcTestEnv.installMinimalStyles(Core.scene);
        Table bar = new Table();
        ArmyControlPopup.show(feature, bar);
        assertEquals(true, ArmyControlPopup.isShowing());

        feature.displayModeConfig.set(ArmyControlFeature.DISPLAY_HUD);
        assertEquals(false, ArmyControlPopup.isShowing());
        assertNotNull(hudViewOf(feature));
        feature.setEnabled(false);
        ArmyControlPopup.hide();
    }

    private static Object hudViewOf(ArmyControlFeature feature) {
        try {
            Field field = ArmyControlFeature.class.getDeclaredField("hudView");
            field.setAccessible(true);
            return field.get(feature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
