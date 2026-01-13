package mindustrytool.features.chat;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.event.DragListener;
import arc.scene.event.Touchable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.chat.dto.ChatMessage;
import mindustrytool.ui.UserCard;
import arc.scene.event.InputEvent;

public class ChatOverlay extends Table {
    private Seq<ChatMessage> messages = new Seq<>();
    private Table messageTable;
    private ScrollPane scrollPane;
    private TextField inputField;
    private TextButton sendButton;
    private boolean isSending = false;

    private boolean isCollapsed = false;
    private float expandedWidth = 400;
    private float expandedHeight = 300;
    private String lastInputText = "";

    public ChatOverlay() {
        updateSize();
        setup();
    }

    public void updateSize() {
        float sW = Core.graphics.getWidth();
        float sH = Core.graphics.getHeight();

        // Max width 400 or 95% of screen
        expandedWidth = Math.min(400, sW * 0.95f);
        // Max height 300 or 60% of screen
        expandedHeight = Math.min(300, sH * 0.6f);

        if (!isCollapsed) {
            setSize(expandedWidth, expandedHeight);
        }
    }

    private void setup() {
        clear();

        if (isCollapsed) {
            background(null);
            setSize(60, 60);
            touchable = Touchable.enabled;

            Table buttonTable = new Table();
            buttonTable.background(Styles.black6);
            buttonTable.touchable(() -> Touchable.enabled);

            // Drag listener for the collapsed button
            buttonTable.addListener(new DragListener() {
                float startX, startY;

                @Override
                public void dragStart(InputEvent event, float x, float y, int pointer) {
                    startX = x;
                    startY = y;
                }

                @Override
                public void drag(InputEvent event, float x, float y, int pointer) {
                    ChatOverlay.this.moveBy(x - startX, y - startY);
                    clamp();
                }
            });

            buttonTable.button(Icon.chat, Styles.clearNoneTogglei, () -> {
                isCollapsed = false;
                setup();
            }).grow();

            add(buttonTable).size(60, 60);
        } else {
            background(Styles.black6);
            setSize(expandedWidth, expandedHeight);
            touchable = Touchable.enabled;

            // Header
            Table header = new Table();
            header.background(Styles.black8);
            header.touchable(() ->Touchable.enabled);

            // Drag listener for header
            header.addListener(new DragListener() {
                float startX, startY;

                @Override
                public void dragStart(InputEvent event, float x, float y, int pointer) {
                    startX = x;
                    startY = y;
                }

                @Override
                public void drag(InputEvent event, float x, float y, int pointer) {
                    ChatOverlay.this.moveBy(x - startX, y - startY);
                    clamp();
                }
            });

            header.add("Global Chat").style(Styles.outlineLabel).growX().padLeft(8);
            header.button(Icon.down, Styles.clearNonei, () -> {
                isCollapsed = true;
                if (inputField != null) {
                    lastInputText = inputField.getText();
                }
                setup();
            }).size(32);

            add(header).growX().height(32).row();

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
            inputField.setText(lastInputText);
            inputField.setMaxLength(1024);

            // Send on Enter
            inputField.keyDown(arc.input.KeyCode.enter, this::sendMessage);

            sendButton = new TextButton("Send", Styles.defaultt);
            sendButton.clicked(this::sendMessage);
            sendButton.setDisabled(() -> !AuthService.getInstance().isLoggedIn() || isSending);

            inputTable.add(inputField).growX().height(40f).padRight(4);
            inputTable.add(sendButton).width(80f).height(40f);

            add(inputTable).growX().pad(4).bottom();

            // Initial population
            rebuildMessages();

            // Scroll to bottom after layout
            Core.app.post(() -> {
                if (scrollPane != null) {
                    scrollPane.setScrollY(scrollPane.getMaxY());
                }
            });
        }

        clamp();
    }

    private void clamp() {
        float w = getWidth();
        float h = getHeight();

        float sW = Core.graphics.getWidth();
        float sH = Core.graphics.getHeight();

        x = Mathf.clamp(x, 0, sW - w);
        y = Mathf.clamp(y, 0, sH - h);

        setPosition(x, y);
    }

    public void addMessages(ChatMessage[] newMessages) {
        for (ChatMessage msg : newMessages) {
            if (messages.contains(m -> m.id.equals(msg.id)))
                continue;
            messages.add(msg);
        }

        // Limit to last 100 messages
        if (messages.size > 100) {
            messages.removeRange(0, messages.size - 100);
        }

        if (messageTable != null) {
            rebuildMessages();
            // Scroll to bottom
            Core.app.post(() -> {
                if (scrollPane != null)
                    scrollPane.setScrollY(scrollPane.getMaxY());
            });
        }
    }

    private void rebuildMessages() {
        if (messageTable == null)
            return;

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
            lastInputText = "";
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
