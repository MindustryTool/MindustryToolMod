package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DevelopmentFlagTest {

    static class DevDouble extends Feature {
        boolean onEnableCalled;

        DevDouble() {
            super(FeatureMetadata.builder()
                    .id("test-dev-double")
                    .icon(Icon.book)
                    .development(true)
                    .build());
        }

        @Override
        public void onEnable() {
            onEnableCalled = true;
        }
    }

    static class ReadyDouble extends Feature {
        ReadyDouble() {
            super(FeatureMetadata.builder()
                    .id("test-ready-double")
                    .icon(Icon.book)
                    .enabledByDefault(false)
                    .build());
        }
    }

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        seedIcons();
    }

    private static void seedIcons() {
        Icon.book = new TextureRegionDrawable();
        Icon.planet = new TextureRegionDrawable();
        Icon.chat = new TextureRegionDrawable();
        Icon.units = new TextureRegionDrawable();
        Icon.save = new TextureRegionDrawable();
        Icon.distribution = new TextureRegionDrawable();
        Icon.defense = new TextureRegionDrawable();
        Icon.filter = new TextureRegionDrawable();
        Icon.up = new TextureRegionDrawable();
        Icon.play = new TextureRegionDrawable();
        Icon.chartBar = new TextureRegionDrawable();
        Icon.eye = new TextureRegionDrawable();
    }

    @Test
    void metadataDefaultsToNotDevelopment() {
        FeatureMetadata metadata = FeatureMetadata.builder()
                .id("test-default")
                .icon(Icon.book)
                .build();

        assertFalse(metadata.isDevelopment());
    }

    @Test
    void enableIsNoOpOnDevelopment() {
        DevDouble feature = new DevDouble();

        feature.enable();

        assertFalse(feature.isEnabled());
        assertFalse(Core.settings.getBool(feature.getSettingKey(), false));
    }

    @Test
    void setEnabledTrueIsNoOpOnDevelopment() {
        DevDouble feature = new DevDouble();

        feature.setEnabled(true);

        assertFalse(feature.isEnabled());
        assertFalse(Core.settings.getBool(feature.getSettingKey(), false));
    }

    @Test
    void normalFeatureStillEnables() {
        ReadyDouble feature = new ReadyDouble();
        assertFalse(feature.isEnabled());

        feature.enable();

        assertTrue(feature.isEnabled());
        assertTrue(Core.settings.getBool(feature.getSettingKey(), false));
    }

    @Test
    void initSkipsDevelopmentFeatures() {
        DevDouble dev = new DevDouble();
        ReadyDouble ready = new ReadyDouble();
        FeatureManager.register(dev, ready);
        try {
            ready.enable();

            FeatureManager.init();

            assertFalse(dev.onEnableCalled);
        } finally {
            FeatureManager.unregister(dev, ready);
        }
    }
}
