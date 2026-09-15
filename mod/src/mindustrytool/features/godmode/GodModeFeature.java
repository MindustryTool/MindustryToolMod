package mindustrytool.features.godmode;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.PlayEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.overlay.SolimDialog;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

public class GodModeFeature extends Feature {

    public static final String PROVIDER_AUTO = "auto";
    public static final String PROVIDER_INTERNAL = "internal";
    public static final String PROVIDER_JS = "js";

    public final ConfigGroup config;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<String> providerModeConfig;

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final InternalGodModeProvider internalProvider = new InternalGodModeProvider();
    private final JSGodModeProvider jsProvider = new JSGodModeProvider();

    private final Signal<GodModeProvider> providerSignal = Signal.of(null);
    private final Signal<Boolean> fogDisabled = Signal.of(false);

    private @Nullable GodModeHudView hudView;
    private @Nullable GodModeSettingsDialog settingsDialog;

    public GodModeFeature() {
        super(FeatureMetadata.builder()
                .id("god-mode")
                .icon(FileIcon.of("wand-sparkles.png"))
                .order(5)
                .enabledByDefault(false)
                .build());

        config = configGroup();
        scaleConfig = config.floatValue("scale", 1f);
        providerModeConfig = config.stringValue("provider", PROVIDER_AUTO);

        positionGroup = config.group("position");

        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float defX = sw > 0 ? sw / 2f : 400f;
        float defY = sh > 0 ? sh / 2f : 200f;

        xConfig = positionGroup.floatValueKeyed("x", Signals.isPortrait(), p -> p ? "portrait" : "landscape", defX);
        yConfig = positionGroup.floatValueKeyed("y", Signals.isPortrait(), p -> p ? "portrait" : "landscape", defY);

        Float initX = xConfig.get();
        Float initY = yConfig.get();

        xSignal = Signal.of(initX != null ? initX : defX);
        ySignal = Signal.of(initY != null ? initY : defY);

        xSignal.subscribe(val -> {
            if (val != null) {
                xConfig.set(val);
            }
        });
        ySignal.subscribe(val -> {
            if (val != null) {
                yConfig.set(val);
            }
        });

        providerModeConfig.signal().subscribe(m -> checkProvider());

        Events.run(PlayEvent.class, () -> {
            fogDisabled.set(false);
            checkProvider();
        });
        Events.run(StateChangeEvent.class, () -> {
            if (Vars.state != null && !Vars.state.isGame()) {
                fogDisabled.set(false);
            }
            checkProvider();
        });
    }

    public @Nullable GodModeProvider getProvider() {
        String mode = providerModeConfig.get();
        if (PROVIDER_JS.equals(mode)) {
            return jsProvider.isAvailable() ? jsProvider : null;
        }
        if (PROVIDER_INTERNAL.equals(mode)) {
            return internalProvider.isAvailable() ? internalProvider : null;
        }
        if (jsProvider.isAvailable()) {
            return jsProvider;
        }
        if (internalProvider.isAvailable()) {
            return internalProvider;
        }
        return null;
    }

    public Signal<GodModeProvider> providerSignal() {
        return providerSignal;
    }

    public void checkProvider() {
        providerSignal.set(getProvider());
    }

    public Signal<Boolean> fogDisabledSignal() {
        return fogDisabled;
    }

    public void toggleFog() {
        boolean nextDisabled = !Boolean.TRUE.equals(fogDisabled.peek());
        fogDisabled.set(nextDisabled);
        GodModeProvider p = getProvider();
        if (p != null) {
            p.setFog(!nextDisabled);
        }
    }

    public void resetPosition() {
        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float cx = sw > 0 ? sw / 2f : 400f;
        float cy = sh > 0 ? sh / 2f : 200f;

        xConfig.set(cx);
        yConfig.set(cy);
        xSignal.set(cx);
        ySignal.set(cy);

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    @Override
    public void onEnable() {
        checkProvider();

        if (hudView != null) {
            hudView.element().remove();
            hudView.dispose();
        }

        hudView = new GodModeHudView(this);
        Element el = hudView.element();
        el.name = "god-mode-hud";
        el.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    @Override
    public void onDisable() {
        if (Boolean.TRUE.equals(fogDisabled.peek())) {
            toggleFog();
        }
        if (hudView != null) {
            GodModeHudView view = hudView;
            hudView = null;
            Core.app.post(() -> {
                view.element().remove();
                view.dispose();
            });
        }
    }

    @Override
    public @Nullable SolimDialog getSettingDialog() {
        if (settingsDialog == null) {
            settingsDialog = new GodModeSettingsDialog(this);
        }
        return settingsDialog;
    }
}
