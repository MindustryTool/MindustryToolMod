package mindustrytool.features.healthbar;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.core.Provider;
import solim.overlay.SolimDialog;

/**
 * Draws health bars above damaged units and a single shield bar with a
 * whole-bar count above shielded units, plus optional HP bars above damaged
 * buildings. Configuration is peeked once per frame into scratch fields;
 * the per-entity loops themselves perform no config reads, allocations,
 * or subscriptions.
 */
public class HealthBarFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> showFriendlyUnitsConfig;
    public final ConfigValue<Boolean> showEnemyUnitsConfig;
    public final ConfigValue<Boolean> showFriendlyBlocksConfig;
    public final ConfigValue<Boolean> showEnemyBlocksConfig;
    public final ConfigValue<Float> zoomThresholdConfig;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Float> widthConfig;

    private final Cons<Unit> unitDrawer = this::drawCheck;
    private final Cons<Building> buildingDrawer = this::drawBuildingCheck;
    private static final Boolf<Building> ANY_BUILDING = build -> true;

    private @Nullable TextureRegion barRegion;
    private @Nullable HealthBarSettingsDialog settingsDialog;

    // Per-frame scratch written once in draw(), read by the per-entity loops.
    private boolean frameShowFriendlyUnits;
    private boolean frameShowEnemyUnits;
    private boolean frameShowFriendlyBlocks;
    private boolean frameShowEnemyBlocks;
    private @Nullable Team framePlayerTeam;
    private float frameZoomThreshold;
    private float frameOpacity;
    private float frameScale;
    private float frameWidth;

    public HealthBarFeature() {
        super(FeatureMetadata.builder()
                .id("health-bar")
                .icon(FileIcon.of("healthbar.png"))
                .order(4)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        showFriendlyUnitsConfig = config.boolValue("show-friendly-units", true);
        showEnemyUnitsConfig = config.boolValue("show-enemy-units", true);
        showFriendlyBlocksConfig = config.boolValue("show-friendly-blocks", false);
        showEnemyBlocksConfig = config.boolValue("show-enemy-blocks", false);
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);
        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);
        widthConfig = config.floatValue("width", 1f);

        Events.run(Trigger.draw, this::draw);
    }

    public void resetToDefaults() {
        showFriendlyUnitsConfig.reset();
        showEnemyUnitsConfig.reset();
        showFriendlyBlocksConfig.reset();
        showEnemyBlocksConfig.reset();
        zoomThresholdConfig.reset();
        opacityConfig.reset();
        scaleConfig.reset();
        widthConfig.reset();
    }

    @Override
    public @Nullable Provider<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new HealthBarSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void draw() {
        if (!isEnabled() || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        Float zoomThreshold = zoomThresholdConfig.signal().peek();
        Float opacity = opacityConfig.signal().peek();
        Float scale = scaleConfig.signal().peek();
        Float width = widthConfig.signal().peek();
        Boolean showFriendlyUnits = showFriendlyUnitsConfig.signal().peek();
        Boolean showEnemyUnits = showEnemyUnitsConfig.signal().peek();
        Boolean showFriendlyBlocks = showFriendlyBlocksConfig.signal().peek();
        Boolean showEnemyBlocks = showEnemyBlocksConfig.signal().peek();

        frameZoomThreshold = zoomThreshold != null ? zoomThreshold : 0.5f;
        frameOpacity = opacity != null ? opacity : 1f;
        frameScale = scale != null ? scale : 1f;
        frameWidth = width != null ? width : 1f;
        frameShowFriendlyUnits = showFriendlyUnits != null ? showFriendlyUnits : true;
        frameShowEnemyUnits = showEnemyUnits != null ? showEnemyUnits : true;
        frameShowFriendlyBlocks = showFriendlyBlocks != null ? showFriendlyBlocks : false;
        frameShowEnemyBlocks = showEnemyBlocks != null ? showEnemyBlocks : false;
        framePlayerTeam = Vars.player != null ? Vars.player.team() : null;

        float zoom = Vars.renderer.getScale();

        if (frameZoomThreshold > 0.01f && zoom < frameZoomThreshold) {
            return;
        }

        if (barRegion == null) {
            barRegion = Core.atlas.find("white-ui");
            if (barRegion == null || !barRegion.found()) {
                barRegion = Core.atlas.white();
            }
        }

        float z = Draw.z();
        Draw.z(Layer.shields + 5f);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;

        if (frameShowFriendlyUnits || frameShowEnemyUnits) {
            Groups.unit.intersect(cx - cw / 2f, cy - ch / 2f, cw, ch, unitDrawer);
        }

        if (frameShowFriendlyBlocks || frameShowEnemyBlocks) {
            float radius = Math.max(cw, ch) * 0.75f;
            Vars.indexer.eachBlock(null, cx, cy, radius, ANY_BUILDING, buildingDrawer);
        }

        Draw.z(z);
        Draw.reset();
    }

    private boolean isFriendly(@Nullable Team team) {
        return framePlayerTeam != null && team == framePlayerTeam;
    }

    private void drawCheck(Unit unit) {
        if (!unit.isValid()) {
            return;
        }

        boolean show = isFriendly(unit.team) ? frameShowFriendlyUnits : frameShowEnemyUnits;
        if (!show) {
            return;
        }

        boolean damaged = unit.health < unit.maxHealth;
        boolean shielded = unit.shield > 0;

        if (!damaged && !shielded) {
            return;
        }

        drawBar(unit);
    }

    private void drawBuildingCheck(Building build) {
        if (build == null || !build.isValid()) {
            return;
        }

        boolean show = isFriendly(build.team) ? frameShowFriendlyBlocks : frameShowEnemyBlocks;
        if (!show) {
            return;
        }

        if (build.health >= build.maxHealth) {
            return;
        }

        drawBuildingBar(build);
    }

    private void drawBuildingBar(Building build) {
        float scale = frameScale;

        float size = build.block.size * Vars.tilesize;
        float w = size * frameWidth;
        float h = 2f * scale;
        float y = build.y + (size / 2f + 3f) * scale;

        Draw.color(Color.black, 0.6f * frameOpacity);
        Draw.rect(barRegion, build.x, y, w + 2f, h + 2f);

        float maxHealth = Math.max(build.maxHealth, 1f);
        if (Float.isNaN(maxHealth)) {
            maxHealth = 1f;
        }

        float health = build.health;
        if (Float.isNaN(health) || health < 0f) {
            health = 0f;
        }

        float hpPercent = Math.max(0f, Math.min(1f, health / maxHealth));

        Draw.color(build.team.color, 0.75f * frameOpacity);

        if (hpPercent > 0) {
            float filledW = w * hpPercent;
            float fillCenterX = build.x - w / 2f + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, y, filledW, h);
        }

        Draw.reset();
    }

    private void drawBar(Unit unit) {
        float scale = frameScale;
        float widthScale = frameWidth;

        float x = unit.x;
        float y = unit.y + (unit.hitSize * 0.8f + 3f) * scale;

        float w = unit.hitSize * 2.5f * widthScale;
        float h = 2f * scale;

        Draw.color(Color.black, 0.6f * frameOpacity);
        Draw.rect(barRegion, x, y, w + 2f, h + 2f);

        float maxHealth = Math.max(unit.maxHealth, 1f);
        if (Float.isNaN(maxHealth)) {
            maxHealth = 1f;
        }

        float health = unit.health;
        if (Float.isNaN(health) || health < 0f) {
            health = 0f;
        }

        float hpPercent = Math.max(0f, Math.min(1f, health / maxHealth));

        float left = x - w / 2f;

        Draw.color(unit.team.color, 0.75f * frameOpacity);

        if (hpPercent > 0) {
            float filledW = w * hpPercent;
            float fillCenterX = left + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, y, filledW, h);
        }

        if (unit.shield > 0) {
            float shieldValue = unit.shield / maxHealth;
            if (Float.isNaN(shieldValue) || shieldValue < 0f) {
                shieldValue = 0f;
            }

            if (shieldValue > 0f) {
                drawShield(x, y, w, h, left, shieldValue, unit.team.color);
            }
        }

        Draw.reset();
    }

    private void drawShield(float x, float y, float w, float h, float left, float shieldValue, Color teamColor) {
        float whole = (float) Math.floor(shieldValue);
        float fill = shieldValue - whole;
        if (fill <= 0f && whole > 0f) {
            fill = 1f;
        }

        float shieldY = y + h * 1.8f;

        Draw.color(Color.black, 0.6f * frameOpacity);
        Draw.rect(barRegion, x, shieldY, w + 2f, h + 2f);

        Draw.color(teamColor, 0.75f * frameOpacity);

        float filledW = w * Math.max(0f, Math.min(1f, fill));
        if (filledW > 0f) {
            float fillCenterX = left + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, shieldY, filledW, h);
        }

        if (whole >= 1f) {
            String count = "x" + (int) whole;
            Fonts.outline.draw(count, x + w / 2f + 3f * frameScale, shieldY,
                    Color.white, h * 0.11f, false, Align.left);
        }

        Draw.reset();
    }
}
