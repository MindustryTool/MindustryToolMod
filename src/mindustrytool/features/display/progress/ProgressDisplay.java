package mindustrytool.features.display.progress;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Align;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class ProgressDisplay implements Feature {
    private boolean enabled = false;

    private final Cons<Building> buildingDrawer = this::drawBuilding;
    private final Boolf<Building> buildingPredicate = b -> {
        if (!b.isValid())
            return false;
        return b instanceof UnitFactory.UnitFactoryBuild ||
                b instanceof Reconstructor.ReconstructorBuild ||
                b instanceof UnitAssembler.UnitAssemblerBuild;
    };

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.progress-display.name")
                .description("@feature.progress-display.description")
                .icon(Utils.icons("progress-display.png"))
                .order(10)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    private void draw() {
        if (!enabled || !Vars.state.isGame()) {
            return;
        }

        Draw.z(Layer.overlayUI);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;
        float maxDimension = Math.max(cw, ch);
        float radius = maxDimension * 0.75f;

        Vars.indexer.eachBlock(null, cx, cy, radius, buildingPredicate, buildingDrawer);

        Draw.reset();
    }

    private void drawBuilding(Building build) {
        float fraction = 0f;
        float remainingTime = 0f;

        if (build instanceof UnitFactory.UnitFactoryBuild b) {
            UnitFactory block = (UnitFactory) b.block;
            if (b.currentPlan != -1 && b.currentPlan < block.plans.size) {
                float totalTime = block.plans.get(b.currentPlan).time;
                fraction = b.progress / totalTime;
                remainingTime = (totalTime - b.progress) / 60f;
            }
        } else if (build instanceof Reconstructor.ReconstructorBuild b) {
            Reconstructor block = (Reconstructor) b.block;
            if (block.constructTime > 0) {
                float totalTime = block.constructTime;
                fraction = b.progress / totalTime;
                remainingTime = (totalTime - b.progress) / 60f;
            }
        } else if (build instanceof UnitAssembler.UnitAssemblerBuild b) {
            if (b.plan() != null) {
                float totalTime = b.plan().time;
                fraction = b.progress / totalTime;
                remainingTime = (totalTime - b.progress) / 60f;
            }
        }

        fraction = Mathf.clamp(fraction, 0f, 1f);

        if (fraction > 0.01f) {
            drawBar(build.x, build.y, build.block.size * Vars.tilesize, fraction, build.team.color, remainingTime);
        }
    }

    private void drawBar(float x, float y, float size, float fraction, Color color, float remainingTime) {
        float width = size;
        float height = 3f;
        float yOffset = size / 2f - 1;

        Draw.color(Color.black);
        Draw.alpha(0.8f);
        Fill.rect(x, y + yOffset, width, height);

        Draw.color(color);
        Fill.rect(x - width / 2f + width * fraction / 2f, y + yOffset, width * fraction, height);

        if (remainingTime > 0) {
            String text = String.format("%.1fs", remainingTime);
            Fonts.outline.draw(text, x, y, Color.white, 0.25f, false, Align.center);
        }

        Draw.reset();
    }
}
