package mindustrytool.features.display.healthbar;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Dialog;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;
import static mindustry.Vars.*;

public class HealthBarVisualizer implements Feature {

    private static TextureRegion barRegion;
    private BaseDialog dialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.health-bar.name")
                .description("@feature.health-bar.description")
                .icon(Utils.icons("healthbar.png"))
                .order(4)
                .enabledByDefault(false)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        HealthBarConfig.load();
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new HealthBarSettingsDialog();
        }
        return Optional.of(dialog);
    }

    public void draw() {
        if (!isEnabled() || !state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
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
        float scale = HealthBarConfig.scale;
        float widthScale = HealthBarConfig.width;

        float x = unit.x;
        float y = unit.y + (unit.hitSize * 0.8f + 3f) * scale;

        float w = unit.hitSize * 2.5f * widthScale;
        float h = 2f * scale;

        Draw.color(Color.black, 0.6f * HealthBarConfig.opacity);
        Draw.rect(barRegion, x, y, w + 2f, h + 2f);

        float maxHealth = Math.max(unit.maxHealth, 1f);
        if (Float.isNaN(maxHealth))
            maxHealth = 1f;

        float hpPercent = Math.max(0f, Math.min(1f, unit.health / maxHealth));

        float left = x - w / 2f;

        Draw.color(unit.team.color, 0.75f * HealthBarConfig.opacity);

        if (hpPercent > 0) {
            float filledW = w * hpPercent;
            float fillCenterX = left + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, y, filledW, h);
        }

        if (unit.shield > 0) {
            float shieldValue = unit.shield / maxHealth;
            if (Float.isNaN(shieldValue))
                shieldValue = 0f;

            // Cap the maximum number of shield bars to prevent OOM
            shieldValue = Math.min(shieldValue, 20f);

            Draw.color(Pal.shield, 0.5f * HealthBarConfig.opacity);

            while (shieldValue > 0) {
                y += h * 1.8f;

                Draw.color(Color.black, 0.6f * HealthBarConfig.opacity);
                Draw.rect(barRegion, x, y, w + 2f, h + 2f);

                float shieldPercent = Math.min(shieldValue, 1f);
                float shieldW = w * shieldPercent;
                float shieldCenterX = left + shieldW / 2f;
                Draw.color(unit.team.color, 0.75f * HealthBarConfig.opacity);
                Draw.rect(barRegion, shieldCenterX, y, shieldW, h);

                shieldValue -= 1;
            }
        }

        Draw.reset();
    }

}
