package mindustrytool.features.godmode;

import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class JSGodModeProvider implements GodModeProvider {

    private void js(String code, Object... objects) {
        Call.sendChatMessage("/js " + Strings.format(code, objects));
    }

    @Override
    public boolean isAvailable() {
        var isAdminInServer = Vars.net.client() &&
                Vars.player != null &&
                Vars.player.admin;

        return isAdminInServer;
    }

    @Override
    public void build(Table table) {
        table.defaults().size(40, 40).pad(2);

        table.button(Icon.players, Styles.cleari, () -> {
            GodModeDialogs.showTeamDialog((player, team) -> {
                js("Groups.player.find(p => p.name == \"@\").team(Team.get(@))", player.name, team.id);
            });
        }).tooltip("Change Team");

        table.button(Icon.box, Styles.cleari, () -> {
            GodModeDialogs.showItemDialog((item, amount, team) -> {
                js("var team = Team.get(@); if(team.core() != null) team.core().items.add(Vars.content.item(@), @)",
                        team.id, item.id, amount);
            });
        }).tooltip("Items");

        table.button(Icon.units, Styles.cleari, () -> {
            GodModeDialogs.showUnitDialog(
                    (unit, amount, team, x, y) -> {
                        js("for(var i=0;i<@;i++){ Vars.content.unit(@).spawn(Team.get(@), @, @); }", amount, unit.id,
                                team.id, x, y);
                    },
                    (unit, team) -> {
                        js("Groups.unit.each(u => u.type.id == @ && u.team.id == @, u => u.kill())", unit.id, team.id);
                    });
        }).tooltip("Units");

        table.button(Icon.effect, Styles.cleari, () -> {
            GodModeDialogs.showEffectDialog(
                    (effect, duration) -> {
                        js("if(Vars.player.unit() != null) Vars.player.unit().apply(Vars.content.statusEffect(\"@\"), @)",
                                effect.name, duration);
                    },
                    (effect) -> {
                        js("if(Vars.player.unit() != null) Vars.player.unit().unapply(Vars.content.statusEffect(\"@\"))",
                                effect.name);
                    });
        }).tooltip("Effects");

        table.button(Icon.hammer, Styles.cleari, () -> {
            GodModeDialogs.showCoreDialog((coreBlock, team, x, y) -> {
                js("Vars.world.tileWorld(@,@).setNet(Vars.content.block(@), Team.get(@), 0)", x, y, coreBlock.id,
                        team.id);
            });
        }).tooltip("Place Core");
    }
}
