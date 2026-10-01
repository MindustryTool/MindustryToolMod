package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Position;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Entityc;
import mindustry.gen.Healthc;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class FleeTask implements AutoplayTask {

    public static final String ID = "flee";

    private final ConfigValue<Float> fleeThreshold;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final FleeAI ai = new FleeAI();

    public FleeTask(AutoplayFeature feature) {
        this.fleeThreshold = feature.configGroup().floatValue("flee.threshold", 0.4f);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.flee");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("shield-alert.png", Icon.move);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public boolean update(Unit unit) {
        float threshold = fleeThreshold.get() != null ? fleeThreshold.get() : 0.4f;
        boolean lowHp = unit.health < unit.maxHealth * threshold;
        boolean unarmed = !unit.canShoot() || unit.type.weapons.isEmpty();

        if (!lowHp && !unarmed) {
            ai.threat = null;
            status.set(Core.bundle.get("feature.autoplay.status.safe"));
            return false;
        }

        Unit enemyUnit = Units.closestEnemy(unit.team, unit.x, unit.y, 400f, u -> !u.dead() && (u.canShoot() || !u.type.weapons.isEmpty()));
        Building enemyTurret = Vars.indexer.findEnemyTile(unit.team, unit.x, unit.y, 400f, b -> b.block instanceof Turret);

        if (enemyUnit == null && enemyTurret == null) {
            ai.threat = null;
            status.set(Core.bundle.get("feature.autoplay.status.safe"));
            return false;
        }

        ai.threat = enemyUnit != null ? enemyUnit : enemyTurret;
        status.set(Core.bundle.get("feature.autoplay.status.fleeing"));
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
        Readable<String> thresholdLabel = fleeThreshold.signal().map(val ->
                Core.bundle.format("feature.autoplay.settings.flee.threshold", Math.round((val != null ? val : 0.4f) * 100)));

        column().growX().gap(unit(1)).children(() -> {
            text(thresholdLabel).growX().left().color(WebStyles.Colors.GHOST_FG);
            slider(fleeThreshold.signal(), 0.1f, 0.7f, 0.05f).growX();
        });
    }

    public static class FleeAI extends BaseAutoplayAI {
        private @Nullable Position threat;

        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            if (threat instanceof Entityc && !((Entityc) threat).isAdded()) {
                threat = null;
            } else if (threat instanceof Healthc && ((Healthc) threat).dead()) {
                threat = null;
            }

            Building core = unit.closestCore();
            if (core != null) {
                moveTo(core, 40f);
            } else if (threat != null) {
                moveTo(threat, 600f, 40f, true, null);
            }
        }
    }
}
