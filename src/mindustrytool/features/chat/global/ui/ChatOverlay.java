package mindustrytool.features.chat.global.ui;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.MdtKeybinds;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.auth.dto.LoginEvent;
import mindustrytool.features.auth.dto.LogoutEvent;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatFeature;
import mindustrytool.features.chat.global.ChatService;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.events.*;
import mindustrytool.features.chat.global.dto.ChannelDto;

public class ChatOverlay extends Table {
    public enum MobileTab {
        CHANNELS, MESSAGES, MEMBERS
    }

    private MobileTab currentMobileTab = MobileTab.MESSAGES;
    private boolean isUserListCollapsed;

    private Table container;
    private Cell<Table> containerCell;

    private Table badgeTable;
    private Image connectionIndicator;

    private ChannelList channelList;
    private MessageList messageList;
    private UserList userList;
    private ChatInput chatInput;

    public ChatOverlay() {
        name = "mdt-chat-overlay";
        touchable = Touchable.childrenOnly;
        isUserListCollapsed = Vars.mobile;

        setPosition(ChatConfig.x(), ChatConfig.y());

        container = new Table();
        containerCell = add(container);

        visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown);

        Events.on(ResizeEvent.class, e -> {
            setup();
            keepInScreen();
        });

        Events.on(LoginEvent.class, e -> Core.app.post(this::setup));
        Events.on(LogoutEvent.class, e -> Core.app.post(this::setup));

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

        Events.on(ChatStateChange.class, event -> {
            if (connectionIndicator != null) {
                connectionIndicator.setColor(event.connected ? Color.green : Color.yellow);
            }
        });

        ChatStore.getInstance().unreadCountState.subscribe((n, o) -> updateBadge());

        chatInput = new ChatInput();
        chatInput.getInputField().keyDown(KeyCode.escape, this::collapse);

        messageList = new MessageList(chatInput);

        channelList = new ChannelList();
        channelList.onChannelSelect(() -> {
            if (Vars.mobile && currentMobileTab != MobileTab.MESSAGES) {
                currentMobileTab = MobileTab.MESSAGES;
                setup();
            }
        });

        userList = new UserList();

        Core.app.post(this::setup);

