package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.util.Nullable;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.signal.Signal;

/**
 * Shared floating action menu (Copy, Reply, Translate) that replaces the old
 * modal {@code MessageActionDialog}. A single transparent dialog instance is
 * reused for every message and anchored near the clicked card. Touching
 * outside the menu dismisses it without dimming the game behind it.
 */
public class ChatActionPopup extends BaseComponent {

    /** Shared popup request: target message plus the stage coordinates to anchor near. */
    public static final class PopupRequest {
        public final ChatMessage message;
        public final float stageX;
        public final float stageY;

        public PopupRequest(ChatMessage message, float stageX, float stageY) {
            this.message = message;
            this.stageX = stageX;
            this.stageY = stageY;
        }
    }

    private static final Signal<PopupRequest> sharedRequest = Signal.of(null);

    /** Opens the shared popup for the given message anchored near the given stage coordinates. */
    public static void showFor(@Nullable ChatMessage message, float stageX, float stageY) {
        sharedRequest.set(message != null ? new PopupRequest(message, stageX, stageY) : null);
    }

    /** Dismisses the shared popup if it is open. */
    public static void dismiss() {
        sharedRequest.set(null);
    }

    private final ChatStore store;
    private final @Nullable ChatService service;
    private final SolimDialog dialog;
    private @Nullable ChatMessage current;
    private boolean catcherAttached = false;

    private final InputListener outsideCatcher = new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            Element hit = null;
            try {
                if (Core.scene != null && Core.scene.root != null) {
                    hit = Core.scene.root.hit(event.stageX, event.stageY, true);
                }
            } catch (Throwable ignored) {
            }
            Element menu = dialog.element();
            for (Element e = hit; e != null; e = e.parent) {
                if (e == menu) {
                    return false;
                }
            }
            dismiss();
            return true;
        }
    };

    public ChatActionPopup(ChatStore store, @Nullable ChatService service) {
        this.store = store;
        this.service = service;
        this.dialog = new SolimDialog(Core.bundle.get("feature.chat.ui.actions", "Message Actions"));
        dialog.fillParent(false);
        dialog.addCloseButton();
        dialog.closeOnBack();
        dialog.dialog().setBackground(null);
        dialog.hidden(() -> {
            detachCatcher();
            dismiss();
        });
        dialog.children(() -> {
            column().growX().children(() -> {
                button(Core.bundle.get("feature.chat.ui.copy", "Copy"), this::copyCurrent)
                        .style(Styles.defaultb)
                        .growX()
                        .height(unit(10));

                button(Core.bundle.get("feature.chat.ui.reply", "Reply"), this::replyCurrent)
                        .style(Styles.defaultb)
                        .growX()
                        .height(unit(10));

                button(Core.bundle.get("feature.chat.ui.translate", "Translate"), this::translateCurrent)
                        .style(Styles.defaultb)
                        .growX()
                        .height(unit(10));
            });
        });
    }

    @Override
    protected Element build() {
        effect(() -> {
            PopupRequest request = sharedRequest.get();
            if (request != null) {
                current = request.message;
                showAt(request.stageX, request.stageY);
            } else {
                current = null;
                hideMenu();
            }
        });
        return spacer();
    }

    @Override
    protected void onDispose() {
        hideMenu();
    }

    private void showAt(float stageX, float stageY) {
        if (Core.scene == null) {
            return;
        }
        dialog.dialog().pack();
        float menuWidth = dialog.dialog().getWidth();
        float menuHeight = dialog.dialog().getHeight();
        float stageWidth = Core.scene.getWidth();
        float stageHeight = Core.scene.getHeight();

        float x = stageX >= 0f ? stageX : Math.max(0f, (stageWidth - menuWidth) / 2f);
        float y = stageY >= 0f ? stageY : Math.max(0f, (stageHeight - menuHeight) / 2f);
        if (stageX >= 0f && stageY >= 0f && y + menuHeight > stageHeight) {
            y = Math.max(0f, stageY - menuHeight);
        }
        x = Math.max(0f, Math.min(x, Math.max(0f, stageWidth - menuWidth)));
        y = Math.max(0f, Math.min(y, Math.max(0f, stageHeight - menuHeight)));

        dialog.dialog().setPosition(x, y);
        if (!dialog.isShown()) {
            dialog.show();
        }
        attachCatcher();
    }

    private void hideMenu() {
        detachCatcher();
        if (dialog.isShown()) {
            dialog.hide();
        }
    }

    private void attachCatcher() {
        if (!catcherAttached && Core.scene != null && Core.scene.root != null) {
            catcherAttached = true;
            Core.scene.root.addCaptureListener(outsideCatcher);
        }
    }

    private void detachCatcher() {
        if (catcherAttached) {
            catcherAttached = false;
            try {
                if (Core.scene != null && Core.scene.root != null) {
                    Core.scene.root.removeCaptureListener(outsideCatcher);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void copyCurrent() {
        try {
            String content = current != null && current.getContent() != null ? current.getContent() : "";
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied to clipboard!"));
        } catch (Throwable ignored) {
        }
        dismiss();
    }

    private void replyCurrent() {
        store.setReplyTarget(current);
        dismiss();
    }

    private void translateCurrent() {
        if (current == null) {
            dismiss();
            return;
        }
        String messageId = current.getId();
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
            future = tf.translate(current.getContent(), tf.getTargetLanguage());
        } else {
            future = MindustryTool.translate(current.getContent(), targetLocale);
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
