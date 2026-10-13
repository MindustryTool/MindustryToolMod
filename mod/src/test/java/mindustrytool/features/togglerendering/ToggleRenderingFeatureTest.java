package mindustrytool.features.togglerendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.UnitEntity;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToggleRenderingFeatureTest extends MindustryTestEnv {

    private ToggleRenderingFeature feature;

    @BeforeEach
    void setUp() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        Icon.eye = new TextureRegionDrawable();
        Icon.refresh = new TextureRegionDrawable();
        feature = new ToggleRenderingFeature();
    }

    @AfterEach
    void tearDown() {
        Vars.player = null;
    }

    @Test
    void configDefaultsAndReset() {
        assertTrue(feature.drawUnitsAlliesConfig.get());
        assertTrue(feature.drawUnitsEnemiesConfig.get());
        assertTrue(feature.drawBlocksConfig.get());

        feature.drawUnitsAlliesConfig.set(false);
        feature.drawUnitsEnemiesConfig.set(false);
        feature.drawBlocksConfig.set(false);

        assertFalse(feature.drawUnitsAlliesConfig.get());
        assertFalse(feature.drawUnitsEnemiesConfig.get());
        assertFalse(feature.drawBlocksConfig.get());

        feature.resetToDefaults();

        assertTrue(feature.drawUnitsAlliesConfig.get());
        assertTrue(feature.drawUnitsEnemiesConfig.get());
        assertTrue(feature.drawBlocksConfig.get());
    }

    @Test
    void shouldHideUnitLogic() {
        Vars.player = new Player() {
            @Override
            public Team team() {
                return Team.sharded;
            }
        };

        UnitEntity ally = new UnitEntity() {
        };
        ally.team = Team.sharded;

        UnitEntity enemy = new UnitEntity() {
        };
        enemy.team = Team.crux;

        assertFalse(feature.shouldHideUnit(ally));
        assertFalse(feature.shouldHideUnit(enemy));

        feature.drawUnitsAlliesConfig.set(false);
        feature.drawUnitsEnemiesConfig.set(true);
        assertTrue(feature.shouldHideUnit(ally));
        assertFalse(feature.shouldHideUnit(enemy));

        feature.drawUnitsAlliesConfig.set(true);
        feature.drawUnitsEnemiesConfig.set(false);
        assertFalse(feature.shouldHideUnit(ally));
        assertTrue(feature.shouldHideUnit(enemy));

        feature.drawUnitsAlliesConfig.set(false);
        feature.drawUnitsEnemiesConfig.set(false);
        assertTrue(feature.shouldHideUnit(ally));
        assertTrue(feature.shouldHideUnit(enemy));
    }

    @Test
    void onDisableCleansUp() {
        feature.onDisable();
        assertEquals(0, feature.getHiddenUnitsCount());
    }
}
