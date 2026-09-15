package mindustrytool.features.godmode;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.PlayEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.PopupDisplayFeature;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;

import solim.overlay.SolimDialog;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

public class GodModeFeature extends Feature implements PopupDisplayFeature {

    public static final String PROVIDER_AUTO = "auto";
    public static final String PROVIDER_INTERNAL = "internal";
    public static final String PROVIDER_JS = "js";
    public static final String DISPLAY_HUD = "hud";
    public static final String DISPLAY_POPUP = "popup";

    public final ConfigGroup config;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<String> providerModeConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;

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
                .quickAccess(true)
                .build());

        config = configGroup();
        scaleConfig = config.floatValue("scale", 1f);
        displayModeConfig = config.stringValue("displayMode", DISPLAY_HUD);
        providerModeConfig = config.stringValue("provider", PROVIDER_AUTO);
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);

        positionGroup = config.group("position");

        xConfig = positionGroup.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = Units.screenWidth();
                    return sw > 0 ? sw / 2f : 400f;
                });
        yConfig = positionGroup.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = Units.screenHeight();
                    return sh > 0 ? sh / 2f : 200f;
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        providerModeConfig.signal().subscribe(m -> checkProvider());
        displayModeConfig.signal().subscribe(mode -> updateHud());

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

        Core.settings.put("mindustrytool.god-mode.position.x.portrait", cx);
        Core.settings.put("mindustrytool.god-mode.position.x.landscape", cx);
        Core.settings.put("mindustrytool.god-mode.position.y.portrait", cy);
        Core.settings.put("mindustrytool.god-mode.position.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    public boolean isPopupMode() {
        return DISPLAY_POPUP.equals(displayModeConfig.get());
    }

    public boolean isPopupActive() {
        if (!isPopupMode()) {
            return false;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            return quickAccess != null && quickAccess.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void togglePopup(@Nullable Element quickAccessBar) {
        GodModePopup.toggle(this, quickAccessBar);
    }

    @Override
    public void openPopup(@Nullable Element quickAccessBar) {
        GodModePopup.toggle(this, quickAccessBar);
    }

    @Override
    public void onEnable() {
        checkProvider();
        updateHud();
    }

    @Override
    public void onDisable() {
        if (Boolean.TRUE.equals(fogDisabled.peek())) {
            toggleFog();
        }
        removeHud();
    }

    private boolean quickAccessHooked = false;

    private void updateHud() {
        ensureQuickAccessHook();
        if (!isEnabled() || isPopupActive()) {
            removeHud();
            return;
        }
        ensureHud();
    }

    private void ensureQuickAccessHook() {
        if (quickAccessHooked) {
            return;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            if (quickAccess == null) {
                return;
            }
            quickAccessHooked = true;
            quickAccess.enabled().subscribe(value -> updateHud());
        } catch (Exception ignored) {
        }
    }

    private void ensureHud() {
        if (hudView != null) {
            return;
        }
        hudView = new GodModeHudView(this);
        Element el = hudView.element();
        el.name = "god-mode-hud";
        el.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null && !isPopupActive()) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void removeHud() {
        if (hudView == null) {
            return;
        }
        GodModeHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new GodModeSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
