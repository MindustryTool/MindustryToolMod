package mindustrytool.features.autoplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Readable;

class AutoplayHudPopupTest extends MindustryTestEnv {

    static class PopupSpyFeature extends AutoplayFeature {
        boolean popupToggled;
        @Nullable Element receivedBar;

        @Override
        public void togglePopup(@Nullable Element quickAccessBar) {
            popupToggled = true;
            receivedBar = quickAccessBar;
        }
    }

    @AfterEach
    void clearPlayer() {
        Vars.player = null;
    }

    private static void mockPlayer() {
        Vars.player = new Player() {
            @Override
            public Unit unit() {
                return null;
            }
        };
    }

    @Test
    void displayMode_defaultsToPopup() {
        AutoplayFeature feature = new AutoplayFeature();
        try {
            assertTrue(feature.isPopupMode());
            assertEquals(AutoplayFeature.DISPLAY_POPUP, feature.displayModeConfig.get());
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void hudModeClick_togglesEnabledWithoutPopup() {
        mockPlayer();
        PopupSpyFeature feature = new PopupSpyFeature();
        try {
            feature.displayModeConfig.set(AutoplayFeature.DISPLAY_HUD);
            assertFalse(feature.isPopupMode());
            assertFalse(feature.isEnabled());

            feature.onQuickAccessClick(null);
            assertTrue(feature.isEnabled());
            assertFalse(feature.popupToggled);

            feature.onQuickAccessClick(null);
            assertFalse(feature.isEnabled());
            assertFalse(feature.popupToggled);
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void popupModeClick_opensPopupWithoutToggling() {
        PopupSpyFeature feature = new PopupSpyFeature();
        try {
            assertTrue(feature.isPopupMode());
            Element anchor = new Element();

            feature.onQuickAccessClick(anchor);
            assertTrue(feature.popupToggled);
            assertTrue(anchor == feature.receivedBar);
            assertFalse(feature.isEnabled());
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void toggleTask_flipsDisabledState() {
        AutoplayFeature feature = new AutoplayFeature();
        try {
            String firstId = AutoplayFeature.DEFAULT_ORDER.get(0);
            assertTrue(feature.isTaskEnabled(firstId));

            feature.setTaskEnabled(firstId, false);
            assertFalse(feature.isTaskEnabled(firstId));

            feature.setTaskEnabled(firstId, true);
            assertTrue(feature.isTaskEnabled(firstId));
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void taskToggleBinding_updatesReactively() {
        AutoplayFeature feature = new AutoplayFeature();
        try {
            String firstId = AutoplayFeature.DEFAULT_ORDER.get(0);
            Readable<Boolean> bound = feature.disabledTasks.signal()
                    .map(disabled -> disabled == null || !disabled.contains(firstId));

            flushEffects();
            assertTrue(bound.peek());

            feature.setTaskEnabled(firstId, false);
            flushEffects();
            assertFalse(bound.peek());

            feature.setTaskEnabled(firstId, true);
            flushEffects();
            assertTrue(bound.peek());
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void popupActive_requiresPopupModeAndQuickAccessOn() {
        AutoplayFeature feature = new AutoplayFeature();
        QuickAccessFeature quickAccess = new QuickAccessFeature();
        FeatureManager.register(quickAccess);
        try {
            assertTrue(quickAccess.isEnabled());
            assertTrue(feature.isPopupActive());

            quickAccess.disable();
            assertFalse(quickAccess.isEnabled());
            assertFalse(feature.isPopupActive());

            feature.displayModeConfig.set(AutoplayFeature.DISPLAY_HUD);
            assertFalse(feature.isPopupActive());
        } finally {
            feature.disable();
            quickAccess.disable();
            flushEffects();
        }
    }

}
