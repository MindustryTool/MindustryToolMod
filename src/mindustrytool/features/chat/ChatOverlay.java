package mindustrytool.features.chat;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
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
import arc.scene.event.InputListener;
import arc.input.KeyCode;

public class ChatOverlay extends Table {
    private Seq<ChatMessage> messages = new Seq<>();
    private Table messageTable;
    private ScrollPane scrollPane;
    private TextField inputField;
    private TextButton sendButton;
    private boolean isSending = false;

    private boolean isCollapsed = false;
    private String lastInputText = "";
    private Table container;
    private Cell<Table> containerCell;

    public ChatOverlay() {
        touchable = Touchable.childrenOnly;
        container = new Table();

        setPosition(200, 200);

        container.setPosition(0, 0);

        containerCell = add(container);

        setup();
    }

    private void setup() {
        container.clearChildren();
        container.touchable = Touchable.enabled;

        if (isCollapsed) {
            container.background(null);
            containerCell.size(60f);

            Table buttonTable = new Table();
            buttonTable.background(Styles.black6);

            Button btn = new Button(Styles.clearNoneTogglei);
            btn.add(new Image(Icon.chat));
            btn.addListener(new InputListener() {
                float lastX, lastY;
                boolean wasDragged = false;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    lastX = x;
                    lastY = y;
                    wasDragged = false;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    float dx = x - lastX;
                    float dy = y - lastY;
                    // Check if actually dragged to avoid sensitive clicks
                    if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f)
                        wasDragged = true;
                    ChatOverlay.this.moveBy(dx, dy);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    if (!wasDragged) {
                        isCollapsed = false;
                        setup();
                    }
                }
            });

            buttonTable.add(btn).grow();
            container.add(buttonTable).grow();
        } else {
            container.background(Styles.black6);
            containerCell.size(450f, 350f);

            // Header
            Table header = new Table();
            header.background(Styles.black8);
            header.touchable(() -> Touchable.enabled);

            // Drag handle
            Image handle = new Image(Icon.move);
            handle.addListener(new InputListener() {
                float lastX, lastY;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    lastX = x;
                    lastY = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    ChatOverlay.this.moveBy(x - lastX, y - lastY);
                }
            });

            header.add(handle).size(32).padLeft(8);
            header.add("Global Chat").style(Styles.outlineLabel).growX().padLeft(8);
            header.button(Icon.down, Styles.clearNonei, () -> {
                isCollapsed = true;
                if (inputField != null) {
                    lastInputText = inputField.getText();
                }
                setup();
            }).size(32);

            container.add(header).growX().height(32).row();

            // Message List
            messageTable = new Table();
            messageTable.top().left();

            scrollPane = new ScrollPane(messageTable, Styles.noBarPane);
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setFadeScrollBars(true);

            container.add(scrollPane).grow().pad(4).row();

            // Input Area
            Table inputTable = new Table();
            inputField = new TextField();
            inputField.setMessageText("Enter message...");
            inputField.setText(lastInputText);
            inputField.setMaxLength(1024);
            inputField.setValidator(text -> text.length() > 0);

            // Send on Enter
            inputField.keyDown(arc.input.KeyCode.enter, this::sendMessage);

            sendButton = new TextButton("Send", Styles.defaultt);
            sendButton.clicked(this::sendMessage);
            sendButton.setDisabled(() -> !AuthService.getInstance().isLoggedIn() || isSending);

            inputTable.add(inputField).growX().height(40f).padRight(4);
            inputTable.add(sendButton).width(80f).height(40f);

            container.add(inputTable).growX().pad(4).bottom();

            // Initial population
            rebuildMessages(messageTable);

            // Scroll to bottom after layout
            Core.app.post(() -> {
                if (scrollPane != null) {
                    scrollPane.setScrollY(scrollPane.getMaxY());
                }
            });
        }

        pack();
        keepInScreen();
    }

    private void keepInScreen() {
        if (getScene() == null)
            return;

        float w = getWidth();
        float h = getHeight();
        float sw = getScene().getWidth();
        float sh = getScene().getHeight();

        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        if (x + w > sw)
            x = sw - w;
        if (y + h > sh)
            y = sh - h;
    }

    public void addMessages(ChatMessage[] newMessages) {
        for (ChatMessage msg : newMessages) {
            if (messages.contains(m -> m.id.equals(msg.id))) {
                continue;
            }
            messages.add(msg);
        }

        // Limit to last 100 messages
        if (messages.size > 100) {
            messages.removeRange(0, messages.size - 100);
        }

        if (messageTable != null) {
            rebuildMessages(messageTable);
            // Scroll to bottom
            Core.app.post(() -> {
                if (scrollPane != null)
                    scrollPane.setScrollY(scrollPane.getMaxY());
            });
        }
    }

    private void rebuildMessages(Table messageTable) {
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
