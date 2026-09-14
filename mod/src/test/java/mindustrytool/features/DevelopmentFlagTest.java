package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.godmode.GodModeFeature;
import mindustrytool.features.healthbar.HealthBarFeature;
import mindustrytool.features.music.MusicFeature;
import mindustrytool.features.pathfinding.PathfindingFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.prettychat.PrettyChatFeature;
import mindustrytool.features.progressdisplay.ProgressDisplayFeature;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import mindustrytool.features.savesync.SaveSyncFeature;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.features.smartupgrade.SmartUpgradeFeature;
import mindustrytool.features.timecontrol.TimeControlFeature;
import mindustrytool.features.togglerendering.ToggleRenderingFeature;
import mindustrytool.features.wavepreview.WavePreviewFeature;
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
    void allStubsAreMarkedDevelopment() {
        Feature[] stubs = {
                new PlayerConnectFeature(),
                new HealthBarFeature(),
                new PathfindingFeature(),
                new RangeDisplayFeature(),
                new PrettyChatFeature(),
                new AutoplayFeature(),
                new WavePreviewFeature(),
                new SaveSyncFeature(),
                new GodModeFeature(),
                new SmartDrillFeature(),
                new SmartUpgradeFeature(),
                new MusicFeature(),
                new ProgressDisplayFeature(),
                new ToggleRenderingFeature(),
                new TimeControlFeature()
        };

        String[] ids = {
                "player-connect",
                "health-bar",
                "pathfinding",
                "range-display",
                "pretty-chat",
                "autoplay",
                "wave-preview",
                "save-sync",
                "item-visualizer",
                "god-mode",
                "smart-drill",
                "smart-upgrade",
                "music",
                "progress-display",
                "toggle-rendering",
                "time-control"
        };

        assertEquals(ids.length, stubs.length);

        for (int i = 0; i < stubs.length; i++) {
            assertTrue(stubs[i].getMetadata().isDevelopment(), ids[i] + " must be in development");
            assertEquals(ids[i], stubs[i].getMetadata().getId());
            assertNotNull(stubs[i].getMetadata().getIcon(), ids[i] + " must declare an icon");
        }
    }

    @Test
    void enableIsNoOpOnDevelopment() {
        AutoplayFeature feature = new AutoplayFeature();

        feature.enable();

        assertFalse(feature.isEnabled());
        assertFalse(Core.settings.getBool(feature.getSettingKey(), false));
    }

    @Test
    void setEnabledTrueIsNoOpOnDevelopment() {
        AutoplayFeature feature = new AutoplayFeature();

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
