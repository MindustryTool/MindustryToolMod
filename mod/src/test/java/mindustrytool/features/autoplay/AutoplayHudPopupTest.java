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

    @Test
    void taskVisibility_togglesAndUpdatesVisibleTasksReactively() {
        AutoplayFeature feature = new AutoplayFeature();
        try {
            String firstId = AutoplayFeature.DEFAULT_ORDER.get(0);
            assertTrue(feature.isTaskVisible(firstId));
            int initialVisibleCount = feature.visibleTasks().peek().size;
            assertEquals(feature.tasks().peek().size, initialVisibleCount);

            feature.setTaskVisible(firstId, false);
            flushEffects();
            assertFalse(feature.isTaskVisible(firstId));
            assertEquals(initialVisibleCount - 1, feature.visibleTasks().peek().size);
            assertFalse(feature.visibleTasks().peek().contains(t -> t.getId().equals(firstId)));
            // All tasks list remains unchanged
            assertEquals(initialVisibleCount, feature.tasks().peek().size);

            feature.setTaskVisible(firstId, true);
            flushEffects();
            assertTrue(feature.isTaskVisible(firstId));
            assertEquals(initialVisibleCount, feature.visibleTasks().peek().size);
            assertTrue(feature.visibleTasks().peek().contains(t -> t.getId().equals(firstId)));
        } finally {
            feature.disable();
            flushEffects();
        }
    }

    private static java.util.List<arc.scene.ui.Button> findAllButtons(Element root) {
        java.util.List<arc.scene.ui.Button> list = new java.util.ArrayList<>();
        java.util.Deque<Element> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Element current = queue.poll();
            if (current instanceof arc.scene.ui.Button) {
                list.add((arc.scene.ui.Button) current);
            }
            if (current instanceof arc.scene.Group) {
                for (Element child : ((arc.scene.Group) current).getChildren()) {
                    queue.add(child);
                }
            }
        }
        return list;
    }

    private static void fireLongClick(arc.scene.ui.Button button) throws Exception {
        solim.core.Component comp = solim.core.SolimToken.getComponent(button);
        assertTrue(comp instanceof solim.input.Button, "Controls strip button must be Solim button");
        java.lang.reflect.Field field = solim.input.Button.class.getDeclaredField("onLongClick");
        field.setAccessible(true);
        Runnable action = (Runnable) field.get(comp);
        org.junit.jupiter.api.Assertions.assertNotNull(action, "Button must have onLongClick handler");
        action.run();
    }

    private static void setupTestScene() {
        if (arc.Core.audio == null) {
            arc.Core.audio = new arc.mock.MockAudio();
        }
        newScene();
        arc.graphics.g2d.Font.FontData fontData = new arc.graphics.g2d.Font.FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        arc.graphics.g2d.Font font = new arc.graphics.g2d.Font(fontData, new arc.graphics.g2d.TextureRegion(), false);
        arc.Core.scene.addStyle(arc.scene.ui.Button.ButtonStyle.class, new arc.scene.ui.Button.ButtonStyle());
        arc.scene.ui.Dialog.DialogStyle dialogStyle = new arc.scene.ui.Dialog.DialogStyle();
        dialogStyle.titleFont = font;
        arc.Core.scene.addStyle(arc.scene.ui.Dialog.DialogStyle.class, dialogStyle);
        arc.scene.ui.TextButton.TextButtonStyle textButtonStyle = new arc.scene.ui.TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        arc.Core.scene.addStyle(arc.scene.ui.TextButton.TextButtonStyle.class, textButtonStyle);
        arc.Core.scene.addStyle(arc.scene.ui.Label.LabelStyle.class, new arc.scene.ui.Label.LabelStyle(font, null));
    }

    @Test
    void masterButtonLongClick_hidesPopupAndOpensSettingsDialog() throws Exception {
        setupTestScene();
        boolean[] settingsOpened = { false };
        AutoplayFeature feature = new AutoplayFeature() {
            @Override
            public @Nullable arc.func.Prov<solim.overlay.SolimDialog> getSettingDialog() {
                return () -> new solim.overlay.SolimDialog("") {
                    @Override
                    public solim.overlay.SolimDialog show() {
                        settingsOpened[0] = true;
                        return this;
                    }
                };
            }
        };

        try {
            AutoplayPopup.show(feature, null);
            assertTrue(AutoplayPopup.isShowing());

            solim.core.Component controls = AutoplayHudView.buildControls(feature, null);
            java.util.List<arc.scene.ui.Button> buttons = findAllButtons(controls.element());
            assertFalse(buttons.isEmpty());

            arc.scene.ui.Button masterBtn = buttons.get(0);
            fireLongClick(masterBtn);

            assertFalse(AutoplayPopup.isShowing(), "Popup must be hidden after long-click");
            assertTrue(settingsOpened[0], "Settings dialog must be shown after long-click");
        } finally {
            AutoplayPopup.hide();
            feature.disable();
            flushEffects();
        }
    }

    @Test
    void taskButtonLongClick_hidesPopupAndOpensSettingsDialog() throws Exception {
        setupTestScene();
        boolean[] settingsOpened = { false };
        AutoplayFeature feature = new AutoplayFeature() {
            @Override
            public @Nullable arc.func.Prov<solim.overlay.SolimDialog> getSettingDialog() {
                return () -> new solim.overlay.SolimDialog("") {
                    @Override
                    public solim.overlay.SolimDialog show() {
                        settingsOpened[0] = true;
                        return this;
                    }
                };
            }
        };

        try {
            AutoplayPopup.show(feature, null);
            assertTrue(AutoplayPopup.isShowing());

            solim.core.Component controls = AutoplayHudView.buildControls(feature, null);
            flushEffects();
            java.util.List<arc.scene.ui.Button> buttons = findAllButtons(controls.element());
            assertTrue(buttons.size() > 1, "Must have master button and at least one task button");

            arc.scene.ui.Button taskBtn = buttons.get(1);
            fireLongClick(taskBtn);

            assertFalse(AutoplayPopup.isShowing(), "Popup must be hidden after long-click");
            assertTrue(settingsOpened[0], "Settings dialog must be shown after long-click");
        } finally {
            AutoplayPopup.hide();
            feature.disable();
            flushEffects();
        }
    }
}
