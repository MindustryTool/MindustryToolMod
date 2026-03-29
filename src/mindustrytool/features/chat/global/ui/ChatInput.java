package mindustrytool.features.chat.global.ui;

import java.util.concurrent.CompletableFuture;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematics;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.auth.dto.SessionLoadEvent;
import mindustrytool.features.chat.global.AttachContentDialog;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatService;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.ContentType;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.services.State;
import mindustrytool.services.UserService;

public class ChatInput extends Table {

    private final TextField inputField;
    private final TextButton sendButton;
    private AttachContentDialog attachContentDialog;
    private final State<Boolean> isSending = new State<>(false);
    private String replyingToMessageId = null;

    public ChatInput() {
        inputField = new TextField();
        inputField.setMessageText("@chat.enter-message");
        inputField.setValidator(this::isValidInput);
        inputField.keyDown(arc.input.KeyCode.enter, this::handleSend);

        sendButton = new TextButton("@chat.send", Styles.defaultt);
        sendButton.clicked(this::handleSend);

        Events.on(SessionLoadEvent.class, e -> {
            if (!e.isLoading) {
                rebuild();
            }
        });

        isSending.subscribe((curr, old) -> {
            inputField.setDisabled(curr);
            rebuild();
        });

        rebuild();
    }

    public void setReplyingTo(String messageId) {
        this.replyingToMessageId = messageId;
        rebuild();
    }

    public void clearReply() {
        this.replyingToMessageId = null;
        rebuild();
    }

    public TextField getInputField() {
        return inputField;
    }

    public void rebuild() {
        clear();
        background(Styles.black6);

        float scale = ChatConfig.scale();

        if (AuthService.getInstance().isLoggedIn()) {
            String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
            if (replyingToMessageId != null && currentChannelId != null) {
                ChatMessage repliedMsg = ChatStore.getInstance().getMessages(currentChannelId)
                        .find(m -> m.id.equals(replyingToMessageId));
                if (repliedMsg != null) {
                    Table replyContainer = new Table();
                    replyContainer.background(Styles.black5);
                    replyContainer.left();
                    replyContainer.add(new Image(Icon.upSmall)).size(16 * scale).padRight(4 * scale).color(Color.gray);

                    UserService.findUserById(repliedMsg.createdBy).thenAccept(replyData -> {
                        Core.app.post(() -> {
                            Label replyUser = new Label(replyData.getName());
                            replyUser.setFontScale(scale * 0.8f);
                            replyUser.setColor(Color.gray);
                            replyContainer.add(replyUser).padRight(4 * scale);

                            Label replyContent = new Label(repliedMsg.content.replace('\n', ' '));
                            replyContent.setFontScale(scale * 0.8f);
                            replyContent.setColor(Color.gray);
                            replyContent.setEllipsis(true);
                            replyContainer.add(replyContent).minWidth(0).maxWidth(200 * scale).padRight(8 * scale);

                            replyContainer.button(Icon.cancel, Styles.clearNonei, this::clearReply).size(20 * scale);
                        });
                    });
                    add(replyContainer).growX().pad(4 * scale).row();
                }
            }

            Table inputRow = new Table();
            sendButton.setText(isSending.get() ? "@sending" : "@chat.send");
            sendButton.setDisabled(() -> isSending.get());

            inputRow.add(inputField).growX().minWidth(0).height(40f * scale).pad(8 * scale).padRight(4 * scale);

            inputRow.button(Utils.icons("attach-file.png"), () -> {
                if (attachContentDialog == null) {
                    attachContentDialog = new AttachContentDialog(this::handleAttachContent);
                }
                attachContentDialog.show();
            }).pad(8 * scale).size(40f * scale);

            inputRow.add(sendButton).width(100f * scale).height(40f * scale).pad(8 * scale).padLeft(0);
            add(inputRow).growX().minWidth(0);
        } else {
            button("@login", Styles.defaultt, () -> {
                AuthService.getInstance().login();
            }).growX().height(40f * scale).pad(8 * scale);
        }
    }

    private void handleSend() {
        if (isSending.get() || !AuthService.getInstance().isLoggedIn())
            return;

        String content = inputField.getText();
        if (content == null || content.trim().isEmpty()) {
            Vars.ui.showInfoFade("@chat.empty-content");
            return;
        }

        ContentType type = detectContentType(content.trim());
        if (type != ContentType.TEXT || inputField.isValid()) {
            sendContent(content, type, true);
        }
    }

    private void handleAttachContent(String content) {
        if (content == null || content.isEmpty())
            return;

        ContentType type = detectContentType(content.trim());
        sendContent(content, type, false);
    }

    private void sendContent(String content, ContentType type, boolean clearInput) {
        String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
        if (currentChannelId == null)
            return;

        isSending.set(true);
        CompletableFuture<ChatMessage> future = ChatService.getInstance().sendMessage(currentChannelId, content,
                replyingToMessageId, type);

        future.thenAccept(msg -> Core.app.post(() -> {
            isSending.set(false);
            if (clearInput) {
                inputField.setText("");
                clearReply();
            }
        })).exceptionally(err -> {
            Core.app.post(() -> {
                isSending.set(false);
                handleError(err);
            });
            return null;
        });
    }

    private void handleError(Throwable err) {
        String errStr = err.toString();

        if (errStr.contains("409") || err.getMessage().contains("409")) {
            Vars.ui.showInfoToast("@chat.rate-limited", 3f);
        } else {
            Vars.ui.showInfoToast("@chat.send-failed", 3f);
            Log.err("Send message failed", err);
        }
    }

    private ContentType detectContentType(String text) {
        if (isSchematic(text)) {
            return ContentType.SCHEMATIC;
        }

        if (isMap(text)) {
            return ContentType.MAP;
        }

        return ContentType.TEXT;
    }

    private boolean isSchematic(String text) {
        if (!text.startsWith(Vars.schematicBaseStart))
            return false;
        try {
            Schematics.readBase64(text);
            return true;
        } catch (Exception _e) {
            return false;
        }
    }

    private boolean isMap(String text) {
        if (!text.startsWith("TVNB"))
            return false;
        try {
            byte[] bytes = Base64Coder.decode(text.trim());
            return bytes.length > 4 && bytes[0] == 'M' && bytes[1] == 'S' && bytes[2] == 'A' && bytes[3] == 'V';
        } catch (Exception _e) {
            return false;
        }
    }

    private boolean isValidInput(String text) {

        if (text.length() <= 0) {
            return false;
        }

        var contentType = detectContentType(text);

        switch (contentType) {
            case SCHEMATIC:
                return text.length() <= 2056 * 12;

            case MAP:
                return text.length() <= 1024 * 1024 * 5; // 5MB max for maps

            case TEXT:
            default:
                return text.length() <= 2056;
        }
    }
}
