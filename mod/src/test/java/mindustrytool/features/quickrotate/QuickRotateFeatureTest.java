package mindustrytool.features.quickrotate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuickRotateFeatureTest extends MindustryTestEnv {

    private QuickRotateFeature feature;
    private Player player;
    private GameState prevState;
    private Player prevPlayer;

    @BeforeEach
    void setUp() {
        prevState = Vars.state;
        prevPlayer = Vars.player;

        Vars.content = new ContentLoader();
        Icon.refresh = new TextureRegionDrawable();

        Vars.state = new GameState();
        player = Player.create();
        player.team(Team.sharded);
        Vars.player = player;

        feature = new QuickRotateFeature();
    }

    @AfterEach
    void tearDown() {
        Vars.state = prevState;
        Vars.player = prevPlayer;
    }

    private Building createBuilding(Block block, Team team) {
        Building building = Building.create();
        building.block = block;
        building.team = team;
        return building;
    }

    @Test
    void shouldAcceptRotatableAndReplaceableBuilding() {
        Block conveyor = new Block("test-conveyor");
        conveyor.rotate = true;
        conveyor.quickRotate = true;
        conveyor.replaceable = true;

        Building building = createBuilding(conveyor, Team.sharded);

        assertTrue(feature.isEligible(building));
    }

    @Test
    void shouldRejectTurretWithQuickRotateFalse() {
        Block turret = new Block("test-turret");
        turret.rotate = true;
        turret.quickRotate = false;
        turret.replaceable = true;

        Building building = createBuilding(turret, Team.sharded);

        assertFalse(feature.isEligible(building));
    }

    @Test
    void shouldRejectNonReplaceableBuilding() {
        Block nonReplaceable = new Block("test-non-replaceable");
        nonReplaceable.rotate = true;
        nonReplaceable.quickRotate = true;
        nonReplaceable.replaceable = false;

        Building building = createBuilding(nonReplaceable, Team.sharded);

        assertFalse(feature.isEligible(building));
    }

    @Test
    void shouldRejectNonRotatableBuilding() {
        Block nonRotatable = new Block("test-non-rotatable");
        nonRotatable.rotate = false;
        nonRotatable.quickRotate = true;
        nonRotatable.replaceable = true;

        Building building = createBuilding(nonRotatable, Team.sharded);

        assertFalse(feature.isEligible(building));
    }

    @Test
    void shouldRejectNullOrMissingBlock() {
        assertFalse(feature.isEligible(null));

        Building building = Building.create();
        building.block = null;
        assertFalse(feature.isEligible(building));
    }

    @Test
    void shouldRejectEnemyTeamBuilding() {
        Block conveyor = new Block("test-conveyor");
        conveyor.rotate = true;
        conveyor.quickRotate = true;
        conveyor.replaceable = true;

        Building building = createBuilding(conveyor, Team.crux);

        assertFalse(feature.isEligible(building));
    }

    @Test
    void shouldRejectWhenPlayerIsNull() {
        Vars.player = null;

        Block conveyor = new Block("test-conveyor");
        conveyor.rotate = true;
        conveyor.quickRotate = true;
        conveyor.replaceable = true;

        Building building = createBuilding(conveyor, Team.sharded);

        assertFalse(feature.isEligible(building));
    }
}
