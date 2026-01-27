package mindustrytool.features.chat.global;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.event.Touchable;
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
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import arc.Events;
import arc.func.Prov;
import mindustrytool.MdtKeybinds;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.auth.dto.LoginEvent;
import mindustrytool.features.auth.dto.LogoutEvent;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatStateChange;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.services.UserService;
import mindustrytool.ui.NetworkImage;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.input.KeyCode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import mindustrytool.features.chat.global.dto.ChatUser.SimpleRole;
import mindustrytool.features.playerconnect.PlayerConnectLink;
import mindustrytool.features.playerconnect.PlayerConnectRenderer;
import mindustrytool.services.PlayerConnectService;
import mindustrytool.services.State;

public class ChatOverlay extends Table {
    private Seq<ChatMessage> messages = new Seq<>();
    private Table messageTable;
    private Table userListTable;
    private ScrollPane scrollPane;
    private Table loadingTable;
    private Table inputTable;
    private TextField inputField;
    private TextButton sendButton;

    private State<Boolean> isSending = new State<>(false);

    private Table container;

    private Cell<Table> containerCell;
    private final PlayerConnectService playerConnectService = new PlayerConnectService();

    private int unreadCount = 0;
    private Table badgeTable;

    private boolean isUserListCollapsed;
    private Image connectionIndicator;