        Time.run(60, messageList::scrollToBottom);
    }

    public boolean isCollapsed() {
        return ChatConfig.collapsed();
    }

    public void rebuild() {
        setup();
    }

    private synchronized void setup() {
        setPosition(ChatConfig.x(ChatConfig.collapsed()), ChatConfig.y(ChatConfig.collapsed()));

        container.clearChildren();
        container.touchable = Touchable.enabled;
        container.setColor(1f, 1f, 1f, ChatConfig.opacity());

        float scale = ChatConfig.scale();
        float widthScale = ChatConfig.width();
        float heightScale = ChatConfig.height();

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
                        if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f)
                            wasDragged = true;
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
                    if (wasDragged)
                        return;
                    try {
                        ChatConfig.collapsed(false);
                        ChatStore.getInstance().resetUnreadCount();
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
            float width = Core.graphics.getWidth() / Scl.scl() * widthScale;
            float height = Core.graphics.getHeight() / Scl.scl() * heightScale;

            float actualWidth = Math.min(width, 1900f);
            containerCell.size(actualWidth, Math.min(height, 1300f));

            Table header = buildHeader(scale);
            container.add(header).growX().height(46 * scale).row();

            Table mainContent = buildMainContent(scale, actualWidth);
            container.add(mainContent).grow().minWidth(0).row();

            channelList.rebuild();
            userList.rebuild();
            messageList.rebuild();
            chatInput.rebuild();
        }

        pack();
        keepInScreen();
    }

    private Table buildHeader(float scale) {
        Table header = new Table();
        header.background(Styles.black6);
        header.touchable(() -> Touchable.enabled);

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
                    Log.err(e);
                }
            }
        });

        connectionIndicator = new Image(Tex.whiteui) {
            @Override
            public void draw() {
                Draw.color(color);
                Fill.circle(x + width / 2f, y + height / 2f, Math.min(width, height) / 2f);
                Draw.reset();
            }
        };
        connectionIndicator.setColor(ChatService.getInstance().isConnected() ? Color.green : Color.yellow);

        if (Vars.mobile) {
            if (currentMobileTab == MobileTab.MESSAGES) {
                header.button(Icon.left, Styles.clearNonei, () -> {
                    currentMobileTab = MobileTab.CHANNELS;
                    setup();
                }).size(40 * scale).padLeft(4 * scale);
                header.image(Icon.move).color(Color.gray).size(24 * scale).padLeft(4 * scale);

                String chanName = "@chat.global-chat";
                String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
                if (currentChannelId != null) {
                    ChannelDto c = ChatStore.getInstance().getChannels().find(ch -> ch.id.equals(currentChannelId));
                    if (c != null)
                        chanName = "# " + c.name;
                }
                Label title = header.add(chanName).style(Styles.outlineLabel).padLeft(8 * scale).get();
                title.setFontScale(scale);

                header.add(connectionIndicator).size(10 * scale).padLeft(8 * scale);
                header.add().growX();

                header.button(Icon.right, Styles.clearNonei, () -> {
                    currentMobileTab = MobileTab.MEMBERS;
                    setup();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.settings, Styles.clearNonei, () -> {
                    FeatureManager.getInstance()
                            .get(ChatFeature.class)
                            .setting()
                            .get()
                            .show();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.refresh, Styles.clearNonei, () -> {
                    ChatStore.getInstance().clearMessages();
                    if (currentChannelId != null) {
                        ChatService.getInstance().fetchMessages(currentChannelId, null);
                        ChatService.getInstance().fetchChatUsers(currentChannelId);
                    }
                    ChatService.getInstance().fetchChannels();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.cancel, Styles.clearNonei, this::collapse).size(40 * scale).padRight(4 * scale);
            } else if (currentMobileTab == MobileTab.CHANNELS) {
                header.image(Icon.move).color(Color.gray).size(24 * scale).padLeft(8 * scale);
                Label title = header.add("@chat.channels").style(Styles.outlineLabel).padLeft(8 * scale).get();
                title.setFontScale(scale);
                header.add().growX();

                header.button(Icon.settings, Styles.clearNonei, () -> {
                    FeatureManager.getInstance()
                            .get(ChatFeature.class)
                            .setting()
                            .get()
                            .show();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.refresh, Styles.clearNonei, () -> {
                    ChatStore.getInstance().clearMessages();
                    String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
                    if (currentChannelId != null) {
                        ChatService.getInstance().fetchMessages(currentChannelId, null);
                        ChatService.getInstance().fetchChatUsers(currentChannelId);
                    }
                    ChatService.getInstance().fetchChannels();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.cancel, Styles.clearNonei, this::collapse).size(40 * scale).padRight(4 * scale);

                header.button(Icon.right, Styles.clearNonei, () -> {
                    currentMobileTab = MobileTab.MESSAGES;
                    setup();
                }).size(40 * scale).padRight(4 * scale);
            } else if (currentMobileTab == MobileTab.MEMBERS) {
                header.button(Icon.left, Styles.clearNonei, () -> {
                    currentMobileTab = MobileTab.MESSAGES;
                    setup();
                }).size(40 * scale).padLeft(4 * scale);

                header.image(Icon.move).color(Color.gray).size(24 * scale).padLeft(4 * scale);

                Label title = header.add("@chat.online-members").style(Styles.outlineLabel).padLeft(8 * scale).get();
                title.setFontScale(scale);
                header.add().growX();

                header.button(Icon.settings, Styles.clearNonei, () -> {
                    FeatureManager.getInstance()
                            .get(ChatFeature.class)
                            .setting()
                            .get()
                            .show();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.refresh, Styles.clearNonei, () -> {
                    ChatStore.getInstance().clearMessages();
                    String cid = ChatStore.getInstance().getCurrentChannelId();
                    if (cid != null) {
                        ChatService.getInstance().fetchMessages(cid, null);
                        ChatService.getInstance().fetchChatUsers(cid);
                    }
                    ChatService.getInstance().fetchChannels();
                }).size(40 * scale).padRight(4 * scale);

                header.button(Icon.cancel, Styles.clearNonei, this::collapse).size(40 * scale).padRight(4 * scale);
            }
        } else {
            header.image(Icon.move).color(Color.gray).size(24 * scale).padLeft(8 * scale);
            Label title = header.add("@chat.global-chat").style(Styles.outlineLabel).padLeft(8 * scale).get();
            title.setFontScale(scale);

            header.add(connectionIndicator).size(10 * scale).padLeft(8 * scale);
            header.add().growX();

            header.button(Icon.settings, Styles.clearNonei, () -> {
                FeatureManager.getInstance()
                        .get(ChatFeature.class)
                        .setting()
                        .get()
                        .show();
            }).size(40 * scale).padRight(4 * scale);

            header.button(Icon.refresh, Styles.clearNonei, () -> {
                ChatStore.getInstance().clearMessages();
                String cid = ChatStore.getInstance().getCurrentChannelId();
                if (cid != null) {
                    ChatService.getInstance().fetchMessages(cid, null);
                    ChatService.getInstance().fetchChatUsers(cid);
                }
                ChatService.getInstance().fetchChannels();
            }).size(40 * scale).padRight(4 * scale);

            header.button(Icon.cancel, Styles.clearNonei, this::collapse).size(40 * scale).padRight(4 * scale);
        }
        return header;
    }

    private Table buildMainContent(float scale, float actualWidth) {
        Table mainContent = new Table();

        if (Vars.mobile) {
            if (currentMobileTab == MobileTab.CHANNELS) {
                Table leftSide = new Table();
                leftSide.top().background(Styles.black5);
                leftSide.add(channelList).grow();
                mainContent.add(leftSide).grow();
            } else if (currentMobileTab == MobileTab.MEMBERS) {
                Table rightSide = new Table();
                rightSide.top().background(Styles.black3);
                rightSide.add(userList).grow().row();
                mainContent.add(rightSide).grow();
            } else {
                Table centerArea = new Table();
                centerArea.add(messageList).grow().row();
                centerArea.add(chatInput).growX().bottom();
                mainContent.add(centerArea).grow().minWidth(0);
            }
        } else {
            float leftWidth = Math.min(160f * scale, actualWidth * 0.25f);
            float rightWidthExp = Math.min(350f * scale, actualWidth * 0.35f);
            float rightWidthCol = 48f * scale;

            Table leftSide = new Table();
            leftSide.top().background(Styles.black5);
            leftSide.add(channelList).grow();
            mainContent.add(leftSide).width(leftWidth).growY();
            mainContent.image(Tex.whiteui).width(1f * scale).color(Color.darkGray).fillY();

            Table centerArea = new Table();
            centerArea.add(messageList).grow().row();
            centerArea.add(chatInput).growX().bottom();
            mainContent.add(centerArea).grow().minWidth(0);

            mainContent.image(Tex.whiteui).width(1f * scale).color(Color.darkGray).fillY();

            Table rightSide = new Table();
            rightSide.top();
            rightSide.background(Styles.black3);

            Runnable rebuildRightSide = new Runnable() {
                @Override
                public void run() {
                    rightSide.clear();
                    Table titleTable = new Table();
                    if (!isUserListCollapsed) {
                        Label l = titleTable.add("@chat.online-members").style(Styles.defaultLabel)
                                .color(Color.gray)
                                .pad(10 * scale).left()
                                .minWidth(0).ellipsis(true).growX().get();
                        l.setFontScale(scale);
                    }

                    titleTable.button(isUserListCollapsed ? Icon.left : Icon.right, Styles.clearNonei, () -> {
                        isUserListCollapsed = !isUserListCollapsed;
                        this.run();
                        Cell<?> cell = mainContent.getCell(rightSide);
                        if (cell != null) {
                            cell.width(isUserListCollapsed ? rightWidthCol : rightWidthExp);
                        }
                    }).size(40 * scale).pad(4 * scale).right();

                    rightSide.add(titleTable).growX().row();

                    if (!isUserListCollapsed) {
                        rightSide.add(userList).grow().row();
                    }
                }
            };
            rebuildRightSide.run();
            mainContent.add(rightSide).width(isUserListCollapsed ? rightWidthCol : rightWidthExp).growY();
        }

        return mainContent;
    }

    public void keepInScreen() {
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

    private void collapse() {
        ChatConfig.collapsed(true);
        ChatStore.getInstance().resetUnreadCount();
        setup();
    }

    private void updateBadge() {
        if (badgeTable == null)
            return;

        int unreadCount = ChatStore.getInstance().getUnreadCount();
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
}
