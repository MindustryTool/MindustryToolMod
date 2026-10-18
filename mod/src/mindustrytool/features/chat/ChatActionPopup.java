package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.style;

import arc.Core;
import arc.util.Nullable;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustrytool.components.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.services.MindustryTool;
import solim.core.Component;
import solim.overlay.Popup;

public final class ChatActionPopup {

    public static @Nullable Popup<ParsedChatMessage> menu;

    private ChatActionPopup() {
    }

    public static void install(ChatStore store) {
        Popup<ParsedChatMessage> p = popup();
        menu = p.props(solim.modifier.Values.rounded(2));
        menu.items(request -> menuRows(store, request));
    }

    public static void showFor(@Nullable ParsedChatMessage message, float stageX, float stageY) {
        if (menu != null) {
            menu.show(message, stageX, stageY);
        }
    }

    public static void dismiss() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component menuRows(ChatStore store, ParsedChatMessage message) {
        return column().props(growX(), padding(unit(1)), gap(unit(1))).children(() -> {
            button(Core.bundle.get("feature.chat.ui.copy", "Copy"), () -> copyMessage(store, message))
                    .props(style(WebStyles.secondaryText()), growX(), height(unit(10)), prefHeight(unit(10)));

            if (message != null && store.translations().getDirect(message.getId()) != null) {
                button(Core.bundle.get("feature.chat.ui.copy-translation", "Copy Translation"), () -> {
                    String tr = store.translations().getDirect(message.getId());
                    if (tr != null) {
                        Core.app.setClipboardText(tr);
                        Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied to clipboard!"));
                    }
                    dismiss();
                }).props(style(WebStyles.secondaryText()), growX(), height(unit(10)), prefHeight(unit(10)));
            }

            button(Core.bundle.get("feature.chat.ui.reply", "Reply"), () -> {
                store.ui().setReplyTarget(message.getRaw());
                dismiss();
            }).props(style(WebStyles.secondaryText()), growX(), height(unit(10)), prefHeight(unit(10)));

            button(Core.bundle.get("feature.chat.ui.translate", "Translate"), () -> translateMessage(store, message))
                    .props(style(WebStyles.secondaryText()), growX(), height(unit(10)), prefHeight(unit(10)));
        });
    }

    private static void copyMessage(ChatStore store, @Nullable ParsedChatMessage message) {
        try {
            String content = message != null && message.getContent() != null ? message.getContent() : "";
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied to clipboard!"));
        } catch (Throwable ignored) {
        }
        dismiss();
    }

    private static void translateMessage(ChatStore store, @Nullable ParsedChatMessage message) {
        if (message == null) {
            dismiss();
            return;
        }
        String messageId = message.getId();
        String content = message.getContent();

        String alreadyTranslating = store.ui().currentTranslatingMessageId();
        if (messageId != null && messageId.equals(alreadyTranslating)) {
            dismiss();
            return;
        }

        String targetLocale = "en";
        if (Vars.ui != null && Vars.ui.language != null && Vars.ui.language.getLocale() != null) {
            targetLocale = Vars.ui.language.getLocale().getLanguage();
        }

        store.ui().setTranslatingMessageId(messageId);
        dismiss();

        TranslationFeature tf = FeatureManager.getFeature(TranslationFeature.class);
        CompletableFuture<String> future;
        if (tf != null && tf.getActiveProvider().isConfigured()) {
            future = tf.translate(content, tf.getTargetLanguage());
        } else {
            future = MindustryTool.translate(content, targetLocale);
        }

        future.whenComplete((res, err) -> {
            Core.app.post(() -> {
                store.ui().setTranslatingMessageId(null);
                if (err != null || res == null) {
                    Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.translate-failed", "Translation failed"));
                } else {
                    ChatMessageHeightCalculator.clearCache();
                    store.translations().set(messageId, res);
                }
            });
        });
    }
}