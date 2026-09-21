package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.entities.units.BuildPlan;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class FollowAssistTask implements AutoplayTask {

    public static final String ID = "follow-assist";

    private final ConfigValue<String> targetPlayerName;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final FollowAI ai = new FollowAI();

    public FollowAssistTask(AutoplayFeature feature) {
        this.targetPlayerName = feature.configGroup().stringValue("follow-assist.target-player", "");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.follow-assist");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.players;
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    private boolean isActiveBuilder(Player p, Team team) {
        return p != Vars.player && p.team() == team && p.unit() != null && !p.unit().dead
                && (p.unit().activelyBuilding() || !p.unit().plans.isEmpty());
    }

    @Override
    public boolean update(Unit unit) {
        if (!Vars.net.active() || Groups.player.size() <= 1) {
            ai.following = null;
            status.set(Core.bundle.get("feature.autoplay.status.singleplayer"));
            return false;
        }

        String preferredName = targetPlayerName.get();
        Player target = preferredName != null && !preferredName.trim().isEmpty()
                ? Groups.player.find(p -> p != Vars.player && p.team() == unit.team && preferredName.equals(p.name) && p.unit() != null && !p.unit().dead)
                : null;

        if (target == null) {
            target = Groups.player.find(p -> isActiveBuilder(p, unit.team));
        }

        if (target == null) {
            ai.following = null;
            status.set(Core.bundle.get("feature.autoplay.status.no-active-builders"));
            return false;
        }

        ai.following = target.unit();
        status.set(Core.bundle.format("feature.autoplay.status.following", target.name));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().growX().gap(unit(1)).children(() -> {
            text(Core.bundle.get("feature.autoplay.settings.follow-assist.select-player")).growX().left().color(WebStyles.Colors.GHOST_FG);

            wrap().gap(unit(1)).left().children(() -> {
                Readable<Boolean> anyChecked = targetPlayerName.signal().map(name -> name == null || name.trim().isEmpty());
                button(() -> targetPlayerName.set(""))
                        .style(WebStyles.filterChipText())
                        .paddingX(unit(2))
                        .checked(anyChecked)
                        .height(unit(8))
                        .children(() -> text(Core.bundle.get("feature.autoplay.settings.follow-assist.any-builder"))
                                .color(anyChecked.map(c -> c ? Color.white : Color.gray)));

                for (Player p : Groups.player) {
                    if (p == Vars.player || p.team() != Vars.player.team()) {
                        continue;
                    }
                    String pName = p.name;
                    Readable<Boolean> playerChecked = targetPlayerName.signal().map(name -> pName.equals(name));
                    button(() -> targetPlayerName.set(pName))
                            .style(WebStyles.filterChipText())
                            .paddingX(unit(2))
                            .checked(playerChecked)
                            .height(unit(8))
                            .children(() -> text(pName)
                                    .color(playerChecked.map(c -> c ? Color.white : Color.gray)));
                }
            });
        });
    }

    public static class FollowAI extends BaseAutoplayAI {
        public @Nullable Unit following;

        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            if (following == null || !following.isAdded() || following.dead()) {
                following = null;
                return;
            }

            if (following.activelyBuilding() && unit.canBuild()) {
                BuildPlan plan = following.buildPlan();
                if (plan != null) {
                    boolean alreadyQueued = false;
                    for (int i = 0; i < unit.plans.size; i++) {
                        BuildPlan p = unit.plans.get(i);
                        if (p != null && p.samePos(plan)) {
                            alreadyQueued = true;
                            break;
                        }
                    }
                    if (!alreadyQueued) {
                        unit.plans.addFirst(plan);
                    }
                    Position target = plan.tile() != null ? plan.tile() : new Vec2(plan.x * Vars.tilesize, plan.y * Vars.tilesize);
                    float range = Math.min(unit.type.buildRange - 20f, 100f);
                    moveTo(target, Math.max(range - 10f, 20f), 20f);
                    return;
                }
            }

            if (following.isShooting && following.mounts.length > 0) {
                for (WeaponMount mount : following.mounts) {
                    if (mount.target != null) {
                        target = mount.target;
                        float range = unit.type.range * 0.8f;
                        moveTo(target, Math.max(range, 20f));
                        unit.lookAt(target);
                        unit.aim(target);
                        unit.controlWeapons(unit.within(target, unit.type.range));
                        return;
                    }
                }
            }

            if (following.mineTile != null && following.mineTile.drop() != null && unit.canMine(following.mineTile.drop())) {
                moveTo(following.mineTile, unit.type.mineRange / 2f, 20f);
                if (unit.within(following.mineTile, unit.type.mineRange) && unit.validMine(following.mineTile)) {
                    unit.mineTile = following.mineTile;
                    return;
                }
            }

            moveTo(following, following.type.hitSize + unit.type.hitSize / 2f + 40f);
        }
    }
}
