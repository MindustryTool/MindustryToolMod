package mindustrytool.features.chat;

import arc.Core;
import arc.scene.Element;
import solim.overlay.SolimDialog;
import arc.util.Nullable;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

public class ChatFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> widthRatioConfig;
    public final ConfigValue<Float> heightRatioConfig;
    public final ConfigValue<Boolean> collapsedConfig;

    public final ConfigGroup collapsedGroup;
    public final ConfigGroup expandedGroup;

    public final ContextualConfigValue<Float, Boolean> collapsedXConfig;
    public final ContextualConfigValue<Float, Boolean> collapsedYConfig;
    public final ContextualConfigValue<Float, Boolean> expandedXConfig;
    public final ContextualConfigValue<Float, Boolean> expandedYConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final ChatStore store;
    private final ChatService service;

    private @Nullable ChatOverlayHudView hudView;
    private @Nullable ChatSettingsDialog settingsDialog;

    public ChatFeature() {
        super(FeatureMetadata.builder()
                .id("chat")
                .icon(FileIcon.of("message-circle.png"))
                .order(20)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();

        opacityConfig = config.floatValue("opacity", 1.0f);
        widthRatioConfig = config.floatValue("width-ratio", 0.9f);
        heightRatioConfig = config.floatValue("height-ratio", 0.9f);
        collapsedConfig = config.boolValue("collapsed", false);

        collapsedGroup = config.group("collapsed");
        expandedGroup = config.group("expanded");

        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float defColX = sw > 0 ? Math.max(10f, sw - 140f) : 800f;
        float defColY = sh > 0 ? Math.max(10f, sh - 60f) : 500f;
        float defExpX = sw > 0 ? Math.max(20f, (sw - 600f) / 2f) : 40f;
        float defExpY = sh > 0 ? Math.max(20f, (sh - 400f) / 2f) : 60f;

        collapsedXConfig = collapsedGroup.floatValueKeyed("x", Signals.isPortrait(), p -> p ? "portrait" : "landscape",
                defColX);
        collapsedYConfig = collapsedGroup.floatValueKeyed("y", Signals.isPortrait(), p -> p ? "portrait" : "landscape",
                defColY);
        expandedXConfig = expandedGroup.floatValueKeyed("x", Signals.isPortrait(), p -> p ? "portrait" : "landscape",
                defExpX);
        expandedYConfig = expandedGroup.floatValueKeyed("y", Signals.isPortrait(), p -> p ? "portrait" : "landscape",
                defExpY);

        boolean isCol = Boolean.TRUE.equals(collapsedConfig.get());
        Float initX = isCol ? collapsedXConfig.get() : expandedXConfig.get();
        Float initY = isCol ? collapsedYConfig.get() : expandedYConfig.get();

        xSignal = Signal.of(initX != null ? initX : (isCol ? defColX : defExpX));
        ySignal = Signal.of(initY != null ? initY : (isCol ? defColY : defExpY));

        xSignal.subscribe(val -> {
            if (val != null) {
                if (Boolean.TRUE.equals(collapsedConfig.get())) {
                    collapsedXConfig.set(val);
                } else {
                    expandedXConfig.set(val);
                }
            }
        });
        ySignal.subscribe(val -> {
            if (val != null) {
                if (Boolean.TRUE.equals(collapsedConfig.get())) {
                    collapsedYConfig.set(val);
                } else {
                    expandedYConfig.set(val);
                }
            }
        });

        store = new ChatStore();
        service = new ChatService(store, () -> !Boolean.TRUE.equals(collapsedConfig.get()));

        collapsedConfig.signal().subscribe(col -> {
            boolean isCollapsed = Boolean.TRUE.equals(col);
            if (!isCollapsed) {
                String activeId = store.channels().currentActiveId();
                if (activeId != null) {
                    store.unread().markAsRead(activeId);
                }
            }
            Float targetX = isCollapsed ? collapsedXConfig.get() : expandedXConfig.get();
            Float targetY = isCollapsed ? collapsedYConfig.get() : expandedYConfig.get();
            if (targetX != null)
                xSignal.set(targetX);
            if (targetY != null)
                ySignal.set(targetY);
            if (hudView != null) {
                Core.app.post(hudView::keepInScreen);
            }
        });
    }

    public ChatStore getStore() {
        return store;
    }

    public ChatService getService() {
        return service;
    }

    public void resetPosition() {
        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float defColX = sw > 0 ? Math.max(10f, sw - 140f) : 800f;
        float defColY = sh > 0 ? Math.max(10f, sh - 60f) : 500f;
        float defExpX = sw > 0 ? Math.max(20f, (sw - 600f) / 2f) : 40f;
        float defExpY = sh > 0 ? Math.max(20f, (sh - 400f) / 2f) : 60f;

        collapsedXConfig.set(defColX);
        collapsedYConfig.set(defColY);
        expandedXConfig.set(defExpX);
        expandedYConfig.set(defExpY);

        boolean isCol = Boolean.TRUE.equals(collapsedConfig.get());
        xSignal.set(isCol ? defColX : defExpX);
        ySignal.set(isCol ? defColY : defExpY);

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    public void resetAppearance() {
        opacityConfig.reset();
        widthRatioConfig.reset();
        heightRatioConfig.reset();
    }

    @Override
    public void onEnable() {
        service.start();

        if (Core.scene != null) {
            if (hudView != null) {
                hudView.element().remove();
                hudView.dispose();
            }

            hudView = new ChatOverlayHudView(this);
            Element el = hudView.element();
            el.name = "chat-overlay-hud";

            Core.app.post(() -> {
                if (hudView != null && Core.scene != null) {
                    Core.scene.add(el);
                    el.toFront();
                }
            });
        }
    }

    @Override
    public void onDisable() {
        service.stop();

        if (hudView != null) {
            ChatOverlayHudView view = hudView;
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
            settingsDialog = new ChatSettingsDialog(this);
        }
        return settingsDialog;
    }
}
