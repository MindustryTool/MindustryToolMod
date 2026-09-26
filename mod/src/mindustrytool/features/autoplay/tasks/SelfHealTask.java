package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Geometry;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.world.meta.BlockFlag;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class SelfHealTask implements AutoplayTask {

    public static final String ID = "self-heal";

    private final ConfigValue<Float> healThreshold;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final SelfHealAI ai = new SelfHealAI();
    private boolean isHealing = false;

    public SelfHealTask(AutoplayFeature feature) {
        this.healThreshold = feature.configGroup().floatValue("self-heal.threshold", 0.6f);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.self-heal");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("heart.png", Icon.refresh);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public boolean update(Unit unit) {
        Seq<Building> repairPoints = Vars.indexer.getFlagged(unit.team, BlockFlag.repair);
        if (repairPoints.isEmpty()) {
            isHealing = false;
            status.set(Core.bundle.get("feature.autoplay.status.no-repair-point"));
            return false;
        }

        float threshold = healThreshold.get() != null ? healThreshold.get() : 0.6f;
        if (unit.health < unit.maxHealth * threshold) {
            isHealing = true;
        }

        if (isHealing) {
            if (unit.health >= unit.maxHealth) {
                isHealing = false;
                status.set(Core.bundle.get("feature.autoplay.status.healthy"));
                return false;
            }

            status.set(Core.bundle.get("feature.autoplay.status.low-hp"));
            return true;
        }

        status.set(Core.bundle.get("feature.autoplay.status.healthy"));
        return false;
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
        Readable<String> thresholdLabel = healThreshold.signal().map(val ->
                Core.bundle.format("feature.autoplay.settings.self-heal.threshold", Math.round((val != null ? val : 0.6f) * 100)));

        column().growX().gap(unit(1)).children(() -> {
            text(thresholdLabel).growX().left().color(WebStyles.Colors.GHOST_FG);
            slider(healThreshold.signal(), 0.2f, 0.8f, 0.05f).growX();
        });
    }

    public static class SelfHealAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            Seq<Building> repairPoints = Vars.indexer.getFlagged(unit.team, BlockFlag.repair);
            if (repairPoints != null && !repairPoints.isEmpty()) {
                Building repair = Geometry.findClosest(unit.x, unit.y, repairPoints);
                if (repair != null) {
                    moveTo(repair, 50f);
                }
            }
        }
    }
}
