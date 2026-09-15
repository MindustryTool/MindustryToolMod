package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.Element;
import arc.scene.ui.Image;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.overlay.Hud;

/**
 * Full integration tests for QuickAccessHudView.
 *
 * Verifies that the HUD's drag-handle button and grid item buttons are sized according to the
 * scale config formula: buttonSize = unit(11) * scale and iconSize = unit(7) * scale,
 * where unit(x) = x * 4.
 *
 * At default scale 1.0f: buttonSize = 44f, iconSize = 28f.
 * At scale 2.0f: buttonSize = 88f, iconSize = 56f.
 */
class QuickAccessHudViewTest {

    // BASE_UNIT in Ui = 4f
    private static final float BASE_UNIT = 4f;
    private static final float BUTTON_UNITS = 11f;
    private static final float ICON_UNITS = 7f;

    private QuickAccessFeature feature;

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();

        // Stub all Icon drawables needed:
        //   - Icon.menu   : used by QuickAccessFeature metadata
        //   - Icon.move   : used by the drag-handle button in QuickAccessHudView
        //   - Icon.settings : used by the settings item button in QuickAccessHudView
        TextureRegionDrawable stub = new TextureRegionDrawable(new TextureRegion());
        Icon.menu = stub;
        Icon.move = stub;
        Icon.settings = stub;

