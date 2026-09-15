package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustrytool.features.bridgevisualizer.BridgeVisualizerFeature;
import mindustrytool.features.chat.ChatFeature;
import mindustrytool.features.godmode.GodModeFeature;
import mindustrytool.features.music.MusicFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.prettychat.PrettyChatFeature;
import mindustrytool.features.progressdisplay.ProgressDisplayFeature;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.features.savesync.SaveSyncFeature;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.features.smartupgrade.SmartUpgradeFeature;
import mindustrytool.features.wavepreview.WavePreviewFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureIconTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        Icon.book = new TextureRegionDrawable();
    }

    @Test
    void allUpdatedFeaturesHaveValidIcons() {
        Feature[] features = new Feature[] {
            new SmartDrillFeature(),
            new PlayerConnectFeature(),
            new ChatFeature(),
            new MusicFeature(),
            new ProgressDisplayFeature(),
            new WavePreviewFeature(),
            new BridgeVisualizerFeature(),
            new SaveSyncFeature(),
            new GodModeFeature(),
            new PrettyChatFeature(),
            new SmartUpgradeFeature(),
            new QuickAccessFeature()
        };

        for (Feature feature : features) {
            FeatureMetadata meta = feature.getMetadata();
            assertNotNull(meta, "Metadata should not be null for " + feature.getClass().getSimpleName());
            assertNotNull(meta.getIcon(), "Icon should not be null for " + feature.getClass().getSimpleName());
        }
    }
}
