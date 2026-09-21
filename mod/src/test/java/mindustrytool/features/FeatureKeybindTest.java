package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustrytool.features.bridgevisualizer.BridgeVisualizerFeature;
import mindustrytool.features.healthbar.HealthBarFeature;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import mindustrytool.features.screenshot.ScreenshotFeature;
import mindustrytool.features.wavepreview.WavePreviewFeature;
import mindustrytool.test.MindustryTestEnv;

class FeatureKeybindTest extends MindustryTestEnv {

    static class TestFeature extends Feature {
        TestFeature(String id) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book)
                    .enabledByDefault(false)
                    .build());
        }

        KeyBind toggle(String name) {
            return bindToggle(name, KeyCode.unset);
        }

        KeyBind action(String name, Runnable runnable, boolean requireEnabled) {
            return bindAction(name, KeyCode.unset, runnable, requireEnabled);
        }

        KeyBind dialog(String name, Runnable show) {
            return bindDialog(name, KeyCode.unset, show);
        }
    }

    @BeforeAll
    static void initCore() {
        Icon.book = new TextureRegionDrawable();
    }

    @Test
    void descriptor_holdsBindActionAndPrecondition() {
        KeyBind bind = KeyBind.add("test.descriptor.holds", KeyCode.unset, "MindustryTool");
        AtomicBoolean ran = new AtomicBoolean(false);
        FeatureKeybind keybind = new FeatureKeybind(bind, () -> ran.set(true), true);

        assertEquals(bind, keybind.getBind());
        assertTrue(keybind.isRequireEnabled());

        keybind.getAction().run();
        assertTrue(ran.get());
    }

    @Test
    void descriptor_rejectsNullBindOrAction() {
        KeyBind bind = KeyBind.add("test.descriptor.null", KeyCode.unset, "MindustryTool");
        assertThrows(IllegalArgumentException.class, () -> new FeatureKeybind(null, () -> {
        }, false));
        assertThrows(IllegalArgumentException.class, () -> new FeatureKeybind(bind, null, false));
    }

    @Test
    void bindToggle_registersWithNoPreconditionAndTogglesEnabled() {
        TestFeature feature = new TestFeature("test-toggle");
        assertFalse(feature.isEnabled());

        feature.toggle("test.bind.toggle");

        assertEquals(1, feature.getKeybinds().size);
        FeatureKeybind keybind = feature.getKeybinds().first();
        assertFalse(keybind.isRequireEnabled());

        keybind.getAction().run();
        assertTrue(feature.isEnabled());

        keybind.getAction().run();
        assertFalse(feature.isEnabled());
    }

    @Test
    void bindAction_maintainsMultipleKeybindsIndependently() {
        TestFeature feature = new TestFeature("test-multi");
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);

        feature.action("test.bind.multi.first", () -> first.set(true), true);
        feature.action("test.bind.multi.second", () -> second.set(true), true);

        assertEquals(2, feature.getKeybinds().size);

        feature.getKeybinds().get(0).getAction().run();
        assertTrue(first.get());
        assertFalse(second.get());

        feature.getKeybinds().get(1).getAction().run();
        assertTrue(second.get());
    }

    @Test
    void bindDialog_registersOperationalKeybind() {
        TestFeature feature = new TestFeature("test-dialog");
        AtomicBoolean shown = new AtomicBoolean(false);

        feature.dialog("test.bind.dialog", () -> shown.set(true));

        assertEquals(1, feature.getKeybinds().size);
        FeatureKeybind keybind = feature.getKeybinds().first();
        assertTrue(keybind.isRequireEnabled());

        keybind.getAction().run();
        assertTrue(shown.get());
    }

    @Test
    void shouldTrigger_toggleRunsWhileDisabled() {
        TestFeature feature = new TestFeature("test-precondition-toggle");
        assertFalse(feature.isEnabled());
        feature.toggle("test.precondition.toggle");

        FeatureKeybind keybind = feature.getKeybinds().first();
        assertTrue(FeatureKeybindManager.shouldTrigger(feature, keybind, false, true));
    }

    @Test
    void shouldTrigger_operationalSuppressedWhileDisabled() {
        TestFeature feature = new TestFeature("test-precondition-operational");
        assertFalse(feature.isEnabled());
        feature.action("test.precondition.operational", () -> {
        }, true);

        FeatureKeybind keybind = feature.getKeybinds().first();
        assertFalse(FeatureKeybindManager.shouldTrigger(feature, keybind, false, true));
    }

    @Test
    void shouldTrigger_operationalRunsWhileEnabled() {
        TestFeature feature = new TestFeature("test-precondition-enabled");
        feature.setEnabled(true);
        assertTrue(feature.isEnabled());
        feature.action("test.precondition.enabled", () -> {
        }, true);

        FeatureKeybind keybind = feature.getKeybinds().first();
        assertTrue(FeatureKeybindManager.shouldTrigger(feature, keybind, false, true));
    }

    @Test
    void shouldTrigger_suppressedWhenFieldFocused() {
        TestFeature feature = new TestFeature("test-focus");
        feature.setEnabled(true);
        feature.action("test.focus.suppressed", () -> {
        }, true);

        FeatureKeybind keybind = feature.getKeybinds().first();
        assertFalse(FeatureKeybindManager.shouldTrigger(feature, keybind, true, true));
    }

    @Test
    void shouldTrigger_suppressedWhenKeyNotReleased() {
        TestFeature feature = new TestFeature("test-release");
        feature.setEnabled(true);
        feature.toggle("test.release.suppressed");

        FeatureKeybind keybind = feature.getKeybinds().first();
        assertFalse(FeatureKeybindManager.shouldTrigger(feature, keybind, false, false));
    }

    @Test
    void screenshotFeature_registersCaptureAndSettingsKeybinds() {
        ScreenshotFeature feature = new ScreenshotFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind captureBind = feature.getKeybinds().find(k -> k.getBind().name.equals("screenshotCapture"));
        assertNotNull(captureBind);
        assertFalse(captureBind.isRequireEnabled());
        assertTrue(FeatureKeybindManager.shouldTrigger(feature, captureBind, false, true));

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("screenshotSettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void rangeDisplayFeature_registersToggleAndSettingsKeybinds() {
        RangeDisplayFeature feature = new RangeDisplayFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind toggleBind = feature.getKeybinds().find(k -> k.getBind().name.equals("rangeDisplay"));
        assertNotNull(toggleBind);
        assertFalse(toggleBind.isRequireEnabled());

        boolean initial = feature.isEnabled();
        toggleBind.getAction().run();
        assertEquals(!initial, feature.isEnabled());

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("rangeDisplaySettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void healthBarFeature_registersToggleAndSettingsKeybinds() {
        HealthBarFeature feature = new HealthBarFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind toggleBind = feature.getKeybinds().find(k -> k.getBind().name.equals("healthBar"));
        assertNotNull(toggleBind);
        assertFalse(toggleBind.isRequireEnabled());

        boolean initial = feature.isEnabled();
        toggleBind.getAction().run();
        assertEquals(!initial, feature.isEnabled());

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("healthBarSettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void bridgeVisualizerFeature_registersToggleAndSettingsKeybinds() {
        BridgeVisualizerFeature feature = new BridgeVisualizerFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind toggleBind = feature.getKeybinds().find(k -> k.getBind().name.equals("bridgeVisualizer"));
        assertNotNull(toggleBind);
        assertFalse(toggleBind.isRequireEnabled());

        boolean initial = feature.isEnabled();
        toggleBind.getAction().run();
        assertEquals(!initial, feature.isEnabled());

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("bridgeVisualizerSettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void wavePreviewFeature_registersToggleAndSettingsKeybinds() {
        WavePreviewFeature feature = new WavePreviewFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind toggleBind = feature.getKeybinds().find(k -> k.getBind().name.equals("wavePreview"));
        assertNotNull(toggleBind);
        assertFalse(toggleBind.isRequireEnabled());

        boolean initial = feature.isEnabled();
        toggleBind.getAction().run();
        assertEquals(!initial, feature.isEnabled());

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("wavePreviewSettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }
}
