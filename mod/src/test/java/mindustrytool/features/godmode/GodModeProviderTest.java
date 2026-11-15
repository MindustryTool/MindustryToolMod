package mindustrytool.features.godmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.mock.MockApplication;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import solim.test.ArcTestEnv;

class GodModeProviderTest extends ArcTestEnv {

    private InternalGodModeProvider internalProvider;
    private JSGodModeProvider jsProvider;

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Vars.content = new ContentLoader();
        internalProvider = new InternalGodModeProvider();
        jsProvider = new JSGodModeProvider();
    }

    @Test
    void internalProviderRejectsNullParameters() {
        Vars.world = null;
        assertFalse(internalProvider.placeBlock(null, Team.sharded, 0f, 0f, 0));
        assertFalse(internalProvider.placeBlock(new Block("test-block"), null, 0f, 0f, 0));
        assertFalse(internalProvider.placeCore(null, Team.sharded, 0f, 0f));
    }

    @Test
    void internalProviderRejectsNullWorldOrTile() {
        Vars.world = null;
        Block block = new Block("test-block");
        assertFalse(internalProvider.placeBlock(block, Team.sharded, 100f, 200f, 2));
    }

    @Test
    void jsProviderRejectsNullParameters() {
        assertFalse(jsProvider.placeBlock(null, Team.sharded, 0f, 0f, 0));
        assertFalse(jsProvider.placeBlock(new Block("test-block"), null, 0f, 0f, 0));
        assertFalse(jsProvider.placeCore(null, Team.sharded, 0f, 0f));
    }

    @Test
    void defaultPlaceCoreDelegatesWithZeroRotation() {
        int[] capturedRotation = new int[]{-1};
        Block block = new Block("test-core");

        GodModeProvider mockProvider = new GodModeProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void changeTeam(Player player, Team team) {
            }

            @Override
            public void addItems(Item item, int amount, Team team) {
            }

            @Override
            public void spawnUnits(UnitType unit, int amount, Team team, float x, float y) {
            }

            @Override
            public void killUnits(UnitType unit, Team team) {
            }

            @Override
            public void applyEffect(StatusEffect effect, float duration) {
            }

            @Override
            public void clearEffect(StatusEffect effect) {
            }

            @Override
            public boolean placeBlock(Block b, Team team, float x, float y, int rotation) {
                capturedRotation[0] = rotation;
                return true;
            }

            @Override
            public void setFog(boolean enabled) {
            }
        };

        boolean result = mockProvider.placeCore(block, Team.sharded, 10f, 20f);
        assertTrue(result);
        assertEquals(0, capturedRotation[0]);
    }
}
