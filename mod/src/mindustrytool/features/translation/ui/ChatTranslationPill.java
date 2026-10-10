package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.ui.TextField;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Tmp;
import mindustry.Vars;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.translation.TranslationFeature;
import solim.core.BaseComponent;
import solim.overlay.Hud;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Compact in-chat HUD pill attached above Mindustry's chat field.
 * Allows quick 1-tap toggle of outgoing translation and target language switching in-game.
 */
public class ChatTranslationPill extends BaseComponent {

    private final TranslationFeature feature;
    private final Signal<Boolean> isChatShown = signal(false);
    private final Signal<Float> pillX = signal(10f);
    private final Signal<Float> pillY = signal(60f);
    private @Nullable Hud hud;

    public ChatTranslationPill(TranslationFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Boolean> outgoingEnabled = feature.outgoingEnabledConfig.signal();
        Readable<String> targetLangCode = feature.outgoingTargetLangConfig.signal().map(lang -> {
            if ("none".equalsIgnoreCase(lang) || !Boolean.TRUE.equals(feature.outgoingEnabledConfig.get())) {
                return "OFF";
            }
            if (lang == null || lang.trim().isEmpty()) {
                return "EN";
            }
            String clean = lang.trim();
            if (clean.length() <= 3) {
                return clean.toUpperCase();
            }
            return clean.substring(0, Math.min(2, clean.length())).toUpperCase();
        });

        Readable<Color> statusColor = outgoingEnabled.map(enabled ->
                Boolean.TRUE.equals(enabled) ? Color.valueOf("#82c341") : Color.gray);

        Readable<Boolean> shouldShowPill = Signal.computed(() ->
                feature.isEnabled()
                        && Boolean.TRUE.equals(feature.showPillConfig.get())
                        && Boolean.TRUE.equals(isChatShown.get()));

        hud = hud(() -> row().gap(unit(1)).padding(unit(1)).children(() -> {
            card().padding(unit(1), unit(2), unit(1), unit(2)).children(() -> row().gap(unit(1)).children(() -> {
                // Toggle translation on/off button
                button(() -> feature.outgoingEnabledConfig.set(!Boolean.TRUE.equals(feature.outgoingEnabledConfig.get())))
                        .style(WebStyles.ghost())
                        .size(unit(9), unit(8))
                        .tooltip(Core.bundle != null
                                ? Core.bundle.get("feature.translation.outgoing.enable", "Translate outgoing chat messages automatically")
                                : "Translate outgoing chat messages automatically")
                        .children(() -> icon(FileIcon.of("translate.png")).size(unit(5)).color(statusColor));

                // Language selection chip button
                button(targetLangCode, feature::showLanguageDialog)
                        .style(WebStyles.secondaryText())
                        .height(unit(8))
                        .tooltip(Core.bundle != null
                                ? Core.bundle.get("feature.translation.outgoing.select-lang", "Select Language")
                                : "Select Language");
            }));
        }));

        hud.position(pillX, pillY);
        hud.visible(shouldShowPill);
        hud.element().update(this::updatePosition);

        return hud.element();
    }

    private void updatePosition() {
        boolean shown = Vars.ui != null && Vars.ui.chatfrag != null && Vars.ui.chatfrag.shown();
        if (shown != Boolean.TRUE.equals(isChatShown.peek())) {
            isChatShown.set(shown);
        }
        if (!shown) {
            return;
        }

        try {
            TextField chatfield = Reflect.get(Vars.ui.chatfrag, "chatfield");
            if (chatfield != null && chatfield.parent != null) {
                Vec2 stagePos = chatfield.localToStageCoordinates(Tmp.v1.set(0f, 0f));
                float targetX = Math.max(10f, stagePos.x);
                float targetY = stagePos.y + chatfield.getHeight() + 6f;
                if (Math.abs(targetX - pillX.peek()) > 1f || Math.abs(targetY - pillY.peek()) > 1f) {
                    pillX.set(targetX);
                    pillY.set(targetY);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
