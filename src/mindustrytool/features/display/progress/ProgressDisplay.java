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
import arc.scene.ui.Dialog;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import java.util.Optional;

public class ProgressDisplay implements Feature {
    private BaseDialog settingsDialog;

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
                .name("@feature.progress-display")
                .description("@feature.progress-display.description")
                .icon(Icon.chartBar)
                .order(10)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        ProgressConfig.load();
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public Optional<Dialog> setting() {
        if (settingsDialog == null) {
            settingsDialog = new ProgressDisplaySettingsDialog();
        }
        return Optional.of(settingsDialog);
    }

    private void draw() {
        if (!isEnabled() || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
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

        var scale = build.timeScale();

        if (build instanceof UnitFactory.UnitFactoryBuild b) {
            UnitFactory block = (UnitFactory) b.block;
            if (b.currentPlan != -1 && b.currentPlan < block.plans.size) {
                float totalTime = block.plans.get(b.currentPlan).time;
                fraction = b.progress / totalTime / scale;
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
            drawBar(build.x, build.y, build.block.size * Vars.tilesize, fraction, build.team.color,
                    remainingTime / scale);
        }
    }

    private void drawBar(float x, float y, float size, float fraction, Color color, float remainingTime) {
        float scale = ProgressConfig.scale;
        float widthScale = ProgressConfig.width;
        float opacity = ProgressConfig.opacity;

        float width = size * widthScale;
        float height = 3f * scale;
        float yOffset = (size / 2f - 1) * scale;

        Draw.color(Color.black);
        Draw.alpha(0.8f * opacity);
        Fill.rect(x, y + yOffset, width, height);

        Draw.color(color);
        Draw.alpha(opacity);
        Fill.rect(x - width / 2f + width * fraction / 2f, y + yOffset, width * fraction, height);

        if (remainingTime > 0) {
            String text = String.format("%.1fs", remainingTime);
            Fonts.outline.draw(text, x, y, Color.white, 0.25f * scale, false, Align.center);
        }

        Draw.reset();
    }
}