    public ChatOverlay() {
        name = "mdt-chat-overlay";
        touchable = Touchable.childrenOnly;
        isUserListCollapsed = Vars.mobile;

        setPosition(ChatConfig.x(), ChatConfig.y());

        container = new Table();
        inputTable = new Table();

        containerCell = add(container);

        inputField = new TextField();
        inputField.setMessageText("@chat.enter-message");
        inputField.setValidator(this::isValidInput);
        inputField.keyDown(arc.input.KeyCode.enter, () -> {
            Core.app.post(() -> {
                boolean isSchematic = isSchematic(inputField.getText());

                if (isSchematic) {
                    sendSchematic();
                } else if (inputField.isValid()) {
                    sendMessage();
                }
            });
        });

        inputField.keyDown(arc.input.KeyCode.escape, this::collapse);

        Events.on(EventType.ResizeEvent.class, e -> {
            setup();
            keepInScreen();
        });

        Events.on(LoginEvent.class, e -> Core.app.post(() -> setup()));
        Events.on(LogoutEvent.class, e -> Core.app.post(() -> setup()));

        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();

            if (noInputFocused && Core.input.keyRelease(MdtKeybinds.chatKb)) {
                ChatConfig.collapsed(!ChatConfig.collapsed());
                setup();
            }
        });

        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                try {
                    if (keycode == KeyCode.escape && !ChatConfig.collapsed()) {
                        collapse();
                        return true;
                    }
                } catch (Exception e) {
                    Log.err(e);
                }
                return false;
            }
        });

        AuthService.getInstance().sessionStore.subscribe((value, state, error) -> {
            buildInputTable(inputTable);
        });

        isSending.subscribe((curr, old) -> {
            inputField.setDisabled(curr);
            buildInputTable(inputTable);
        });
        Core.app.post(() -> setup());
    }

    public boolean isCollapsed() {
        return ChatConfig.collapsed();
    }

    private synchronized void setup() {
        setPosition(ChatConfig.x(ChatConfig.collapsed()), ChatConfig.y(ChatConfig.collapsed()));

        container.clearChildren();
        container.touchable = Touchable.enabled;

        if (ChatConfig.collapsed()) {
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
                    try {
                        float dx = x - lastX;
                        float dy = y - lastY;

                        if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f) {
                            wasDragged = true;
                        }

                        ChatOverlay.this.moveBy(dx, dy);
                        ChatConfig.x(ChatOverlay.this.x);
                        ChatConfig.y(ChatOverlay.this.y);

                        keepInScreen();
                    } catch (Exception e) {
                        Log.err(e);
                    }
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    if (wasDragged) {
                        return;
                    }

                    try {
                        ChatConfig.collapsed(false);
                        unreadCount = 0;
                        Core.app.post(() -> setup());
                    } catch (Exception e) {
                        Log.err(e);
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
                    try {
                        ChatOverlay.this.moveBy(x - lastX, y - lastY);
                        keepInScreen();
                        ChatConfig.x(ChatOverlay.this.x);
                        ChatConfig.y(ChatOverlay.this.y);
                    } catch (Exception e) {
                        Log.info(e);
                    }
                }
            });

            header.image(Icon.move).color(Color.gray).size(24).padLeft(8);
            header.add("@chat.global-chat").style(Styles.outlineLabel).padLeft(8);

            connectionIndicator = new Image(Tex.whiteui) {
                @Override
                public void draw() {
                    Draw.color(color);
                    Fill.circle(x + width / 2f, y + height / 2f, Math.min(width, height) / 2f);
                    Draw.reset();
                }
            };
            connectionIndicator.setColor(ChatService.getInstance().isConnected() ? Color.green : Color.yellow);
            header.add(connectionIndicator).size(10).padLeft(8);

            header.add().growX();

            // Minimize button
            header.button(Icon.refresh, Styles.clearNonei, () -> {
                messages.clear();
                ChatService.getInstance().disconnectStream();
                ChatService.getInstance().connectStream();
            }).size(40).padRight(4);
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
            scrollPane.visible(() -> ChatService.getInstance().isConnected());

            loadingTable = new Table();

            loadingTable.add("@loading").style(Styles.defaultLabel).color(Color.gray)
                    .visible(() -> !ChatService.getInstance().isConnected());

            Stack stack = new Stack();
            stack.add(loadingTable);
            stack.add(scrollPane);

            mainContent.add(stack).grow();

            Events.on(ChatStateChange.class, event -> {
                var connected = event.connected;

                if (connectionIndicator != null) {
                    connectionIndicator.setColor(connected ? Color.green : Color.yellow);
                }
            });

            // Vertical Separator
            mainContent.image(Tex.whiteui).width(1f).color(Color.darkGray).fillY();

            // User List Sidebar
            Table rightSide = new Table();
            rightSide.top();
            rightSide.background(Styles.black3);

            Table titleTable = new Table();
            if (!isUserListCollapsed) {
                titleTable.add("@chat.online-members").style(Styles.defaultLabel).color(Color.gray).pad(10).left()
                        .minWidth(0).ellipsis(true).growX();
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
                rightSide.pack();
            }

            mainContent.add(rightSide).width(isUserListCollapsed ? 48f : 280f).growY();

            container.add(mainContent).grow().row();

            // Fetch users
            Timer.schedule(() -> {
                ChatService.getInstance().getChatUsers(this::rebuildUserList, e -> Log.err("Failed to fetch users", e));
            }, 0.25f);

            buildInputTable(inputTable);

            container.add(inputTable).growX().bottom();

            // Initial population
            rebuildMessages(messageTable);

            // Scroll to bottom after layout
            Core.app.post(() -> {
                if (scrollPane != null) {
                    scrollPane.setScrollY(scrollPane.getMaxY());
                }
            });

            Time.runTask(60, () -> {
                if (scrollPane != null) {
                    Core.app.post(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
                }
            });

        }

        pack();

        keepInScreen();

    }

    private void buildInputTable(Table inputTable) {
        inputTable.clear();
        inputTable.background(Styles.black6);

        if (AuthService.getInstance().isLoggedIn()) {
            sendButton = new TextButton(isSending.get() ? "@sending" : "@chat.send", Styles.defaultt);
            sendButton.clicked(this::sendMessage);
            sendButton.setDisabled(() -> isSending.get());

            inputTable.add(inputField).growX().height(40f).pad(8).padRight(4);

            inputTable.button(Utils.icons("attach-file.png"), () -> {
                Vars.ui.showInfoFade("feature-not-implemented");
            }).pad(8);

            inputTable.add(sendButton).width(100f).height(40f).pad(8).padLeft(0);
        } else {
            inputTable.button("@login", Styles.defaultt, () -> {
                AuthService.getInstance().login();
            }).growX().height(40f).pad(8);
        }
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
        if (getScene() == null) {
            return;
        }

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

    public synchronized void addMessages(ChatMessage... newMessages) {
        int addedCount = 0;

        for (ChatMessage msg : newMessages) {
            if (messages.contains(m -> m.id.equals(msg.id))) {
                continue;
            }

            messages.add(msg);

            try {
                if (ChatConfig.collapsed() && ChatConfig.lastRead().isBefore(Instant.parse(msg.createdAt))) {
                    addedCount++;
                }
            } catch (Exception e) {
                Log.err(e);
            }
        }

        if (ChatConfig.collapsed() && addedCount > 0) {
            unreadCount += addedCount;
            updateBadge();
        } else {
            ChatConfig.lastRead(Instant.now());
        }

        if (messages.size > 1000) {
            messages.remove(0);
        }

        if (messageTable != null && !ChatConfig.collapsed()) {
            // Scroll to bottom

            Core.app.post(() -> {
                rebuildMessages(messageTable);
            });

            Time.runTask(60 * 3, () -> {
                if (scrollPane != null) {
                    Core.app.post(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
                }
            });
        }
    }

    private void rebuildMessages(Table messageTable) {
        messageTable.clear();
        messageTable.top().left();

        ChatConfig.lastRead(Instant.now());

        for (ChatMessage msg : messages) {
            Table entry = new Table();
            entry.setBackground(null); // Clear background

            // Avatar Column
            entry.table(avatar -> {
                avatar.top();
                UserService.findUserById(msg.createdBy).thenAccept(data -> {
                    Core.app.post(() -> {
                        avatar.clear();
                        if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
                            avatar.add(new NetworkImage(data.getImageUrl())).size(40);
                        }
                    });
                });
            }).size(48).top().pad(8);

            // Content Column
            entry.table(card -> {
                card.top().left();
                Label label = new Label("...");
                label.setStyle(Styles.defaultLabel);

                UserService.findUserById(msg.createdBy).thenAccept(data -> {
                    Core.app.post(() -> {
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
                                        return Color.valueOf(r.getColor());
                                    } catch (Exception err) {
                                        Log.err(err);
                                        return Color.white;
                                    }
                                })
                                .orElse(Color.white);

                        label.setText("[#" + color.toString() + "]" + data.getName() + "[]"
                                + (timeStr.isEmpty() ? "" : " [gray]" + timeStr));
                    });
                });

                card.add(label).left().row();
                card.table(c -> {
                    String content = msg.content.trim();

                    if (PlayerConnectLink.isValid(content)) {
                        c.add(content).wrap().color(Color.lightGray).left().growX().padTop(2);
                        c.row();
                        renderPlayerConnectRoom(c, content);
                    } else {
                        int schematicBasePosition = content.indexOf(Vars.schematicBaseStart);

                        if (schematicBasePosition != -1) {
                            int endPosition = content.indexOf(" ", schematicBasePosition) + 1;

                            if (endPosition == 0) {
                                endPosition = content.length();
                            }

                            String prev = content.substring(0, schematicBasePosition);

                            c.add(prev).wrap().color(Color.lightGray).left().growX().padTop(2);
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
                                c.add(content).wrap().color(Color.lightGray).left().growX().padTop(2);
                            }
                        } else {
                            c.add(content).wrap().color(Color.lightGray).left().growX().padTop(2);
                        }
                    }
                })
                        .top().left().growX();

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
            int l1 = u1.getHighestRole().map(SimpleRole::getLevel).orElse(0);
            int l2 = u2.getHighestRole().map(SimpleRole::getLevel).orElse(0);

            return Integer.compare(l2, l1);
        });

        for (ChatUser user : users) {
            Table card = new Table();

            // Avatar
            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                card.add(new NetworkImage(user.getImageUrl())).size(40).padRight(8);
            }

            // Info Table
            card.table(info -> {
                info.left();
                info.add(user.getName() + "[]").minWidth(0).ellipsis(true).style(Styles.defaultLabel)
                        .color(Color.white)
                        .left().row();

                user.getHighestRole().ifPresent(role -> {
                    info.add(role.getId()).minWidth(0).ellipsis(true).style(Styles.defaultLabel)
                            .color(Color.valueOf(role.getColor()))
                            .left().row();
                });
            }).growX().left();

            userListTable.add(card).growX().minWidth(0).padBottom(8).padLeft(8).padRight(8).row();
            userListTable.pack();
        }
    }

    private void collapse() {
        ChatConfig.collapsed(true);
        unreadCount = 0;
        setup();
    }

    private void sendMessage() {
        String content = inputField.getText();

        if (content == null || content.trim().isEmpty()) {
            Vars.ui.showInfoFade("@chat.empty-content");
            return;
        }

        send(() -> ChatService.getInstance().sendMessage(content).thenAccept(this::addMessages));
    }

    private void sendSchematic() {
        send(() -> ChatService.getInstance().sendSchematic(inputField.getText()).thenAccept(this::addMessages));
    }

    private <T> void send(Prov<CompletableFuture<T>> prov) {
        if (!AuthService.getInstance().isLoggedIn()) {
            Vars.ui.showInfoFade("@chat.not-logged-in");
            return;
        }

        if (isSending.get()) {
            Vars.ui.showInfoFade("@chat.sending-in-progress");
            return;
        }

        try {
            isSending.set(true);

            if (sendButton != null) {
                Core.app.post(() -> sendButton.setText("@sending"));
            }

            prov.get().thenRun(() -> {
                Core.app.post(() -> {
                    isSending.set(false);
                    inputField.setText("");

                    if (sendButton != null) {
                        sendButton.setText("@chat.send");
                    }
                });
            }).exceptionally((err) -> {
                Core.app.post(() -> {
                    isSending.set(false);

                    String errStr = err.toString();

                    if (errStr.contains("409") || err.getMessage().contains("409")) {
                        Vars.ui.showInfoToast("@chat.rate-limited", 3f);
                    } else {
                        Vars.ui.showInfoToast("@chat.send-failed", 3f);
                        Log.err("Send message failed", err);
                    }

                    if (sendButton != null) {
                        sendButton.setText("@chat.send");
                    }

                });
                return null;
            });

        } catch (Exception _e) {
            isSending.set(false);
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

    private void renderPlayerConnectRoom(Table table, String link) {
        table.table(t -> {
            t.left();
            t.add("@chat.loading-room-info").color(Color.gray);

            playerConnectService.getRoomWithCache(link).thenAccept(room -> {
                Core.app.post(() -> {
                    t.clear();

                    if (room != null) {
                        PlayerConnectRenderer.render(t, room).grow();
                    } else {
                        t.add("@chat.room-not-found").color(Color.gray);
                    }
                });
            });
        }).growX().left().padTop(10);
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
