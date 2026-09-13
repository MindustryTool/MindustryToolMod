package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.time.Instant;
import java.util.UUID;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserData;
import mindustrytool.services.auth.AuthOverlay;
import mindustrytool.services.auth.MindustryAuthProvider;
import solim.core.BaseComponent;
import solim.signal.Readable;
import solim.signal.Signal;
import mindustrytool.components.FileIcon;

public class ChatInputView extends BaseComponent {

    private final ChatStore store;
    private final ChatService service;
    private final Signal<String> messageText = Signal.of("");
    private final Signal<Boolean> isSending = Signal.of(false);

    public ChatInputView(ChatStore store, ChatService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    protected Element build() {
        Readable<Boolean> isLoggedIn = store.loggedIn().map(l -> Boolean.TRUE.equals(l));
        Readable<Boolean> isNotLoggedIn = isLoggedIn.map(l -> !l);
        Readable<Boolean> canSend = isSending.map(s -> !s);

        return column().growX().gap(unit(1)).padding(unit(2)).children(() -> {
            // Login banner when not logged in
            dynamic(isNotLoggedIn, notLoggedIn -> {
                if (Boolean.TRUE.equals(notLoggedIn)) {
                    return row().growX().padding(unit(1)).children(() -> {
                        button(Core.bundle.get("auth.login", "Login"), () -> AuthOverlay.getInstance().startLoginUI())
                                .style(Styles.defaultt)
                                .growX()
                                .height(unit(8));
                    });
                }

                // Composer area when logged in
                return column().growX().gap(unit(1)).children(() -> {
                    dynamic(store.replyTarget(), target -> {
                        if (target == null) {
                            return null;
                        }
                        String authorId = target.getCreatedBy();
                        UserData cachedUser = (authorId != null && store.userCache().peek() != null)
                                ? store.userCache().peek().get(authorId)
                                : null;
                        String targetName = (cachedUser != null && cachedUser.getName() != null)
                                ? cachedUser.getName()
                                : (authorId != null ? authorId : "message");

                        return row().growX().padding(unit(1)).height(unit(12)).gap(unit(1)).children(() -> {
                            icon(Icon.leftSmall).size(unit(4), unit(4)).color(Pal.accent);
                            text(Core.bundle.format("feature.chat.ui.replying", targetName)).color(Color.lightGray)
                                    .fontScale(0.85f).left();
                            spacer();
                            button(() -> store.setReplyTarget(null))
                                    .style(Styles.clearNonei)
                                    .size(unit(6), unit(6))
                                    .children(() -> icon(Icon.cancel).size(unit(4), unit(4)));
                        });
                    }).growX();

                    row().growX().gap(unit(1)).children(() -> {
                        card(Styles.black5).growX().children(() -> {
                            row().growX().gap(unit(1))
                                    .paddingLeft(unit(2))
                                    .padRight(unit(2))
                                    .padTop(unit(1))
                                    .padBottom(unit(1))
                                    .rounded(unit(3), Color.clear)
                                    .border(1.5f, Color.darkGray)
                                    .children(() -> {
                                        textField(messageText)
                                                .style(WebStyles.clearInput())
                                                .placeholder(
                                                        Core.bundle.get("feature.chat.ui.placeholder", "Message..."))
                                                .validator(this::isValidInput)
                                                .onEnter(this::onSend)
                                                .disabled(isSending)
                                                .growX();

                                        button(() -> new AttachContentDialog(this::handleAttachContent).show())
                                                .style(Styles.cleart)
                                                .size(unit(10))
                                                .children(
                                                        () -> image(FileIcon.of("upload.png")).size(unit(6), unit(6)));

                                        button(this::onSend)
                                                .style(Styles.cleart)
                                                .enabled(canSend)
                                                .size(unit(10))
                                                .children(() -> image(FileIcon.of("send.png")).size(unit(6), unit(6))
                                                        .color(Pal.accent));
                                    });
                        });
                    });
                });
            }).grow();
        }).element();
    }

    private void handleAttachContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        sendDirect(content.trim());
    }

    private void onSend() {
        String content = messageText.peek();
        if (content == null || content.trim().isEmpty()) {
            Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.empty-content", "Message cannot be empty."));
            return;
        }

        if (!isValidInput(content)) {
            return;
        }

        messageText.set("");
        sendDirect(content.trim());
    }

    private void sendDirect(String content) {
        if (Boolean.TRUE.equals(isSending.peek())) {
            return;
        }

        ChatMessage replyTarget = store.replyTarget().peek();
        String replyToId = replyTarget != null ? replyTarget.getId() : null;

        String activeChannelId = store.activeChannelId().peek();
        if (activeChannelId == null || activeChannelId.isEmpty()) {
            return;
        }

        String userId = MindustryAuthProvider.getInstance().session().get() != null
                ? MindustryAuthProvider.getInstance().session().get().getId()
                : null;

        String tempId = "temp_" + UUID.randomUUID();
        ChatMessage tempMsg = new ChatMessage();
        tempMsg.setId(tempId);
        tempMsg.setCreatedBy(userId);
        tempMsg.setCreatedAt(Instant.now().toString());
        tempMsg.setContent(content);
        tempMsg.setReplyTo(replyToId);
        tempMsg.setChannelId(activeChannelId);

        store.addPendingMessage(tempId);
        store.appendMessage(tempMsg, true);

        isSending.set(true);
        service.sendMessage(content, replyToId).whenComplete((msg, err) -> {
            Core.app.post(() -> {
                isSending.set(false);
                if (err != null) {
                    store.removePendingMessage(tempId);
                    store.addFailedMessage(tempId);
                    handleSendError(err);
                } else {
                    store.removePendingMessage(tempId);
                    boolean realExists = store.hasMessage(msg.getId());
                    if (realExists) {
                        store.removeMessage(tempId);
                    } else {
                        store.replaceMessage(tempId, msg);
                    }
                    store.setReplyTarget(null);
                }
            });
        });
    }

    private void handleSendError(Throwable err) {
        String errStr = err.toString();
        if (errStr.contains("409") || (err.getMessage() != null && err.getMessage().contains("409"))) {
            Vars.ui.showInfoToast(Core.bundle.get("feature.chat.ui.rate-limited", "Rate limited. Please wait."), 3f);
        } else {
            Vars.ui.showInfoToast(Core.bundle.get("feature.chat.ui.send-failed", "Message failed to send."), 3f);
        }
    }

    private boolean isValidInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith(Vars.schematicBaseStart)) {
            return trimmed.length() <= 2056 * 12;
        }
        if (trimmed.startsWith("TVNB")) {
            return trimmed.length() <= 1024 * 1024 * 5;
        }
        return trimmed.length() <= 2056;
    }
}
