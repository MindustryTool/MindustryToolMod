package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.test.MindustryTestEnv;

class QuickAccessVisibilityTest extends MindustryTestEnv {

    static class TestFeature extends Feature {
        TestFeature(String id, boolean quickAccessByDefault) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book)
                    .quickAccessByDefault(quickAccessByDefault)
                    .build());
        }
    }

    @BeforeAll
    static void initCore() {
        Icon.book = new TextureRegionDrawable();
    }

    @Test
    void defaultVisibility_followsMetadata() {
        TestFeature onFeature = new TestFeature("feat-on", true);
        TestFeature offFeature = new TestFeature("feat-off", false);
        FeatureManager.register(onFeature, offFeature);

        QuickAccessFeature qa = new QuickAccessFeature();

        assertTrue(qa.isFeatureVisible("feat-on"));
        assertFalse(qa.isFeatureVisible("feat-off"));
    }

    @Test
    void toggleDefaultOnFeature_updatesHiddenConfig() {
        TestFeature onFeature = new TestFeature("feat-on", true);
        FeatureManager.register(onFeature);

        QuickAccessFeature qa = new QuickAccessFeature();
        assertTrue(qa.isFeatureVisible("feat-on"));

        qa.setFeatureVisible("feat-on", false);
        assertFalse(qa.isFeatureVisible("feat-on"));
        assertTrue(qa.hiddenFeaturesConfig.get().contains("feat-on"));
        assertFalse(qa.shownFeaturesConfig.get().contains("feat-on"));

        qa.setFeatureVisible("feat-on", true);
        assertTrue(qa.isFeatureVisible("feat-on"));
        assertFalse(qa.hiddenFeaturesConfig.get().contains("feat-on"));
    }

    @Test
    void toggleDefaultOffFeature_updatesShownConfig() {
        TestFeature offFeature = new TestFeature("feat-off", false);
        FeatureManager.register(offFeature);

        QuickAccessFeature qa = new QuickAccessFeature();
        assertFalse(qa.isFeatureVisible("feat-off"));

        qa.setFeatureVisible("feat-off", true);
        assertTrue(qa.isFeatureVisible("feat-off"));
        assertTrue(qa.shownFeaturesConfig.get().contains("feat-off"));
        assertFalse(qa.hiddenFeaturesConfig.get().contains("feat-off"));

        qa.setFeatureVisible("feat-off", false);
        assertFalse(qa.isFeatureVisible("feat-off"));
        assertFalse(qa.shownFeaturesConfig.get().contains("feat-off"));
    }

    @Test
    void existingHiddenSetting_overridesDefaultOn() {
        Core.settings.put("mindustrytool.features.quick-access.hidden", "feat-on");

        TestFeature onFeature = new TestFeature("feat-on", true);
        FeatureManager.register(onFeature);

        QuickAccessFeature qa = new QuickAccessFeature();
        assertFalse(qa.isFeatureVisible("feat-on"));
    }
}