        feature = new QuickAccessFeature();
    }

    /**
     * Flushes all pending reactive effects via reflection on SignalDispatcher, which is an
     * internal solim-runtime class that cannot be referenced directly from the mod module.
     */
    private static void flushSignals() {
        try {
            Class<?> dispatcher = Class.forName("solim.runtime.SignalDispatcher");
            dispatcher.getMethod("flush").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to flush SignalDispatcher", e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the view, triggers element creation via element(), and returns the Hud.
     */
    private Hud buildHud() {
        QuickAccessHudView view = new QuickAccessHudView(feature);
        view.element(); // triggers build()
        Hud hud = view.getHud();
        assertNotNull(hud, "Hud must be non-null after build()");
        return hud;
    }

    /** Returns the first Arc button found in the Hud container (the drag-handle button). */
    private arc.scene.ui.Button findFirstButton(Hud hud) {
        for (Element child : hud.container().getChildren()) {
            if (child instanceof arc.scene.ui.Button) {
                return (arc.scene.ui.Button) child;
            }
        }
        fail("No button found in Hud container");
        return null; // unreachable
    }

    /**
     * Returns the first Image child of the given button element - i.e. the icon rendered inside
     * the button.
     */
    private Image findIconImage(arc.scene.ui.Button button) {
        for (Element child : button.getChildren()) {
            if (child instanceof Image) {
                return (Image) child;
            }
        }
        fail("No Image (icon) found inside button");
        return null; // unreachable
    }

    /**
     * Finds the settings item button inside the reactive grid within the Hud container.
     *
     * Container children: [drag-handle button, divider, grid table].
     * The grid table contains the per-item buttons.
     */
    private arc.scene.ui.Button findSettingsItemButton(Hud hud) {
        for (Element containerChild : hud.container().getChildren()) {
            if (containerChild instanceof arc.scene.ui.layout.Table
                    && !(containerChild instanceof arc.scene.ui.Button)) {
                arc.scene.ui.layout.Table gridTable =
                        (arc.scene.ui.layout.Table) containerChild;
                for (Element gridChild : gridTable.getChildren()) {
                    if (gridChild instanceof arc.scene.ui.Button) {
                        return (arc.scene.ui.Button) gridChild;
                    }
                }
            }
        }
        return null;
    }

    @Test
    void dragHandleButtonHasCorrectSizeAtDefaultScale() {
        // Default scale = 1f -> buttonSize = unit(11) * 1 = 44f
        float expectedSize = BUTTON_UNITS * BASE_UNIT * 1f;

        Hud hud = buildHud();
        arc.scene.ui.Button dragHandle = findFirstButton(hud);

        // ElementConfig.width(float) calls el.setWidth(val), so getWidth() reflects the applied size.
        assertEquals(expectedSize, dragHandle.getWidth(), 0.01f,
                "Drag handle button width must equal unit(11) * scale");
        assertEquals(expectedSize, dragHandle.getHeight(), 0.01f,
                "Drag handle button height must equal unit(11) * scale");

        hud.dispose();
    }

    @Test
    void dragHandleIconHasCorrectSizeAtDefaultScale() {
        // Default scale = 1f -> iconSize = unit(7) * 1 = 28f
        float expectedSize = ICON_UNITS * BASE_UNIT * 1f;

        Hud hud = buildHud();
        arc.scene.ui.Button dragHandle = findFirstButton(hud);
        Image icon = findIconImage(dragHandle);

        assertEquals(expectedSize, icon.getWidth(), 0.01f,
                "Drag handle icon width must equal unit(7) * scale");
        assertEquals(expectedSize, icon.getHeight(), 0.01f,
                "Drag handle icon height must equal unit(7) * scale");

        hud.dispose();
    }

    @Test
    void buttonAndIconSizesScaleReactivelyWithScaleConfig() {
        float scale1 = 1f;
        float scale2 = 2f;
        float expectedButton1 = BUTTON_UNITS * BASE_UNIT * scale1; // 44f
        float expectedButton2 = BUTTON_UNITS * BASE_UNIT * scale2; // 88f
        float expectedIcon1 = ICON_UNITS * BASE_UNIT * scale1;     // 28f
        float expectedIcon2 = ICON_UNITS * BASE_UNIT * scale2;     // 56f

        Hud hud = buildHud();
        arc.scene.ui.Button dragHandle = findFirstButton(hud);
        Image icon = findIconImage(dragHandle);

        // Verify initial sizes at scale = 1f
        assertEquals(expectedButton1, dragHandle.getWidth(), 0.01f,
                "Initial button width must equal unit(11) * 1f");
        assertEquals(expectedIcon1, icon.getWidth(), 0.01f,
                "Initial icon width must equal unit(7) * 1f");

        // Change scale to 2f and flush reactive graph
        feature.scaleConfig.set(scale2);
        flushSignals();

        assertEquals(expectedButton2, dragHandle.getWidth(), 0.01f,
                "Button width must update to unit(11) * 2f after scale change");
        assertEquals(expectedIcon2, icon.getWidth(), 0.01f,
                "Icon width must update to unit(7) * 2f after scale change");

        hud.dispose();
    }

    @Test
    void buttonSizesStopUpdatingAfterViewDisposal() {
        float expectedButton1 = BUTTON_UNITS * BASE_UNIT * 1f; // 44f

        QuickAccessHudView view = new QuickAccessHudView(feature);
        view.element();
        Hud hud = view.getHud();
        assertNotNull(hud);

        arc.scene.ui.Button dragHandle = findFirstButton(hud);
        assertEquals(expectedButton1, dragHandle.getWidth(), 0.01f);

        // Dispose the view - reactive bindings should stop
        view.dispose();

        // Scale change after disposal must not update the button
        feature.scaleConfig.set(2f);
        flushSignals();

        assertEquals(expectedButton1, dragHandle.getWidth(), 0.01f,
                "Button size must not change after view disposal");

        hud.dispose();
    }

    @Test
    void settingsItemButtonHasCorrectSizeAtDefaultScale() {
        // The settings item is always appended (__settings__) in computeVisibleItems.
        // It also receives .size(buttonSize).
        float expectedSize = BUTTON_UNITS * BASE_UNIT * 1f; // 44f

        Hud hud = buildHud();

        arc.scene.ui.Button settingsButton = findSettingsItemButton(hud);
        assertNotNull(settingsButton, "Settings item button must be present in the grid");

        assertEquals(expectedSize, settingsButton.getWidth(), 0.01f,
                "Settings item button width must equal unit(11) * scale");
        assertEquals(expectedSize, settingsButton.getHeight(), 0.01f,
                "Settings item button height must equal unit(11) * scale");

        hud.dispose();
    }

    @Test
    void settingsItemIconHasCorrectSizeAtDefaultScale() {
        float expectedSize = ICON_UNITS * BASE_UNIT * 1f; // 28f

        Hud hud = buildHud();

        arc.scene.ui.Button settingsButton = findSettingsItemButton(hud);
        assertNotNull(settingsButton);
        Image icon = findIconImage(settingsButton);

        assertEquals(expectedSize, icon.getWidth(), 0.01f,
                "Settings item icon width must equal unit(7) * scale");
        assertEquals(expectedSize, icon.getHeight(), 0.01f,
                "Settings item icon height must equal unit(7) * scale");

        hud.dispose();
    }
}