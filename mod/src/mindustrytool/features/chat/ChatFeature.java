package mindustrytool.features.chat;

import arc.Core;
import arc.func.Prov;
import arc.scene.Element;
import java.util.Objects;
import solim.overlay.SolimDialog;

import arc.util.Nullable;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

public class ChatFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> widthRatioConfig;
    public final ConfigValue<Float> heightRatioConfig;
    public final ConfigValue<Boolean> collapsedConfig;
    public final ConfigValue<Boolean> sharePresenceConfig;
    public final ConfigValue<Boolean> channelsCollapsedConfig;
    public final ConfigValue<Boolean> usersCollapsedConfig;
    public final ConfigValue<String> activeChannelConfig;

    public final ContextualConfigValue<Float, String> xConfig;
    public final ContextualConfigValue<Float, String> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final ChatStore store;
    private final ChatService service;
    private final ChatPresence presence;

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
        sharePresenceConfig = config.boolValue("share-presence", true);
        channelsCollapsedConfig = config.boolValue("channels-collapsed", false);
        usersCollapsedConfig = config.boolValue("users-collapsed", false);
        activeChannelConfig = config.stringValue("active-channel", "");

        Readable<String> positionContext = Signal.computed(() -> {
            boolean isCol = Boolean.TRUE.equals(collapsedConfig.signal().get());
            boolean isPort = Boolean.TRUE.equals(Signals.isPortrait().get());
            return (isCol ? "collapsed" : "expanded") + "." + (isPort ? "portrait" : "landscape");
        });

        xConfig = config.floatValueKeyed(
                "",
                positionContext,
                ctx -> (ctx.startsWith("collapsed") ? "collapsed" : "expanded") + ".x."
                        + (ctx.endsWith("portrait") ? "portrait" : "landscape"),
                ctx -> {
                    float sw = Units.screenWidth();
                    return ctx.startsWith("collapsed")
                            ? (sw > 0 ? Math.max(10f, sw - 140f) : 800f)
                            : (sw > 0 ? Math.max(20f, (sw - 600f) / 2f) : 40f);
                });

        yConfig = config.floatValueKeyed(
                "",
                positionContext,
                ctx -> (ctx.startsWith("collapsed") ? "collapsed" : "expanded") + ".y."
                        + (ctx.endsWith("portrait") ? "portrait" : "landscape"),
                ctx -> {
                    float sh = Units.screenHeight();
                    return ctx.startsWith("collapsed")
                            ? (sh > 0 ? Math.max(10f, sh - 60f) : 500f)
                            : (sh > 0 ? Math.max(20f, (sh - 400f) / 2f) : 60f);
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        store = new ChatStore(this);
        store.ui().setChannelsCollapsed(Boolean.TRUE.equals(channelsCollapsedConfig.get()));
        store.ui().setUsersCollapsed(Boolean.TRUE.equals(usersCollapsedConfig.get()));

        store.ui().channelsCollapsed().subscribe(col -> {
            if (!Objects.equals(channelsCollapsedConfig.get(), col)) {
                channelsCollapsedConfig.set(col);
            }
        });
        store.ui().usersCollapsed().subscribe(col -> {
            if (!Objects.equals(usersCollapsedConfig.get(), col)) {
                usersCollapsedConfig.set(col);
            }
        });

        channelsCollapsedConfig.signal().subscribe(col -> {
            if (!Objects.equals(store.ui().channelsCollapsed().peek(), col)) {
                store.ui().setChannelsCollapsed(Boolean.TRUE.equals(col));
            }
        });
        usersCollapsedConfig.signal().subscribe(col -> {
            if (!Objects.equals(store.ui().usersCollapsed().peek(), col)) {
                store.ui().setUsersCollapsed(Boolean.TRUE.equals(col));
            }
        });

        service = new ChatService(store, () -> !Boolean.TRUE.equals(collapsedConfig.get()));
        presence = new ChatPresence(store.session(), sharePresenceConfig, enabled());

        collapsedConfig.signal().subscribe(col -> {
            boolean isCollapsed = Boolean.TRUE.equals(col);
            if (!isCollapsed) {
                String activeId = store.channels().currentActiveId();
                if (activeId != null) {
                    store.unread().markAsRead(activeId);
                }
            }
            if (hudView != null) {
                Core.app.post(hudView::keepInScreen);
            }
        });
    }

    public ChatStore getStore() {
        return store;
    }

    public ChatPresence getChatPresence() {
        return presence;
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

        Core.settings.put("mindustrytool.features.chat.collapsed.x.portrait", defColX);
        Core.settings.put("mindustrytool.features.chat.collapsed.x.landscape", defColX);
        Core.settings.put("mindustrytool.features.chat.collapsed.y.portrait", defColY);
        Core.settings.put("mindustrytool.features.chat.collapsed.y.landscape", defColY);

        Core.settings.put("mindustrytool.features.chat.expanded.x.portrait", defExpX);
        Core.settings.put("mindustrytool.features.chat.expanded.x.landscape", defExpX);
        Core.settings.put("mindustrytool.features.chat.expanded.y.portrait", defExpY);
        Core.settings.put("mindustrytool.features.chat.expanded.y.landscape", defExpY);

        xConfig.reset();
        yConfig.reset();

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
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new ChatSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
