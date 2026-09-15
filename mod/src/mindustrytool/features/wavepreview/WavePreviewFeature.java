package mindustrytool.features.wavepreview;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.WaveEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.core.Provider;
import solim.overlay.SolimDialog;

/**
 * Wave Preview feature providing upcoming wave unit compositions directly inside the HUD waves panel.
 */
public class WavePreviewFeature extends Feature {

    public static final int MIN_DEPTH = 1;
    public static final int MAX_DEPTH = 5;
    public static final int DEFAULT_DEPTH = 1;

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Integer> depthConfig;

    private final WavePreviewState state = new WavePreviewState();
    private @Nullable WavePreviewPanelView panelView;
    private @Nullable WavePreviewSettingsDialog settingsDialog;

    public WavePreviewFeature() {
        super(FeatureMetadata.builder()
                .id("wave-preview")
                .icon(FileIcon.of("swords.png"))
                .order(1)
                .quickAccess(true)
                .enabledByDefault(true)
                .build());

        config = configGroup();
        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);
        depthConfig = config.intValue("depth", DEFAULT_DEPTH);

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                injectPanel();
            }
        }));

        Events.run(WaveEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                recompute();
            }
        }));

        depthConfig.signal().subscribe(d -> {
            if (isEnabled()) {
                recompute();
            }
        });
    }

    public WavePreviewState getState() {
        return state;
    }

    @Override
    public void onEnable() {
        Core.app.post(this::injectPanel);
    }

    @Override
    public void onDisable() {
        detachPanel();
        state.clear();
    }

    @Override
    public @Nullable Provider<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new WavePreviewSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public void injectPanel() {
        if (!isEnabled()) {
            return;
        }

        if (Vars.ui == null || Vars.ui.hudGroup == null) {
            return;
        }

        Stack parent = Vars.ui.hudGroup.find("waves/editor");
        if (parent == null) {
            Log.err("WavePreviewFeature: waves/editor not found");
            return;
        }

        Table waves = parent.find("waves");
        if (waves == null) {
            Log.err("WavePreviewFeature: waves not found");
            return;
        }

        detachPanel();

        panelView = new WavePreviewPanelView(this);
        waves.row();
        waves.add(panelView.element()).growX().padTop(10f);

        recompute();
    }

    public void detachPanel() {
        if (panelView != null) {
            panelView.element().remove();
            panelView.dispose();
            panelView = null;
        }
    }

    public void recompute() {
        if (Vars.state != null) {
            int depth = depthConfig.get() != null ? depthConfig.get() : DEFAULT_DEPTH;
            state.recompute(Vars.state.wave, depth);
        }
    }
}
