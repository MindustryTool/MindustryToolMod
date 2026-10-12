package mindustrytool.features.logicvisualizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.style.TextureRegionDrawable;
import mindustry.core.ContentLoader;
import mindustry.gen.Icon;
import mindustry.Vars;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogicVisualizerFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        Vars.content = new ContentLoader();
        Icon.logic = new TextureRegionDrawable();
        Icon.refresh = new TextureRegionDrawable();
    }

    @Test
    void testSignalSynchronization() {
        LogicVisualizerFeature feature = new LogicVisualizerFeature();

        assertTrue(feature.isShowAllyUnits());
        assertFalse(feature.isShowEnemyUnits());

        feature.showAllyUnitsConfig.signal().set(false);
        assertFalse(feature.isShowAllyUnits());

        feature.showEnemyUnitsConfig.signal().set(true);
        assertTrue(feature.isShowEnemyUnits());
    }
}
