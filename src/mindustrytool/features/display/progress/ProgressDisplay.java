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
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import java.util.Optional;

public class ProgressDisplay implements Feature {
    private boolean enabled = false;
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
                .name("@feature.progress-display.name")
                .description("@feature.progress-display.description")
                .icon(Utils.icons("progress-display.png"))
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
            settingsDialog = new BaseDialog("@progress.settings.title");
            settingsDialog.name = "progressSettingDialog";
            settingsDialog.addCloseButton();
            settingsDialog.shown(this::rebuildSettings);
            settingsDialog.buttons.button("@reset", Icon.refresh, () -> {
                ProgressConfig.reset();
                rebuildSettings();
            }).size(250, 64);
        }
        return Optional.of(settingsDialog);
    }

    private void rebuildSettings() {
        Table settingsContainer = settingsDialog.cont;
        settingsContainer.clear();
        settingsContainer.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // Opacity
        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(ProgressConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", ProgressConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = arc.scene.event.Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            ProgressConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", ProgressConfig.opacity * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        // Scale
        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(ProgressConfig.scale);

        Label scaleValue = new Label(
                String.format("%.0f%%", ProgressConfig.scale * 100),
                Styles.outlineLabel);
        scaleValue.setColor(Color.lightGray);

        Table scaleContent = new Table();
        scaleContent.touchable = arc.scene.event.Touchable.disabled;
        scaleContent.margin(3f, 33f, 3f, 33f);
        scaleContent.add("@scale", Styles.outlineLabel).left().growX();
        scaleContent.add(scaleValue).padLeft(10f).right();

        scaleSlider.changed(() -> {
            ProgressConfig.scale = scaleSlider.getValue();
            scaleValue.setText(String.format("%.0f%%", ProgressConfig.scale * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(scaleSlider, scaleContent).width(width).left().padTop(4f).row();

        // Width
        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(ProgressConfig.width);

        Label widthValue = new Label(
                String.format("%.0f%%", ProgressConfig.width * 100),
                Styles.outlineLabel);
        widthValue.setColor(Color.lightGray);

        Table widthContent = new Table();
        widthContent.touchable = arc.scene.event.Touchable.disabled;
        widthContent.margin(3f, 33f, 3f, 33f);
        widthContent.add("@width", Styles.outlineLabel).left().growX();
        widthContent.add(widthValue).padLeft(10f).right();

        widthSlider.changed(() -> {
            ProgressConfig.width = widthSlider.getValue();
            widthValue.setText(String.format("%.0f%%", ProgressConfig.width * 100));
            ProgressConfig.save();
        });

        settingsContainer.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();
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
        if (!enabled || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
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
            drawBar(build.x, build.y, build.block.size * Vars.tilesize, fraction, build.team.color, remainingTime / scale);
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
