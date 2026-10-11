package mindustrytool.features.healthbar;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.geom.Rect;
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
import mindustrytool.features.settings.ModSettings;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;

import solim.overlay.SolimDialog;

/**
 * Draws health bars above damaged units and a single shield bar with a
 * whole-bar count above shielded units, plus optional HP bars above damaged
 * buildings. Configuration is synchronized to primitive cached fields via reactive
 * signal subscriptions; frame scratch fields copy these primitives once per frame with
 * zero boxing, allocations, or signal peeking.
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

    private final Rect viewBounds = new Rect();
    private @Nullable TextureRegion barRegion;
    private @Nullable HealthBarSettingsDialog settingsDialog;

    private static final String[] SHIELD_COUNT_CACHE = new String[64];
    private static final String SHIELD_COUNT_MAX = "x64+";

    static {
        for (int i = 0; i < 64; i++) {
            SHIELD_COUNT_CACHE[i] = "x" + i;
        }
    }

    // Cached primitive configurations synchronized via signal subscriptions
    private boolean cachedShowFriendlyUnits = true;
    private boolean cachedShowEnemyUnits = true;
    private boolean cachedShowFriendlyBlocks = false;
    private boolean cachedShowEnemyBlocks = false;
    private float cachedZoomThreshold = 0.5f;
    private float cachedOpacity = 1f;
    private float cachedScale = 1f;
    private float cachedWidth = 1f;

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
                .quickAccess(false)
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

        // Synchronize primitive caches on reactive signal changes
        showFriendlyUnitsConfig.signal().subscribe(v -> cachedShowFriendlyUnits = v != null ? v : true);
        showEnemyUnitsConfig.signal().subscribe(v -> cachedShowEnemyUnits = v != null ? v : true);
        showFriendlyBlocksConfig.signal().subscribe(v -> cachedShowFriendlyBlocks = v != null ? v : false);
        showEnemyBlocksConfig.signal().subscribe(v -> cachedShowEnemyBlocks = v != null ? v : false);
        zoomThresholdConfig.signal().subscribe(v -> cachedZoomThreshold = v != null ? v : 0.5f);
        opacityConfig.signal().subscribe(v -> cachedOpacity = v != null ? v : 1f);
        scaleConfig.signal().subscribe(v -> cachedScale = v != null ? v : 1f);
        widthConfig.signal().subscribe(v -> cachedWidth = v != null ? v : 1f);

        syncConfigCache();

        bindToggle("healthBar", KeyCode.unset);
        bindDialog("healthBarSettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::draw);
    }

    private void syncConfigCache() {
        Boolean sfu = showFriendlyUnitsConfig.get();
        cachedShowFriendlyUnits = sfu != null ? sfu : true;
        Boolean seu = showEnemyUnitsConfig.get();
        cachedShowEnemyUnits = seu != null ? seu : true;
        Boolean sfb = showFriendlyBlocksConfig.get();
        cachedShowFriendlyBlocks = sfb != null ? sfb : false;
        Boolean seb = showEnemyBlocksConfig.get();
        cachedShowEnemyBlocks = seb != null ? seb : false;
        Float zt = zoomThresholdConfig.get();
        cachedZoomThreshold = zt != null ? zt : 0.5f;
        Float op = opacityConfig.get();
        cachedOpacity = op != null ? op : 1f;
        Float sc = scaleConfig.get();
        cachedScale = sc != null ? sc : 1f;
        Float w = widthConfig.get();
        cachedWidth = w != null ? w : 1f;
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

        syncConfigCache();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
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

        frameZoomThreshold = cachedZoomThreshold;
        frameOpacity = cachedOpacity;
        frameScale = ModSettings.effectiveScale(cachedScale);
        frameWidth = cachedWidth;
        frameShowFriendlyUnits = cachedShowFriendlyUnits;
        frameShowEnemyUnits = cachedShowEnemyUnits;
        frameShowFriendlyBlocks = cachedShowFriendlyBlocks;
        frameShowEnemyBlocks = cachedShowEnemyBlocks;
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

        Core.camera.bounds(viewBounds);
        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = viewBounds.width;
        float ch = viewBounds.height;

        if (frameShowFriendlyUnits || frameShowEnemyUnits) {
            Groups.unit.intersect(viewBounds.x, viewBounds.y, viewBounds.width, viewBounds.height, unitDrawer);
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

        float halfSize = (build.block.size * Vars.tilesize) / 2f;
        if (build.x + halfSize < viewBounds.x || build.x - halfSize > viewBounds.x + viewBounds.width
                || build.y + halfSize + 16f < viewBounds.y || build.y - halfSize > viewBounds.y + viewBounds.height) {
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
            int countIdx = (int) whole;
            String count = countIdx < SHIELD_COUNT_CACHE.length ? SHIELD_COUNT_CACHE[countIdx] : SHIELD_COUNT_MAX;
            Fonts.outline.draw(count, x + w / 2f + 3f * frameScale, shieldY,
                    Color.white, h * 0.11f, false, Align.left);
        }
    }
}
