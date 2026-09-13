package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.util.Nullable;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.services.MindustryTool;
import solim.core.Component;
import solim.overlay.Popup;

/**
 * Shared floating message-action menu (Copy, Reply, Translate) hosted once in
 * the chat overlay and driven by the native {@link Popup}. Message targeting
 * stays here; all floating-menu machinery (positioning, dismissal, listener
 * lifecycle) lives in Solim.
 */
public final class ChatActionPopup {

    /** The single shared menu instance, installed by the chat overlay. */
    public static @Nullable Popup<ChatMessage> menu;

    private ChatActionPopup() {
    }

    /** Hosts the shared menu. Call once from the overlay build. */
    public static void install(ChatStore store) {
        menu = popup();
        menu.children(request -> menuRows(store, request)).rounded(2);
    }

    /** Opens the shared menu for the given message anchored near the given stage coordinates. */
    public static void showFor(@Nullable ChatMessage message, float stageX, float stageY) {
        if (menu != null) {
            menu.show(message, stageX, stageY);
        }
    }

    /** Dismisses the shared menu if it is open. */
    public static void dismiss() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component menuRows(ChatStore store, ChatMessage message) {
        return column().growX().padding(unit(1)).gap(unit(1)).children(() -> {
            button(Core.bundle.get("feature.chat.ui.copy", "Copy"), () -> copyMessage(store, message))
                    .style(WebStyles.secondaryText())
                    .growX()
                    .height(unit(10));

            button(Core.bundle.get("feature.chat.ui.reply", "Reply"), () -> {
                store.setReplyTarget(message);
                dismiss();
            }).style(WebStyles.secondaryText()).growX().height(unit(10));

            button(Core.bundle.get("feature.chat.ui.translate", "Translate"), () -> translateMessage(store, message))
                    .style(WebStyles.secondaryText())
                    .growX()
                    .height(unit(10));
        });
    }

    private static void copyMessage(ChatStore store, @Nullable ChatMessage message) {
        try {
            String content = message != null && message.getContent() != null ? message.getContent() : "";
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied to clipboard!"));
        } catch (Throwable ignored) {
        }
        dismiss();
    }

    private static void translateMessage(ChatStore store, @Nullable ChatMessage message) {
        if (message == null) {
            dismiss();
            return;
        }
        String messageId = message.getId();
        String alreadyTranslating = store.translatingMessageId().peek();
        if (messageId != null && messageId.equals(alreadyTranslating)) {
            dismiss();
            return;
        }

        String targetLocale = "en";
        if (Vars.ui != null && Vars.ui.language != null && Vars.ui.language.getLocale() != null) {
            targetLocale = Vars.ui.language.getLocale().getLanguage();
        }

        store.translatingMessageId().set(messageId);
        dismiss();

        TranslationFeature tf = FeatureManager.getFeature(TranslationFeature.class);
        CompletableFuture<String> future;
        if (tf != null && tf.isEnabled() && tf.getActiveProvider().isConfigured()) {
            future = tf.translate(message.getContent(), tf.getTargetLanguage());
        } else {
            future = MindustryTool.translate(message.getContent(), targetLocale);
        }

        future.whenComplete((res, err) -> {
            Core.app.post(() -> {
                store.translatingMessageId().set(null);
                if (err != null || res == null) {
                    Vars.ui.showInfoToast(Core.bundle.get("feature.chat.ui.translate-failed", "Translation failed"), 2f);
                } else {
                    ChatMessageHeightCalculator.clearCache();
                    store.setTranslation(messageId, res);
                }
            });
        });
    }
}
