package mindustrytool.features.chat;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import arc.Events;
import mindustrytool.Main;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.chat.dto.ChatMessage;
import mindustrytool.features.chat.dto.ChatUser;
import mindustrytool.services.UserService;
import mindustrytool.ui.NetworkImage;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.input.KeyCode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import mindustrytool.features.chat.dto.ChatUser.SimpleRole;

public class ChatOverlay extends Table {
    private Seq<ChatMessage> messages = new Seq<>();
    private Table messageTable;
    private Table userListTable;
    private ScrollPane scrollPane;
    private Table loadingTable;
    private TextField inputField;
    private TextButton sendButton;
    private boolean isSending = false;

    private boolean isCollapsed = true;
    private String lastInputText = "";
    private Table container;
    private Cell<Table> containerCell;
    private final ChatConfig config = new ChatConfig();

    private int unreadCount = 0;
    private Table badgeTable;

    private boolean isUserListCollapsed;

    public ChatOverlay() {
        touchable = Touchable.childrenOnly;

        isCollapsed = config.collapsed();
        isUserListCollapsed = Vars.mobile;
        setPosition(config.x(), config.y());

        container = new Table();

        containerCell = add(container);

        setup();

        Events.on(EventType.ResizeEvent.class, e -> {
            setup();
            keepInScreen();
        });

        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                if (keycode == KeyCode.escape && !isCollapsed) {
                    collapse();
                    return true;
                }
                return false;
            }
        });
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    private void setup() {
        setPosition(config.x(isCollapsed), config.y(isCollapsed));

        container.clearChildren();
        container.touchable = Touchable.enabled;

        if (isCollapsed) {
            container.background(null);
            containerCell.size(48);

            Table buttonTable = new Table();
            buttonTable.background(Styles.black6);

            Button btn = new Button(Styles.clearNoneTogglei);
            Stack stack = new Stack();
            stack.add(new Image(Icon.chat));

            badgeTable = new Table();
            stack.add(badgeTable);
            updateBadge();

            btn.add(stack);
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
                    if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f)
                        wasDragged = true;
                    ChatOverlay.this.moveBy(dx, dy);
                    config.x(ChatOverlay.this.x);
                    config.y(ChatOverlay.this.y);

                    keepInScreen();
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    if (!wasDragged) {
                        isCollapsed = false;
                        config.collapsed(false);
                        unreadCount = 0;
                        setup();
                    }
                }
            });

            buttonTable.add(btn).grow();
            container.add(buttonTable).grow();
        } else {
            container.background(Styles.black8);
            float width = Core.graphics.getWidth() / Scl.scl() * 0.7f;
            float height = Core.graphics.getHeight() / Scl.scl() * 0.7f;

            containerCell.size(Math.min(width, 1400f), Math.min(height, 900f));

            // Header
            Table header = new Table();
            header.background(Styles.black6);
            header.touchable(() -> Touchable.enabled);

            // Header Drag Listener
            header.addListener(new InputListener() {
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
                    keepInScreen();
                    config.x(ChatOverlay.this.x);
                    config.y(ChatOverlay.this.y);
                }
            });

            header.image(Icon.move).color(Color.gray).size(24).padLeft(8);
            header.add("Global Chat").style(Styles.outlineLabel).padLeft(8).growX().left();

            // Minimize button
            header.button(Icon.down, Styles.clearNonei, this::collapse).size(40).padRight(4);

            container.add(header).growX().height(46).row();

            // Main Content Area
            Table mainContent = new Table();

            // Messages
            messageTable = new Table();
            messageTable.top().left();

            scrollPane = new ScrollPane(messageTable, Styles.noBarPane);
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setFadeScrollBars(false);

            loadingTable = new Table();

            loadingTable.add("Loading...").style(Styles.defaultLabel).color(Color.gray);

            Stack stack = new Stack();
            stack.add(loadingTable);
            stack.add(scrollPane);

            mainContent.add(stack).grow();

            ChatService.getInstance().setConnectionListener(connected -> {
                if (loadingTable != null) {
                    loadingTable.visible = !connected;
                }

                if (scrollPane != null) {
                    scrollPane.visible = connected;
                }
            });

            // Vertical Separator
            mainContent.image(Tex.whiteui).width(1f).color(Color.darkGray).fillY();

            // User List Sidebar
            Table rightSide = new Table();
            rightSide.background(Styles.black3);

            Table titleTable = new Table();
            if (!isUserListCollapsed) {
                titleTable.add("ONLINE MEMBERS").style(Styles.defaultLabel).color(Color.gray).pad(10).left().growX();
            }

            titleTable.button(isUserListCollapsed ? Icon.left : Icon.right, Styles.clearNonei, () -> {
                isUserListCollapsed = !isUserListCollapsed;
                setup();
            }).size(40).pad(4).right();

            rightSide.add(titleTable).growX().row();

            if (!isUserListCollapsed) {
                userListTable = new Table();
                userListTable.top().left();

                ScrollPane userScrollPane = new ScrollPane(userListTable,
                        Styles.noBarPane);
                userScrollPane.setScrollingDisabled(true, false);

                rightSide.add(userScrollPane).grow().row();
            }

            mainContent.add(rightSide).width(isUserListCollapsed ? 50f : 280f).growY();

            container.add(mainContent).grow().row();

            // Fetch users
            Timer.schedule(() -> {
                ChatService.getInstance().getChatUsers(this::rebuildUserList, e -> Log.err("Failed to fetch users", e));
            }, 0.25f);

            // Input Area
            Table inputTable = new Table();
            inputTable.background(Styles.black6);

            if (inputField == null) {
                inputField = new TextField();
                inputField.setMessageText("Enter message...");
                inputField.setValidator(this::isValidInput);
                inputField.keyDown(arc.input.KeyCode.enter, () -> {
                    boolean isSchematic = isSchematic(inputField.getText());

                    if (isSchematic) {
                        sendSchematic();
                    } else if (inputField.isValid()) {
                        sendMessage();
                    }
                });
                inputField.keyDown(arc.input.KeyCode.escape, this::collapse);
            }

            inputField.setText(lastInputText);

            sendButton = new TextButton("Send", Styles.defaultt);
            sendButton.clicked(this::sendMessage);
            sendButton.setDisabled(() -> !AuthService.getInstance().isLoggedIn() || isSending);

            inputTable.add(inputField).growX().height(40f).pad(8).padRight(4);

            var mod = Vars.mods.getMod(Main.class);

            var texture = new TextureRegion(new Texture(mod.root.child("icons").child("attach-file.png")));
            TextureRegionDrawable drawable = new TextureRegionDrawable(texture);

            inputTable.button(drawable, () -> {
                Vars.ui.showInfoFade("This do nothing :v");
            }).pad(8);

            inputTable.add(sendButton).width(100f).height(40f).pad(8).padLeft(0);

            container.add(inputTable).growX().bottom();

            // Initial population
            rebuildMessages(messageTable);

            // Scroll to bottom after layout
            Core.app.post(() -> {
                if (scrollPane != null) {
                    scrollPane.setScrollY(scrollPane.getMaxY());
                }
            });

            if (inputField != null && !isCollapsed) {
                Core.scene.setKeyboardFocus(inputField);
            }
        }

        pack();

        keepInScreen();

    }

    private boolean isSchematic(String text) {
        if (!text.startsWith(Vars.schematicBaseStart)) {
            return false;
        }

        try {
            Schematics.readBase64(text);
            return true;
        } catch (Exception _e) {
            return false;
        }
    }

    private boolean isValidInput(String text) {
        if (text.length() <= 0) {
            return false;
        }

        if (text.startsWith(Vars.schematicBaseStart)) {
            if (text.length() > 2056 * 12) {
                return false;
            }
        } else {
            if (text.length() > 2056) {
                return false;
            }
        }

        return true;
    }

    public void keepInScreen() {
        if (getScene() == null)
            return;

        float w = getWidth();
        float h = getHeight();
        float sw = getScene().getWidth();
        float sh = getScene().getHeight();

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (x + w > sw) {
            x = sw - w;
        }

        if (y + h > sh) {
            y = sh - h;
        }
    }

    public void addMessages(ChatMessage[] newMessages) {
        int addedCount = 0;

        for (ChatMessage msg : newMessages) {
            if (messages.contains(m -> m.id.equals(msg.id))) {
                continue;
            }

            messages.add(msg);

            try {
                if (isCollapsed && config.lastRead().isBefore(Instant.parse(msg.createdAt))) {
                    addedCount++;
                }
            } catch (Exception e) {
                Log.err(e);
            }
        }

        if (isCollapsed && addedCount > 0) {
            unreadCount += addedCount;
            updateBadge();
        }

        if (messageTable != null && !isCollapsed) {
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

        config.lastRead(Instant.now());

        for (ChatMessage msg : messages) {
            Table entry = new Table();
            entry.setBackground(null); // Clear background

            // Avatar Column
            entry.table(avatar -> {
                avatar.top();
                UserService.findUserById(msg.createdBy, data -> {
                    avatar.clear();
                    if (data.imageUrl() != null && !data.imageUrl().isEmpty()) {
                        avatar.add(new NetworkImage(data.imageUrl())).size(40);
                    }
                });
            }).size(48).top().pad(8);

            // Content Column
            entry.table(card -> {
                card.top().left();
                Label label = new Label("...");
                label.setStyle(Styles.defaultLabel);

                UserService.findUserById(msg.createdBy, data -> {
                    String timeStr = "";

                    if (msg.createdAt != null) {
                        try {
                            Instant instant = Instant.parse(msg.createdAt);
                            timeStr = DateTimeFormatter.ofPattern("HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(instant);

                        } catch (Exception err) {
                            Log.err(err);
                        }
                    }

                    Color color = data.getHighestRole()
                            .map(r -> {
                                try {
                                    return Color.valueOf(r.color());
                                } catch (Exception err) {
                                    Log.err(err);
                                    return Color.white;
                                }
                            })
                            .orElse(Color.white);

                    label.setText("[#" + color.toString() + "]" + data.name() + "[]"
                            + (timeStr.isEmpty() ? "" : " [gray]" + timeStr));
                });

                card.add(label).left().row();
                card.table(c -> {
                    String content = msg.content.trim();
                    int schematicBasePosition = content.indexOf(Vars.schematicBaseStart);

                    if (schematicBasePosition != -1) {
                        int endPosition = content.indexOf(" ", schematicBasePosition) + 1;

                        if (endPosition == 0) {
                            endPosition = content.length();
                        }

                        String prev = content.substring(0, schematicBasePosition);

                        c.add(prev).wrap().color(Color.lightGray).left().padTop(2);
                        String schematicBase64 = content.substring(schematicBasePosition, endPosition);

                        try {
                            var schematic = Schematics.readBase64(schematicBase64);
                            c.row();
                            renderSchematic(card, schematic);
                            c.row();
                            String after = content.substring(endPosition);
                            c.add(after).wrap().color(Color.lightGray).left().growX().padTop(2);
                        } catch (Exception e) {
                            c.clear();
                            c.add(content).wrap().color(Color.lightGray).left().padTop(2);
                        }
                    } else {
                        c.add(content).wrap().color(Color.lightGray).left().growX().padTop(2);
                    }
                })
                        .top().left();

                card.clicked(() -> {
                    Core.app.setClipboardText(msg.content);
                    Vars.ui.showInfoFade("@copied");
                });
            }).growX().pad(8).top();

            messageTable.add(entry).growX().padBottom(4).row();
        }
    }

    private void rebuildUserList(ChatUser[] users) {
        if (userListTable == null) {
            return;
        }

        userListTable.clear();
        userListTable.top().left();

        Arrays.sort(users, (u1, u2) -> {
            int l1 = u1.getHighestRole().map(SimpleRole::level).orElse(0);
            int l2 = u2.getHighestRole().map(SimpleRole::level).orElse(0);
            return Integer.compare(l2, l1);
        });

        for (ChatUser user : users) {
            Table card = new Table();

            // Avatar
            if (user.imageUrl() != null && !user.imageUrl().isEmpty()) {
                card.add(new NetworkImage(user.imageUrl())).size(40).padRight(8);
            }

            // Info Table
            card.table(info -> {
                info.left();
                info.add(user.name() + "[]").ellipsis(true).maxWidth(200).style(Styles.defaultLabel).color(Color.white)
                        .left().row();

                user.getHighestRole().ifPresent(role -> {
                    info.add(role.id()).style(Styles.defaultLabel).color(Color.valueOf(role.color()))
                            .left().row();
                });
            }).growX().left();

            userListTable.add(card).growX().padBottom(8).padLeft(8).padRight(8).row();
        }
    }

    private void collapse() {
        isCollapsed = true;
        config.collapsed(true);
        unreadCount = 0;
        if (inputField != null) {
            lastInputText = inputField.getText();
        }
        setup();
    }

    private void sendMessage() {
        String content = inputField.getText();

        if (content == null || content.trim().isEmpty()) {
            Vars.ui.showInfoFade("Content is empty");
            return;
        }

        if (!AuthService.getInstance().isLoggedIn()) {
            Vars.ui.showInfoFade("You're not logged in");
            return;
        }

        if (isSending) {
            Vars.ui.showInfoFade("Last message still sending");
            return;
        }

        isSending = true;

        ChatService.getInstance().sendMessage(content, () -> {
            Core.app.post(() -> {
                isSending = false;
                inputField.setText("");
                lastInputText = "";
            });
        }, this::handleSendError);
    }

    private void sendSchematic() {
        String content = inputField.getText();

        if (isSending) {
            Vars.ui.showInfoFade("Last message still sending");
            return;
        }

        isSending = true;

        ChatService.getInstance().sendSchematic(content, () -> {
            Core.app.post(() -> {
                isSending = false;
                inputField.setText("");
                lastInputText = "";
            });
        }, this::handleSendError);
    }

    private void handleSendError(Throwable err) {
        isSending = false;

        String errStr = err.toString();
        if (errStr.contains("409") || err.getMessage().contains("409")) {
            Vars.ui.showInfoToast("Rate limited! Please wait.", 3f);
        } else {
            Vars.ui.showInfoToast("Failed to send message.", 3f);
            Log.err("Send message failed", err);
        }
    }

    private void updateBadge() {
        if (badgeTable == null)
            return;

        badgeTable.clear();
        badgeTable.visible = unreadCount > 0;
        badgeTable.top().right();

        if (unreadCount > 0) {
            Table badge = new Table();
            badge.background(Tex.whiteui);
            badge.setColor(Color.red);

            Label label = new Label(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            label.setColor(Color.white);
            label.setFontScale(0.6f);

            badge.add(label).padLeft(2).padRight(2);

            badgeTable.add(badge).height(16).minWidth(16);
        }
    }

    private void renderSchematic(Table table, Schematic schematic) {
        Button[] sel = { null };
        sel[0] = table.button(b -> {
            b.top();
            b.margin(0f);
            b.table(buttons -> {
                buttons.left();
                buttons.defaults().size(50f);

                ImageButtonStyle style = Styles.emptyi;

                buttons.button(Icon.info, style, () -> Vars.ui.schematics.showInfo(schematic)).tooltip("@info.title")
                        .growX();
                buttons.button(Icon.upload, style, () -> Vars.ui.schematics
                        .showExport(schematic)).tooltip("@editor.export")
                        .growX();
                buttons.button(Icon.pencil, style, () -> Vars.ui.schematics
                        .showEdit(schematic)).tooltip("@schematic.edit").growX();

            }).growX().height(50f);
            b.row();
            b.stack(new SchematicImage(schematic).setScaling(Scaling.fit), new Table(n -> {
                n.top();
                n.table(Styles.black3, c -> {
                    Label label = c.add(schematic.name()).style(Styles.outlineLabel).color(Color.white).top().growX()
                            .maxWidth(200f - 8f).get();
                    label.setEllipsis(true);
                    label.setAlignment(Align.center);
                }).growX().margin(1).pad(4).maxWidth(Scl.scl(200f - 8f)).padBottom(0);
            })).size(200f);
        }, () -> {
            if (sel[0].childrenPressed())
                return;
            if (Vars.state.isMenu()) {
                Vars.ui.schematics.showInfo(schematic);
            } else {
                if (!Vars.state.rules.schematicsAllowed) {
                    Vars.ui.showInfo("@schematic.disabled");
                } else {
                    Vars.control.input.useSchematic(schematic);
                }
            }
        }).top().left().pad(4).style(Styles.flati).get();

        sel[0].getStyle().up = Tex.pane;
    }
}
