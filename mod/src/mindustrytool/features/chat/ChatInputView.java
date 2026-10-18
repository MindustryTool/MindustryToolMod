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
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserData;
import mindustrytool.services.auth.AuthOverlay;
import mindustrytool.services.auth.MindustryAuthProvider;
import solim.core.Component;
import solim.input.SolimTextField;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;
import mindustrytool.components.FileIcon;

public class ChatInputView extends Component {

    private final ChatStore store;
    private final ChatService service;
    private final Signal<String> messageText = Signal.of("");
    private final Signal<Boolean> isSending = Signal.of(false);

    private SolimTextField chatInput;

    public ChatInputView(ChatStore store, ChatService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    protected Element build() {
        Readable<Boolean> isLoggedIn = store.session().loggedIn();
        Readable<Boolean> isNotLoggedIn = isLoggedIn.map(l -> !Boolean.TRUE.equals(l));
        Readable<Boolean> canSend = new Computed<>(() -> !Boolean.TRUE.equals(isSending.get())
                && isValidInput(messageText.get()));

        effect(() -> {
            ChatMessage target = store.ui().replyTarget().get();
            if (target != null && chatInput != null) {
                chatInput.focus();
            }
        });

        return column().props(growX(), padding(unit(2)), gap(unit(1))).children(() -> {

            when(isNotLoggedIn)
                    .thenDo(() -> row().props(growX(), padding(unit(1))).children(() -> {
                        button(Core.bundle.get("auth.login", "Login"), () -> AuthOverlay.getInstance().startLoginUI())
                                .props(style(Styles.defaultt), growX(), height(unit(8)), prefHeight(unit(8)));
                    }))
                    .elseDo(() -> {

                        column().props(growX(), gap(unit(1))).children(() -> {
                            dynamic(store.ui().replyTarget(), target -> {
                                if (target != null) {
                                    String authorId = target.getCreatedBy();
                                    UserData cachedUser = authorId != null ? store.users().getDirect(authorId) : null;
                                    String targetName = (cachedUser != null && cachedUser.getName() != null)
                                            ? cachedUser.getName()
                                            : (authorId != null ? authorId : "message");

                                    row().props(growX(), padding(unit(1)), height(unit(12)), prefHeight(unit(12)), gap(unit(1))).children(() -> {
                                        text(Core.bundle.format("feature.chat.ui.replying", targetName))
                                                .props(color(Color.lightGray), fontScale(0.85f), left());
                                        spacer();
                                        button(() -> store.ui().setReplyTarget(null))
                                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)))
                                                .children(() -> icon(Icon.cancel).props(size(unit(4), unit(4)), prefWidth(unit(4)), prefHeight(unit(4))));
                                    });
                                }
                            }).props(growX());

                            row().props(growX(), gap(unit(1))).children(() -> {
                                card(Styles.black5).props(growX()).children(() -> {
                                    row().props(growX(), gap(unit(1)),
                                            paddingLeft(unit(3)), paddingRight(unit(2)), paddingTop(unit(1)), paddingBottom(unit(1)), rounded(unit(3), Color.clear), border(1.5f, Color.darkGray),
                                            center())
                                            .children(() -> {
                                                chatInput = textField(messageText)
                                                        .validator(this::isValidInput)
                                                        .onEnter(this::onSend)
                                                        .props(
                                                                style(WebStyles.clearInput()),
                                                                growX(),
                                                                placeholder(
                                                                        Core.bundle.get("feature.chat.ui.placeholder",
                                                                                "Message...")),
                                                                disabled(isSending)
                                                        );

                                                button(() -> new AttachContentDialog(this::handleAttachContent).show())
                                                        .props(style(WebStyles.ghost()), size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)))
                                                        .children(
                                                                () -> image(FileIcon.of("upload.png")).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                                                button(this::onSend)
                                                        .props(style(WebStyles.ghost()), enabled(canSend), size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)))
                                                        .children(() -> image(FileIcon.of("send.png"))
                                                                .props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(Pal.accent)));
                                            });
                                });
                            });
                        });
                    }).props(grow());
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

        ChatMessage replyTarget = store.ui().currentReplyTarget();
        String replyToId = replyTarget != null ? replyTarget.getId() : null;

        String activeChannelId = store.channels().currentActiveId();
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

        store.delivery().markPending(tempId);
        store.messages().append(tempMsg);

        isSending.set(true);
        service.sendMessage(content, replyToId).whenComplete((msg, err) -> {
            Core.app.post(() -> {
                isSending.set(false);
                if (err != null) {
                    store.delivery().markFailed(tempId);
                    handleSendError(err);
                } else {
                    store.delivery().clear(tempId);
                    store.messages().confirm(tempId, msg);
                    store.ui().setReplyTarget(null);
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
