package mindustrytool.features.godmode;

import arc.scene.ui.layout.Table;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class JSGodModeProvider implements GodModeProvider {

    private void js(String code) {
        Call.sendChatMessage("/js " + code);
    }

    @Override
    public void build(Table table) {
        table.defaults().size(40, 40).pad(2);

        table.button(Icon.players, Styles.cleari, () -> {
            GodModeDialogs.showTeamDialog((player, team) -> {
                js("Groups.player.find(p => p.name == \"" + player.name + "\").team(Team.get(" + team.id + "))");
            });
        }).tooltip("Change Team");

        table.button(Icon.box, Styles.cleari, () -> {
            GodModeDialogs.showItemDialog((item, amount, team) -> {
                js("var team = Team.get(" + team.id
                        + "); if(team.core() != null) team.core().items.add(Vars.content.items.find(i => i.name == \""
                        + item.name + "\"), " + amount + ")");
            });
        }).tooltip("Items");

        table.button(Icon.units, Styles.cleari, () -> {
            GodModeDialogs.showUnitDialog(
                    (unit, amount, team, x, y) -> {
                        js("for(var i=0;i<" + amount + ";i++){ Vars.content.units.find(u => u.name == \"" + unit.name
                                + "\").spawn(Team.get(" + team.id + "), " + x + ", " + y + "); }");
                    },
                    (unit, team) -> {
                        js("Groups.unit.each(u => u.type.name == \"" + unit.name + "\" && u.team.id == " + team.id
                                + ", u => u.kill())");
                    });
        }).tooltip("Units");

        table.button(Icon.effect, Styles.cleari, () -> {
            GodModeDialogs.showEffectDialog(
                    (effect, duration) -> {
                        js("if(Vars.player.unit() != null) Vars.player.unit().apply(Vars.content.statusEffects.find(s => s.name == \""
                                + effect.name + "\"), " + duration + ")");
                    },
                    (effect) -> {
                        js("if(Vars.player.unit() != null) Vars.player.unit().unapply(Vars.content.statusEffects.find(s => s.name == \""
                                + effect.name + "\"))");
                    });
        }).tooltip("Effects");

        table.button(Icon.hammer, Styles.cleari, () -> {
            GodModeDialogs.showCoreDialog((coreBlock, team, x, y) -> {
                js("Vars.world.tileWorld(" + x + ", " + y + ").setNet(Vars.content.block(\"" + coreBlock.name
                        + "\"), Team.get(" + team.id + "), 0)");
            });
        }).tooltip("Place Core");
    }
}
