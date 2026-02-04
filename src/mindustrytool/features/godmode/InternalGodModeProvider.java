package mindustrytool.features.godmode;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.ui.Styles;
import mindustry.world.Tile;

public class InternalGodModeProvider implements GodModeProvider {

    @Override
    public boolean isAvailable() {
        return !Vars.net.active() || Vars.net.server();
    }

    @Override
    public void build(Table table) {
        table.defaults().size(40, 40).pad(2);

        table.button(Icon.players, Styles.cleari, () -> {
            GodModeDialogs.showTeamDialog((player, team) -> {
                if (player != null)
                    player.team(team);
            });
        }).tooltip("Change Team");

        table.button(Icon.box, Styles.cleari, () -> {
            GodModeDialogs.showItemDialog((item, amount, team) -> {
                if (team != null && team.core() != null) {
                    team.core().items.add(item, amount);
                }
            });
        }).tooltip("Items");

        table.button(Icon.units, Styles.cleari, () -> {
            GodModeDialogs.showUnitDialog(
                    (unit, amount, team, x, y) -> {
                        for (int i = 0; i < amount; i++) {
                            unit.spawn(team, x + Mathf.range(unit.hitSize * 2),
                                    y + Mathf.range(unit.hitSize * 2));
                        }
                    },
                    (unit, team) -> {
                        Groups.unit.each(u -> u.type == unit && u.team == team, Unit::kill);
                    });
        }).tooltip("Units");

        table.button(Icon.effect, Styles.cleari, () -> {
            GodModeDialogs.showEffectDialog(
                    (effect, duration) -> {
                        if (Vars.player != null && Vars.player.unit() != null) {
                            Vars.player.unit().apply(effect, duration);
                        }
                    },
                    (effect) -> {
                        if (Vars.player != null && Vars.player.unit() != null) {
                            Vars.player.unit().unapply(effect);
                        }
                    });
        }).tooltip("Effects");

        table.button(Icon.hammer, Styles.cleari, () -> {
            GodModeDialogs.showCoreDialog((coreBlock, team, x, y) -> {
                Tile tile = Vars.world.tileWorld(x, y);
                if (tile != null) {
                    tile.setNet(coreBlock, team, 0);
                }
            });
        }).tooltip("Place Core");
    }
}
