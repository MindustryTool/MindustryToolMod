package mindustrytool.features.display.healthbar;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static mindustry.Vars.*;

public class HealthBarVisualizer implements Feature {

    private static TextureRegion barRegion;
    private BaseDialog dialog;
    private boolean enabled = false;

    private ConcurrentHashMap<UnitType, Float> maxHpMap = new ConcurrentHashMap<>();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.health-bar.name")
                .description("@feature.health-bar.description")
                .icon(Utils.icons("healthbar.png"))
                .order(4)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        HealthBarConfig.load();
        Events.run(Trigger.draw, this::draw);
        Events.run(EventType.WorldLoadEndEvent.class, maxHpMap::clear);
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new BaseDialog("@health-bar.settings.title");
            dialog.name = "healthBarSettingDialog";
            dialog.addCloseButton();
            dialog.buttons.button("@reset", Icon.refresh, () -> {
                HealthBarConfig.reset();
                rebuild();
            }).size(250, 64);

            dialog.shown(this::rebuild);
        }
        return Optional.of(dialog);
    }

    public void draw() {
        if (!enabled || !state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        float zoom = renderer.getScale();

        if (-zoom > -HealthBarConfig.zoomThreshold) {
            return;
        }

        if (barRegion == null) {
            barRegion = Core.atlas.find("white-ui");
            if (barRegion == null || !barRegion.found()) {
                barRegion = Core.atlas.white();
            }
        }

        Draw.z(mindustry.graphics.Layer.shields + 5f);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;

        Groups.unit.intersect(cx - cw / 2f, cy - ch / 2f, cw, ch, this::drawCheck);

        Draw.reset();
    }

    private void drawCheck(Unit unit) {
        if (!unit.isValid()) {
            return;
        }

        boolean damaged = unit.health < unit.maxHealth;
        boolean shielded = unit.shield > 0;

        if (!damaged && !shielded) {
            return;
        }

        drawBar(unit);
    }

    private void drawBar(Unit unit) {
        float x = unit.x;
        float y = unit.y + unit.hitSize * 0.8f + 3f;

        float w = unit.hitSize * 2.5f;
        float h = 2f;

        Draw.color(Color.black, 0.6f * HealthBarConfig.opacity);
        Draw.rect(barRegion, x, y, w + 2f, h + 2f);

        float hpPercent = unit.health / unit.maxHealth;
        float maxHealth = unit.maxHealth;

        if (unit.health > maxHealth) {
            maxHealth = maxHpMap.computeIfAbsent(unit.type, t -> t.health);
            hpPercent = unit.health / maxHealth;
        }

        float left = x - w / 2f;

        Draw.color(unit.team.color, 0.75f * HealthBarConfig.opacity);

        if (hpPercent > 0) {
            float filledW = w * hpPercent;
            float fillCenterX = left + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, y, filledW, h);
        }

        if (unit.shield > 0) {
            float shieldValue = unit.shield / maxHealth;
            Draw.color(Pal.shield, 0.5f * HealthBarConfig.opacity);

            if (shieldValue > 1) {
                float shieldW = w * (shieldValue % 1);
                float shieldCenterX = left + shieldW / 2f;
                Draw.color(Pal.shield.cpy().add(0f, 0f, 0.2f * shieldValue), 0.75f * HealthBarConfig.opacity);
                Draw.rect(barRegion, shieldCenterX, y, shieldW, h);
            }

            float shieldPercent = Math.min(shieldValue, 1f);
            float shieldW = w * shieldPercent;
            float shieldCenterX = left + shieldW / 2f;
            Draw.color(unit.team.color, 0.75f * HealthBarConfig.opacity);
            Draw.rect(barRegion, shieldCenterX, y, shieldW, h);
        }

        Draw.reset();
    }

    private void rebuild() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        Slider zoomSlider = new Slider(0f, 2f, 0.1f, false);
        zoomSlider.setValue(HealthBarConfig.zoomThreshold);

        Label zoomValue = new Label(
                HealthBarConfig.zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", HealthBarConfig.zoomThreshold),
                Styles.outlineLabel);
        zoomValue.setColor(HealthBarConfig.zoomThreshold <= 0.01f ? Color.gray : Color.lightGray);

        Table zoomContent = new Table();
        zoomContent.touchable = arc.scene.event.Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("@health-bar.min-zoom", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValue).padLeft(10f).right();

        zoomSlider.changed(() -> {
            HealthBarConfig.zoomThreshold = zoomSlider.getValue();
            zoomValue.setText(HealthBarConfig.zoomThreshold <= 0.01f ? "@off"
                    : String.format("%.1fx", HealthBarConfig.zoomThreshold));
            zoomValue.setColor(HealthBarConfig.zoomThreshold <= 0.01f ? Color.gray : Color.lightGray);
            HealthBarConfig.save();
        });

        cont.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(HealthBarConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", HealthBarConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = arc.scene.event.Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            HealthBarConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", HealthBarConfig.opacity * 100));
            HealthBarConfig.save();
        });

        cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();
    }
}
