package mindustrytool.features.godmode;

import arc.util.Nullable;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class JSGodModeProvider implements GodModeProvider {

    private void js(String template, Object... args) {
        Call.sendChatMessage("/js " + Strings.format(template, args));
    }

    @Override
    public boolean isAvailable() {
        return Vars.net.client() && Vars.player != null && Vars.player.admin;
    }

    @Override
    public void changeTeam(@Nullable Player player, @Nullable Team team) {
        if (player != null && team != null) {
            js("var p = Groups.player.getByID(@); if(p != null) p.team(Team.get(@));", player.id, team.id);
        }
    }

    @Override
    public void addItems(@Nullable Item item, int amount, @Nullable Team team) {
        if (item != null && team != null) {
            js("var t = Team.get(@); if(t != null && t.core() != null) t.core().items.add(Vars.content.item(@), @);", team.id, item.id, amount);
        }
    }

    @Override
    public void spawnUnits(@Nullable UnitType unit, int amount, @Nullable Team team, float x, float y) {
        if (unit != null && team != null && amount > 0) {
            js("for(var i = 0; i < @; i++) { Vars.content.unit(@).spawn(Team.get(@), @, @); }", amount, unit.id, team.id, x, y);
        }
    }

    @Override
    public void killUnits(@Nullable UnitType unit, @Nullable Team team) {
        if (unit != null && team != null) {
            js("Groups.unit.each(u => u.type.id == @ && u.team.id == @, u => u.kill());", unit.id, team.id);
        }
    }

    @Override
    public void applyEffect(@Nullable StatusEffect effect, float duration) {
        if (effect != null) {
            js("if(Vars.player != null && Vars.player.unit() != null) Vars.player.unit().apply(Vars.content.statusEffect(\"@\"), @);", effect.name, duration);
        }
    }

    @Override
    public void clearEffect(@Nullable StatusEffect effect) {
        if (effect != null) {
            js("if(Vars.player != null && Vars.player.unit() != null) Vars.player.unit().unapply(Vars.content.statusEffect(\"@\"));", effect.name);
        }
    }

    @Override
    public boolean placeCore(@Nullable Block coreBlock, @Nullable Team team, float x, float y) {
        if (coreBlock != null && team != null) {
            js("Vars.world.tileWorld(@, @).setNet(Vars.content.block(@), Team.get(@), 0);", x, y, coreBlock.id, team.id);
            return true;
        }
        return false;
    }

    @Override
    public void setFog(boolean enabled) {
        js("Vars.state.rules.fog = @; Call.setRules(Vars.state.rules);", enabled);
    }
}
