package mindustrytool.features.autoplay.tasks;

import java.util.Optional;

import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Styles;

public class AssistTask implements AutoplayTask {
    private boolean enabled = true;
    private final BuilderAI ai = new BuilderAI();

    @Override
    public String getName() {
        return Iconc.players + " Auto Build";
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.players;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean shouldRun(Unit unit) {
        if (ai.assistFollowing != null) {
            return true;
        }

        if (!unit.canBuild()) {
            return false;
        }

        var buildingPlayer = Groups.player
                .find(p -> p.unit() != null && unit.team() == p.team() && p.unit().buildPlan() != null);

        if (buildingPlayer != null) {
            unit.plans.add(buildingPlayer.unit().buildPlan());

            return true;
        }

        return !Vars.player.team().data().plans.isEmpty();
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public Optional<Table> settings() {
        Table table = new Table();
        build(table);

        return Optional.of(table);
    }

    private void build(Table table) {
        table.clear();
        table.add("@following").top().left().labelAlign(Align.left).padBottom(5).row();

        table.button("None", Styles.togglet, () -> {
            ai.assistFollowing = null;
            build(table);
        })
                .checked(ai.assistFollowing == null)
                .growX()
                .top()
                .left()
                .labelAlign(Align.left)
                .padBottom(5)
                .row();

        table.pane(t -> {
            t.top();

            for (Player p : Groups.player) {
                if (p.team() != Vars.player.team() || p == Vars.player) {
                    continue;
                }

                t.button(p.name, Styles.togglet, () -> {
                    ai.assistFollowing = p.unit();
                    build(table);
                })
                        .checked(ai.assistFollowing == p.unit())
                        .top()
                        .left()
                        .labelAlign(Align.left)
                        .padBottom(5)
                        .growX()
                        .row();
            }
        }).growX();
    }
}
