package mindustrytool.features.godmode;

import arc.math.Mathf;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

public class InternalGodModeProvider implements GodModeProvider {

    @Override
    public boolean isAvailable() {
        return !Vars.net.active() || Vars.net.server();
    }

    @Override
    public void changeTeam(@Nullable Player player, @Nullable Team team) {
        if (player != null && team != null) {
            player.team(team);
        }
    }

    @Override
    public void addItems(@Nullable Item item, int amount, @Nullable Team team) {
        if (item != null && team != null && team.core() != null) {
            team.core().items.add(item, amount);
        }
    }

    @Override
    public void spawnUnits(@Nullable UnitType unit, int amount, @Nullable Team team, float x, float y) {
        if (unit == null || team == null || amount <= 0) {
            return;
        }
        for (int i = 0; i < amount; i++) {
            float spawnX = x + Mathf.range(unit.hitSize * 2f);
            float spawnY = y + Mathf.range(unit.hitSize * 2f);
            unit.spawn(team, spawnX, spawnY);
        }
    }

    @Override
    public void killUnits(@Nullable UnitType unit, @Nullable Team team) {
        if (unit == null || team == null) {
            return;
        }
        Groups.unit.each(u -> u.type == unit && u.team == team, Unit::kill);
    }

    @Override
    public void applyEffect(@Nullable StatusEffect effect, float duration) {
        if (effect != null && Vars.player != null && Vars.player.unit() != null) {
            Vars.player.unit().apply(effect, duration);
        }
    }

    @Override
    public void clearEffect(@Nullable StatusEffect effect) {
        if (effect != null && Vars.player != null && Vars.player.unit() != null) {
            Vars.player.unit().unapply(effect);
        }
    }

    @Override
    public boolean placeCore(@Nullable Block coreBlock, @Nullable Team team, float x, float y) {
        if (coreBlock == null || team == null || !(coreBlock instanceof CoreBlock)) {
            return false;
        }
        Tile tile = Vars.world.tileWorld(x, y);
        if (tile == null) {
            return false;
        }
        int offset = -(coreBlock.size - 1) / 2;
        for (int dx = 0; dx < coreBlock.size; dx++) {
            for (int dy = 0; dy < coreBlock.size; dy++) {
                Tile t = Vars.world.tile(tile.x + offset + dx, tile.y + offset + dy);
                if (t == null || t.solid()) {
                    return false;
                }
            }
        }
        tile.setNet(coreBlock, team, 0);
        return true;
    }

    @Override
    public void setFog(boolean enabled) {
        if (Vars.state != null && Vars.state.rules != null) {
            Vars.state.rules.fog = enabled;
            if (Vars.net.server()) {
                Call.setRules(Vars.state.rules);
            }
        }
    }
}
