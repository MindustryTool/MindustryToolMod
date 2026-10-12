package mindustrytool.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureKeybind;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.test.MindustryTestEnv;
import solim.core.Component;

class SettingKeybindComponentTest extends MindustryTestEnv {

    static class DummyFeature extends Feature {
        DummyFeature() {
            super(FeatureMetadata.builder()
                    .id("dummy-keybind-test")
                    .icon(Icon.book)
                    .build());
        }

        KeyBind addKey(String name, KeyCode code) {
            return bindAction(name, code, () -> {});
        }
    }

    @BeforeAll
    static void initDrawables() {
        Icon.book = new TextureRegionDrawable();
        Icon.settings = new TextureRegionDrawable();
        Icon.refresh = new TextureRegionDrawable();
    }

    @Test
    void forFeature_returnsNullWhenNoKeybinds() {
        DummyFeature feature = new DummyFeature();
        assertNull(SettingKeybindCard.forFeature(feature));
    }

    @Test
    void forFeature_returnsComponentWhenKeybindsExist() {
        mindustry.Vars.mobile = false;
        DummyFeature feature = new DummyFeature();
        feature.addKey("testKey1", KeyCode.space);

        Component card = SettingKeybindCard.forFeature(feature);
        assertNotNull(card);
        assertNotNull(card.element());
    }

    @Test
    void forFeature_returnsNullOnMobile() {
        mindustry.Vars.mobile = true;
        try {
            DummyFeature feature = new DummyFeature();
            feature.addKey("testKey1", KeyCode.space);
            assertNull(SettingKeybindCard.forFeature(feature));
            assertNull(SettingKeybindCard.of("Test", feature.getKeybinds()));
        } finally {
            mindustry.Vars.mobile = false;
        }
    }

    @Test
    void settingKeybindRow_rendersAndHandlesInputLifecycle() {
        KeyBind bind = KeyBind.add("test.row.bind", KeyCode.g, "MindustryTool");
        FeatureKeybind featureKeybind = new FeatureKeybind(bind, () -> {}, false);
        SettingKeybindRow row = new SettingKeybindRow(featureKeybind);

        assertNotNull(row.element());
        assertEquals(KeyCode.g, featureKeybind.getKeyCode());
        assertTrue(featureKeybind.isDefault());

        // Start listening
        row.startListening();

        // Reject mouseLeft (must remain listening, no change)
        row.handleInput(KeyCode.mouseLeft);
        assertEquals(KeyCode.g, featureKeybind.getKeyCode());
        assertTrue(SettingKeybindRow.isAnyListening());

        // Cancel with Escape
        row.handleInput(KeyCode.escape);
        assertEquals(KeyCode.g, featureKeybind.getKeyCode());
        assertFalse(SettingKeybindRow.isAnyListening());

        // Rebind to H
        row.startListening();
        row.handleInput(KeyCode.h);
        flushEffects();
        assertEquals(KeyCode.h, featureKeybind.getKeyCode());
        assertFalse(featureKeybind.isDefault());

        // Clear with Backspace
        row.startListening();
        row.handleInput(KeyCode.backspace);
        flushEffects();
        assertEquals(KeyCode.unset, featureKeybind.getKeyCode());

        // Reset to default
        featureKeybind.resetToDefault();
        assertEquals(KeyCode.g, featureKeybind.getKeyCode());
        assertTrue(featureKeybind.isDefault());

        row.dispose();
    }

    @Test
    void of_buildsCardForCustomCollection() {
        KeyBind bind1 = KeyBind.add("test.col.1", KeyCode.f, "MindustryTool");
        KeyBind bind2 = KeyBind.add("test.col.2", KeyCode.h, "MindustryTool");
        Seq<FeatureKeybind> list = new Seq<>();
        list.add(new FeatureKeybind(bind1, () -> {}, false));
        list.add(new FeatureKeybind(bind2, () -> {}, false));

        Component card = SettingKeybindCard.of("Test Binds", list);
        assertNotNull(card);
        assertNotNull(card.element());
    }

    @Test
    void card_rendersExactNumberOfRowsWithoutDuplication() {
        DummyFeature feature = new DummyFeature();
        feature.addKey("singleKey", KeyCode.j);

        Component card = SettingKeybindCard.forFeature(feature);
        assertNotNull(card);
        arc.scene.Element elem = card.element();
        assertNotNull(elem);

        // Find all SettingKeybindRow root tables by looking for children containing the bind name
        arc.struct.Seq<arc.scene.Element> children = new arc.struct.Seq<>();
        findMatchingElements(elem, e -> e instanceof arc.scene.ui.Label && "singleKey".equals(((arc.scene.ui.Label) e).getText().toString()), children);
        assertEquals(1, children.size, "Should render exactly 1 row for 1 keybind, no duplicates");
    }

    private void findMatchingElements(arc.scene.Element root, arc.func.Boolf<arc.scene.Element> predicate, arc.struct.Seq<arc.scene.Element> out) {
        if (predicate.get(root)) {
            out.add(root);
        }
        if (root instanceof arc.scene.Group) {
            for (arc.scene.Element child : ((arc.scene.Group) root).getChildren()) {
                findMatchingElements(child, predicate, out);
            }
        }
    }

    @Test
    void isAnyListening_tracksListeningState() {
        KeyBind bind = KeyBind.add("test.listen.track", KeyCode.k, "MindustryTool");
        FeatureKeybind featureKeybind = new FeatureKeybind(bind, () -> {}, false);
        SettingKeybindRow row = new SettingKeybindRow(featureKeybind);

        assertFalse(SettingKeybindRow.isAnyListening());
        row.startListening();
        assertTrue(SettingKeybindRow.isAnyListening());

        row.cancelListening();
        assertFalse(SettingKeybindRow.isAnyListening());

        row.dispose();
    }
}
