package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.TextureRegionDrawable;
import java.util.concurrent.atomic.AtomicBoolean;
import mindustry.gen.Icon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureKeybindTest {

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
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Icon.book = new TextureRegionDrawable();
    }

    @BeforeEach
    void setUp() {
        Core.settings = new Settings();
        Core.settings.clear();
        FeatureManager.clear();
    }

    @AfterEach
    void tearDown() {
        FeatureManager.clear();
        Core.settings.clear();
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
}
