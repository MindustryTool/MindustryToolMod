package mindustrytool.features.camerazoom;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Lets players customize how far the camera zooms out (min) and in (max)
 * while playing, including beyond vanilla limits. Vanilla derives its
 * effective in-game bounds every frame from the base {@code minZoom} /
 * {@code maxZoom} renderer fields, so this feature writes those base fields
 * (mapping through the vanilla zoom multiplier settings) and pulls the live
 * scale back into range. Disabling restores the previously observed vanilla
 * bounds.
 */
public class CameraZoomFeature extends Feature {

    public static final float DEFAULT_MIN_ZOOM = 0.5f;
    public static final float DEFAULT_MAX_ZOOM = 6f;

    public static final float MIN_SLIDER_MIN = 0.1f;
    public static final float MIN_SLIDER_MAX = 6f;
    public static final float MIN_SLIDER_STEP = 0.1f;

    public static final float MAX_SLIDER_MIN = 1f;
    public static final float MAX_SLIDER_MAX = 12f;
    public static final float MAX_SLIDER_STEP = 0.5f;

    private static final String MIN_MULT_KEY = "minzoomingamemultiplier";
    private static final String MAX_MULT_KEY = "maxzoomingamemultiplier";

    public final ConfigGroup config;
    public final ConfigValue<Float> minZoomConfig;
    public final ConfigValue<Float> maxZoomConfig;

    private @Nullable CameraZoomSettingsDialog settingsDialog;

    private boolean vanillaCaptured = false;
    private float vanillaMinBase;
    private float vanillaMaxBase;
    private boolean applied = false;

    public CameraZoomFeature() {
        super(FeatureMetadata.builder()
                .id("camera-zoom")
                .icon(FileIcon.of("camera.png"))
                .order(5)
                .enabledByDefault(false)
                .quickAccess(false)
                .build());

        config = configGroup();
        minZoomConfig = config.floatValue("min-zoom", DEFAULT_MIN_ZOOM);
        maxZoomConfig = config.floatValue("max-zoom", DEFAULT_MAX_ZOOM);

        minZoomConfig.signal().subscribe(min -> {
            Float max = maxZoomConfig.signal().peek();
            if (min != null && max != null && min > max) {
                maxZoomConfig.set(min);
            }
        });

        maxZoomConfig.signal().subscribe(max -> {
            Float min = minZoomConfig.signal().peek();
            if (min != null && max != null && max < min) {
                minZoomConfig.set(max);
            }
        });

        Events.run(Trigger.update, this::update);
    }

    public void resetToDefaults() {
        minZoomConfig.reset();
        maxZoomConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new CameraZoomSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    @Override
    public void onDisable() {
        restoreVanillaBounds();
    }

    private void update() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.renderer == null
                || Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            restoreVanillaBounds();
            return;
        }

        if (!vanillaCaptured) {
            vanillaMinBase = Vars.renderer.minZoom;
            vanillaMaxBase = Vars.renderer.maxZoom;
            vanillaCaptured = true;
        }

        Float minBoxed = minZoomConfig.signal().peek();
        Float maxBoxed = maxZoomConfig.signal().peek();
        float lo = Math.min(minBoxed != null ? minBoxed : DEFAULT_MIN_ZOOM,
                maxBoxed != null ? maxBoxed : DEFAULT_MAX_ZOOM);
        float hi = Math.max(minBoxed != null ? minBoxed : DEFAULT_MIN_ZOOM,
                maxBoxed != null ? maxBoxed : DEFAULT_MAX_ZOOM);

        float minMult = validMultiplier(Core.settings.getFloat(MIN_MULT_KEY, 1f));
        float maxMult = validMultiplier(Core.settings.getFloat(MAX_MULT_KEY, 1f));

        Vars.renderer.minZoom = lo * minMult;
        Vars.renderer.maxZoom = hi / maxMult;
        applied = true;

        float minBound = Scl.scl(lo);
        float maxBound = Mathf.round(Scl.scl(hi));
        float scale = Vars.renderer.getScale();
        float clamped = Mathf.clamp(scale, minBound, maxBound);
        if (clamped != scale) {
            Vars.renderer.setScale(clamped);
        }
    }

    private void restoreVanillaBounds() {
        if (!applied || !vanillaCaptured || Vars.renderer == null) {
            return;
        }
        Vars.renderer.minZoom = vanillaMinBase;
        Vars.renderer.maxZoom = vanillaMaxBase;
        applied = false;
    }

    private float validMultiplier(float mult) {
        return mult > 0f && !Float.isNaN(mult) && !Float.isInfinite(mult) ? mult : 1f;
    }
}
