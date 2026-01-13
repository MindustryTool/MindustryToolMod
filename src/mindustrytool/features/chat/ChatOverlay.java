package mindustrytool.features.chat;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.chat.dto.ChatMessage;
import mindustrytool.ui.UserCard;

public class ChatOverlay extends Table {
    private Seq<ChatMessage> messages = new Seq<>();
    private Table messageTable;
    private ScrollPane scrollPane;
    private TextField inputField;
    private TextButton sendButton;
    private boolean isSending = false;

    public ChatOverlay() {
        setup();
    }

    private void setup() {
        background(Styles.black6);

        // Header
        add("Global Chat").style(Styles.outlineLabel).growX().pad(4).center().row();

        // Message List
        messageTable = new Table();
        messageTable.top().left();

        scrollPane = new ScrollPane(messageTable, Styles.noBarPane);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(true);

        add(scrollPane).grow().pad(4).row();

        // Input Area
        Table inputTable = new Table();
        inputField = new TextField();
        inputField.setMessageText("Enter message...");
        inputField.setMaxLength(1024);

        // Send on Enter
        inputField.keyDown(arc.input.KeyCode.enter, this::sendMessage);

        sendButton = new TextButton("Send", Styles.defaultt);
        sendButton.clicked(this::sendMessage);
        sendButton.setDisabled(() -> !AuthService.getInstance().isLoggedIn() || isSending);

        inputTable.add(inputField).growX().height(40f).padRight(4);
        inputTable.add(sendButton).width(120f).height(40f);

        add(inputTable).growX().pad(4).bottom();

        // Initial population
        rebuildMessages();
    }

    public void addMessages(ChatMessage[] newMessages) {
        for (ChatMessage msg : newMessages) {
            // Check for duplicates if needed, or just append
            // Assuming stream sends new messages or history?
            // Usually SSE sends new events.
            // We might want to limit history size.
            if (messages.contains(m -> m.id.equals(msg.id)))
                continue;
            messages.add(msg);
        }

        // Limit to last 100 messages
        if (messages.size > 100) {
            messages.removeRange(0, messages.size - 100);
        }

        rebuildMessages();

        // Scroll to bottom
        Core.app.post(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
    }

    private void rebuildMessages() {
        messageTable.clear();
        messageTable.top().left();

        for (ChatMessage msg : messages) {
            Table bubble = new Table();
            bubble.background(Styles.grayPanel);
            UserCard.draw(bubble, msg.createdBy);
            bubble.add(": " + msg.content).wrap().color(Color.white).left().growX();

            messageTable.add(bubble).growX().padBottom(4).left().row();
        }
    }

    private void sendMessage() {
        String content = inputField.getText();
        if (content == null || content.trim().isEmpty())
            return;
        if (!AuthService.getInstance().isLoggedIn())
            return;
        if (isSending)
            return;

        isSending = true;
        ChatService.getInstance().sendMessage(content, () -> {
            isSending = false;
            inputField.setText("");
        }, err -> {
            isSending = false;
            // Handle 409
            String errStr = err.toString();
            if (errStr.contains("409") || err.getMessage().contains("409")) {
                Vars.ui.showInfoToast("Rate limited! Please wait.", 3f);
            } else {
                Vars.ui.showInfoToast("Failed to send message.", 3f);
                Log.err("Send message failed", err);
            }
        });
    }
}
