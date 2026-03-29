package mindustrytool.features.chat.global.ui;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.features.browser.map.MapDialog;
import mindustrytool.features.browser.map.MapImage;
import mindustrytool.features.browser.map.MapInfoDialog;
import mindustrytool.features.browser.schematic.SchematicDialog;
import mindustrytool.features.browser.schematic.SchematicInfoDialog;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatService;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.playerconnect.PlayerConnectLink;
import mindustrytool.features.playerconnect.PlayerConnectRenderer;
import mindustrytool.services.MapService;
import mindustrytool.services.PlayerConnectService;
import mindustrytool.services.SchematicService;
import mindustrytool.services.UserService;
import mindustrytool.ui.NetworkImage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageList extends Table {
    private static final Pattern MINDUSTRY_TOOL_LINK_PATTERN = Pattern
            .compile("^https?://[^/]+/[^/]+/(schematics|maps)/([0-9a-fA-F-]+)");

    private static final float TARGET_WIDTH = 250f;
    private static final float PREVIEW_BUTTON_SIZE = 50f;
    private static final float CARD_HEIGHT = 330f;

    private final Table messageTable;
    private final ScrollPane scrollPane;
    private final Table loadingTable;
    private final PlayerConnectService playerConnectService = new PlayerConnectService();

    private final SchematicInfoDialog schematicInfoDialog = new SchematicInfoDialog();
    private final MapInfoDialog mapInfoDialog = new MapInfoDialog();

    private String expandedMessageId = null;
    private ChatInput chatInput;

    public MessageList(ChatInput chatInput) {
        this.chatInput = chatInput;
        messageTable = new Table();
        messageTable.top().left();

        scrollPane = new ScrollPane(messageTable);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.visible(() -> ChatService.getInstance().isConnected());
        scrollPane.update(() -> {
            ChatStore store = ChatStore.getInstance();
            String currentChannelId = store.getCurrentChannelId();
            if (scrollPane.getScrollY() < 100 && !store.isLoadingMessages() && currentChannelId != null
                    && !store.isFullyLoaded(currentChannelId)) {
                Seq<ChatMessage> msgs = store.getMessages(currentChannelId);
                if (msgs != null && !msgs.isEmpty()) {
                    ChatService.getInstance().fetchMessages(currentChannelId, msgs.first().id);
                }
            }
        });

        loadingTable = new Table();
        float scale = ChatConfig.scale();
        loadingTable.add("@loading").style(Styles.defaultLabel).color(Color.gray)
                .visible(() -> !ChatService.getInstance().isConnected() || ChatStore.getInstance().isLoadingMessages())
                .get()
                .setFontScale(scale);

        arc.scene.ui.layout.Stack stack = new arc.scene.ui.layout.Stack();
        stack.add(loadingTable);
        stack.add(scrollPane);

        add(stack).grow();

        Events.on(ChatStore.MessagesUpdateEvent.class, e -> {
            if (e.channelId.equals(ChatStore.getInstance().getCurrentChannelId())) {
                float oldMaxY = scrollPane.getMaxY();
                float oldScrollY = scrollPane.getScrollY();

                rebuild();

                if (e.isPrepend) {
                    Core.app.post(() -> {
                        float newMaxY = scrollPane.getMaxY();
                        scrollPane.setScrollYForce(oldScrollY + (newMaxY - oldMaxY));
                    });
                } else {
                    var nearBottom = scrollPane.getScrollY() >= scrollPane.getMaxY() - 100;
                    if (nearBottom) {
                        scrollToBottom();
                    }
                }
            }
        });
        Events.on(ChatStore.CurrentChannelChangeEvent.class, e -> {
            rebuild();
            scrollToBottom();
        });
        Events.on(ChatStore.LoadingMessagesEvent.class, e -> {
            // loadingTable visibility updates automatically
        });
    }

    public void rebuild() {
        messageTable.clear();
        messageTable.top().left();

        ChatStore store = ChatStore.getInstance();
        String currentChannelId = store.getCurrentChannelId();
        if (currentChannelId != null) {
            ChatConfig.lastRead(Instant.now());
        } else {
            return;
        }

        if (store.isFullyLoaded(currentChannelId)) {
            messageTable.add("End").row();
        }

        Seq<ChatMessage> channelMsgs = store.getMessages(currentChannelId);
        if (channelMsgs == null)
            return;

        float scale = ChatConfig.scale();

        for (ChatMessage msg : channelMsgs) {
            Table entry = new Table();
            entry.setBackground(null);

            entry.table(avatar -> {
                avatar.top();
                UserService.findUserById(msg.createdBy).thenAccept(data -> {
                    Core.app.post(() -> {
                        avatar.clear();
                        if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
                            avatar.add(new NetworkImage(data.getImageUrl())).size(40 * scale);
                        } else {
                            avatar.add(new Image(Icon.players)).size(40 * scale);
                        }
                    });
                });
            }).size(48 * scale).top().pad(8 * scale);

            entry.table(card -> {
                card.top().left();

                Label label = new Label("...");
                label.setStyle(Styles.defaultLabel);
                label.setFontScale(scale);

                UserService.findUserById(msg.createdBy).thenAccept(data -> {
                    Core.app.post(() -> {
                        String timeStr = msg.createdAt;
                        if (msg.createdAt != null) {
                            try {
                                Instant instant = Instant.parse(msg.createdAt);
                                timeStr = DateTimeFormatter.ofPattern("HH:mm")
                                        .withZone(ZoneId.systemDefault())
                                        .format(instant);
                            } catch (Throwable err) {
                                Log.err(err);
                            }
                        }

                        Color color = data.getHighestRole()
                                .map(r -> {
                                    try {
                                        return Color.valueOf(r.getColor());
                                    } catch (Exception err) {
                                        return Color.white;
                                    }
                                })
                                .orElse(Color.white);

                        label.setText("[#" + color.toString() + "]" + data.getName() + "[white]"
                                + (timeStr.isEmpty() ? "" : " [gray]" + timeStr));
                    });
                });

                card.add(label).left().row();

                if (msg.replyTo != null && !msg.replyTo.isEmpty()) {
                    ChatMessage repliedMsg = channelMsgs.find(m -> m.id.equals(msg.replyTo));
                    if (repliedMsg != null) {
                        card.table(replyTable -> {
                            replyTable.center().left();
                            replyTable.image(Icon.rightSmall).size(16 * scale).padRight(4 * scale).color(Color.gray);

                            Label replyContent = new Label(repliedMsg.content.replace('\n', ' '));
                            replyContent.setFontScale(scale);
                            replyContent.setColor(Color.gray);
                            replyContent.setEllipsis(true);
                            replyTable.add(replyContent).minWidth(0).maxWidth(200 * scale);
                        }).growX().padBottom(2 * scale).row();
                    }
                }

                card.table(c -> renderContent(c, msg.content, scale)).top().left().growX().padTop(6 * scale);

                card.clicked(() -> {
                    if (expandedMessageId != null && expandedMessageId.equals(msg.id)) {
                        expandedMessageId = null;
                    } else {
                        expandedMessageId = msg.id;
                    }
                    rebuild();
                });

                if (expandedMessageId != null && expandedMessageId.equals(msg.id)) {
                    card.row();
                    card.table(actions -> {
                        actions.left().defaults().height(36 * scale).minWidth(160 * scale).padRight(8 * scale);

                        TextButton copyBtn = new TextButton("@copy", Styles.defaultt);
                        copyBtn.clicked(() -> {
                            try {
                                Core.app.setClipboardText(msg.content);
                                Vars.ui.showInfoFade("@copied");
                                expandedMessageId = null;
                                rebuild();
                            } catch (Exception e) {
                                Vars.ui.showInfoFade(e.getMessage());
                            }
                        });
                        copyBtn.getLabel().setFontScale(scale * 0.8f);
                        actions.add(copyBtn);

                        TextButton replyBtn = new TextButton("@chat.reply", Styles.defaultt);
                        replyBtn.clicked(() -> {
                            chatInput.setReplyingTo(msg.id);
                            expandedMessageId = null;
                            rebuild();
                        });
                        replyBtn.getLabel().setFontScale(scale * 0.8f);
                        actions.add(replyBtn);

                    }).growX().padTop(4 * scale);
                }
            }).growX().pad(8 * scale).top();

            messageTable.add(entry).growX().padBottom(4 * scale).row();
        }
    }

    private void renderContent(Table c, String content, float scale) {
        content = content.trim();

        if (PlayerConnectLink.isValid(content)) {
            Label l = c.add(content).wrap().color(Color.lightGray).left().growX().get();
            l.setFontScale(scale);
            c.row();
            renderPlayerConnectRoom(c, content);
            return;
        }

        if (NetworkImage.isValidImageLink(content)) {
            Label l = c.add(content).wrap().color(Color.lightGray).left().growX().get();
            l.setFontScale(scale);
            c.row();
            c.add(new NetworkImage(content)).maxHeight(800 * scale).maxWidth(800 * scale);
            c.table().growX();
            return;
        }

        int schematicBasePosition = content.indexOf(Vars.schematicBaseStart);

        if (schematicBasePosition != -1) {
            int endPosition = content.indexOf(" ", schematicBasePosition) + 1;
            if (endPosition == 0)
                endPosition = content.length();

            String prev = content.substring(0, schematicBasePosition);
            Label l = c.add(prev).wrap().color(Color.lightGray).left().growX().get();
            l.setFontScale(scale);
            String schematicBase64 = content.substring(schematicBasePosition, endPosition);

            try {
                var schematic = Schematics.readBase64(schematicBase64);
                c.row();
                renderSchematic(c, schematic);
                c.row();
                String after = content.substring(endPosition);
                Label l2 = c.add(after).wrap().color(Color.lightGray).left().growX().get();
                l2.setFontScale(scale);
            } catch (Exception e) {
                c.clear();
                Label l2 = c.add(content).wrap().color(Color.lightGray).left().growX().get();
                l2.setFontScale(scale);
            }
            return;
        }

        Matcher matcher = MINDUSTRY_TOOL_LINK_PATTERN.matcher(content);
        if (matcher.find()) {
            String url = matcher.group(0);
            String contentType = matcher.group(1);
            String id = matcher.group(2);

            if (contentType.equals("maps")) {
                renderMap(c, id, url);
                c.table().growX();
                return;
            }

            if (contentType.equals("schematics")) {
                renderSchematic(c, id, url);
                c.table().growX();
                return;
            }
        }

        Label l = c.add(content).wrap().color(Color.lightGray).left().growX().get();
        l.setFontScale(scale);
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

    private void renderSchematic(Table table, String id, String url) {
        table.table(Tex.pane, preview -> {
            preview.top().left().margin(0f);
            preview.table(buttons -> {
                buttons.center().defaults().size(PREVIEW_BUTTON_SIZE);
                buttons.button(Icon.copy, Styles.emptyi, () -> SchematicDialog.handleCopySchematic(id)).pad(2);
                buttons.button(Icon.download, Styles.emptyi, () -> SchematicDialog.handleDownloadSchematic(id)).pad(2);
                buttons.button(Icon.info, Styles.emptyi,
                        () -> SchematicService.findSchematicById(id)
                                .thenAccept(schem -> Core.app.post(() -> schematicInfoDialog.show(schem))))
                        .tooltip("@info.title");
                buttons.button(Icon.link, Styles.emptyi, () -> Core.app.openURI(url)).pad(2);
            }).growX().height(PREVIEW_BUTTON_SIZE);
            preview.row();
            preview.stack(new Table(t -> t.add(new mindustrytool.features.browser.schematic.SchematicImage(id)))).top()
                    .left();
        }).style(Styles.flati).width(TARGET_WIDTH).height(CARD_HEIGHT).top().left();
    }

    private void renderMap(Table table, String id, String url) {
        table.table(Tex.pane, preview -> {
            preview.top().left().margin(0f);
            preview.table(buttons -> {
                buttons.center().defaults().size(PREVIEW_BUTTON_SIZE);
                buttons.button(Icon.download, Styles.emptyi, () -> MapDialog.handleDownloadMap(id)).pad(2);
                buttons.button(Icon.info, Styles.emptyi,
                        () -> MapService.findMapById(id)
                                .thenAccept(m -> Core.app.post(() -> mapInfoDialog.show(m))))
                        .tooltip("@info.title");
                buttons.button(Icon.link, Styles.emptyi, () -> Core.app.openURI(url)).pad(2);
            }).growX().height(PREVIEW_BUTTON_SIZE);
            preview.row();
            preview.stack(new Table(t -> t.add(new MapImage(id)))).top().left();
        }).style(Styles.flati).width(TARGET_WIDTH).height(CARD_HEIGHT).top().left();
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
                buttons.button(Icon.upload, style, () -> Vars.ui.schematics.showExport(schematic))
                        .tooltip("@editor.export").growX();
                buttons.button(Icon.pencil, style, () -> Vars.ui.schematics.showEdit(schematic))
                        .tooltip("@schematic.edit").growX();
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

    public void scrollToBottom() {
        if (scrollPane != null) {
            Core.app.post(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
            Time.runTask(10, () -> {
                if (scrollPane != null) {
                    Core.app.post(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
                }
            });
        }
    }
}
