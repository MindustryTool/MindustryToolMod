package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Styles;

import java.util.Optional;

public class FollowAssistTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final FollowAI ai = new FollowAI();
    private Player targetPlayer;

    @Override
    public String getName() {
        return Iconc.players + " " + Core.bundle.get("autoplay.task.follow-assist.name");
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
        if (!unit.canBuild()) {
            status = Core.bundle.get("autoplay.status.cannot-build");
            return false;
        }

        // If a specific player is selected, try to follow them
        if (targetPlayer != null) {
            if (targetPlayer.con != null && !targetPlayer.dead() && targetPlayer.team() == unit.team) {
                ai.following = targetPlayer.unit();
                status = Core.bundle.format("autoplay.status.following", targetPlayer.name);
                return true;
            }
            targetPlayer = null; // Reset if invalid
        }

        // Otherwise find any player to follow that is building
        Player target = Groups.player.find(
                p -> p.team() == unit.team && p.unit() != null && p.unit() != unit && p.unit().activelyBuilding());

        if (target != null) {
            ai.following = target.unit();
            status = Core.bundle.format("autoplay.status.following", target.name);
            return true;
        }

        status = Core.bundle.get("autoplay.status.no-one-to-follow");
        return false;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Optional<Table> settings() {
        Table table = new Table();
        buildSettings(table);
        return Optional.of(table);
    }

    private void buildSettings(Table table) {
        table.clear();
        table.add("@autoplay.settings.follow-assist.select-player").left().row();

        table.table(Styles.grayPanel, t -> {
            t.button("@autoplay.settings.follow-assist.any-player", Styles.togglet, () -> {
                targetPlayer = null;
                buildSettings(table);
            }).checked(targetPlayer == null).growX().left().row();

            for (Player p : Groups.player) {
                if (p == Vars.player || p.team() != Vars.player.team())
                    continue;

                t.button(p.name, Styles.togglet, () -> {
                    targetPlayer = p;
                    buildSettings(table);
                }).checked(targetPlayer == p).padTop(5).growX().left().row();
            }
        }).growX().padTop(5).row();
    }

    public static class FollowAI extends BaseAutoplayAI {
        public Unit following;

        @Override
        public void updateMovement() {
            if (following == null || !following.isAdded() || following.dead()) {
                following = null;
                unit.plans.clear();
                return;
            }

            // Help build
            if (following.activelyBuilding()) {
                unit.plans.clear();
                unit.plans.addFirst(following.buildPlan());

                var req = unit.buildPlan();
                if (req != null) {
                    float range = Math.min(unit.type.buildRange - 20f, 100f);
                    moveTo(req.tile(), range - 10f, 20f);
                    return;
                }
            } else {
                unit.plans.clear();
            }

            // Help attack
            if (following.isShooting && following.mounts.length > 0) {
                var targetMount = following.mounts[0];
                if (targetMount.target != null) {
                    target = targetMount.target;
                    float range = unit.type.range * 0.8f;
                    moveTo(target, range);
                    unit.lookAt(target);
                    unit.aim(target);
                    unit.controlWeapons(unit.within(target, unit.type.range));
                    return;
                }
            }

            // Just follow if doing nothing
            moveTo(following, following.type.hitSize + unit.type.hitSize / 2f + 40f);
        }
    }
}
