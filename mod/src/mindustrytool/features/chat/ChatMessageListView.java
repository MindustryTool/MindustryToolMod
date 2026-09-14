package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Timer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.SchematicMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserData;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Direction;
import solim.layout.VirtualList;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.display.Text;
import solim.input.Button;
import solim.signal.Readable;
import solim.overlay.SolimDialog;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;

public class ChatMessageListView extends BaseComponent {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"'{}|\\\\^`]+");

    private final ChatStore store;
    private final @Nullable ChatService service;
    private @Nullable VirtualList<MessageGroup, String> virtualList;

    private @Nullable String lastChannelId = null;
    private int lastMessageCount = 0;
    private @Nullable String lastFirstMessageId = null;

    public ChatMessageListView(ChatStore store) {
        this(store, null);
    }

    public ChatMessageListView(ChatStore store, @Nullable ChatService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    protected Element build() {
        Readable<Boolean> hasMessages = store.activeMessages().map(list -> list != null && !list.isEmpty());
        Readable<Boolean> showEndOfHistory = new Computed<>(() -> {
            Boolean fully = store.activeChannelFullyLoaded().get();
            Boolean has = hasMessages.get();
            return Boolean.TRUE.equals(fully) && Boolean.TRUE.equals(has);
        });

        Readable<List<MessageGroup>> groupedMessages = new Computed<>(() -> {
            List<ChatMessage> msgs = store.activeMessages().get();
            if (msgs == null || msgs.isEmpty()) {
                return Collections.emptyList();
            }
            return ChatMessageGrouper.groupRaw(msgs);
        });

        Effect.of(() -> {
            String chanId = store.activeChannelId().get();
            if (!Objects.equals(chanId, lastChannelId)) {
                lastChannelId = chanId;
                lastMessageCount = 0;
                lastFirstMessageId = null;
                scrollToBottom();
            }
        });

        Effect.of(() -> {
            List<ChatMessage> msgs = store.activeMessages().get();
            if (msgs == null || msgs.isEmpty()) {
                lastMessageCount = 0;
                lastFirstMessageId = null;
                return;
            }

            int count = msgs.size();
            String firstId = msgs.get(0).getId();
            boolean isOlderPrepended = lastMessageCount > 0 && count > lastMessageCount
                    && !Objects.equals(firstId, lastFirstMessageId);
            boolean isNewAppended = lastMessageCount > 0 && count > lastMessageCount
                    && Objects.equals(firstId, lastFirstMessageId);

            if (isOlderPrepended) {
                float prevContentHeight = (virtualList != null) ? virtualList.getTotalHeight() : 0f;
                float prevScrollY = (virtualList != null && virtualList.pane() != null)
                        ? virtualList.pane().getScrollY()
                        : 0f;

                Core.app.post(() -> {
                    if (virtualList != null && virtualList.pane() != null) {
                        var pane = virtualList.pane();
                        pane.layout();
                        float newContentHeight = virtualList.getTotalHeight();
                        float heightDelta = newContentHeight - prevContentHeight;
                        if (heightDelta > 0) {
                            pane.setScrollYForce(prevScrollY + heightDelta);
                            pane.updateVisualScroll();
                        }
                    }
                });
            } else if (isNewAppended) {
                if (virtualList != null && virtualList.pane() != null) {
                    var pane = virtualList.pane();
                    boolean wasNearBottom = (pane.getMaxY() - pane.getScrollY()) <= 150f;
                    if (wasNearBottom) {
                        scrollToBottom();
                    }
                }
            } else if (lastMessageCount == 0) {
                scrollToBottom();
            }

            lastMessageCount = count;
            lastFirstMessageId = firstId;
        });

        Timer.schedule(this::scrollToBottom, 1);

        return column().grow().top().left().gap(unit(1)).padding(unit(2)).children(() -> {
            dynamic(showEndOfHistory, show -> {
                if (Boolean.TRUE.equals(show)) {
                    return row().top().center().growX().padding(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.chat.ui.end-of-history", "Beginning of chat history"))
                                .color(Color.gray)
                                .fontScale(0.85f);
                    });
                }
                return null;
            });

            dynamic(store.loadingOlder(), loading -> {
                if (Boolean.TRUE.equals(loading)) {
                    return row().top().left().padding(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.chat.ui.loading-older", "Loading older messages..."))
                                .color(Color.gray)
                                .fontScale(0.85f)
                                .left();
                    });
                }
                return null;
            });

            dynamic(hasMessages, available -> {
                if (Boolean.TRUE.equals(available)) {
                    virtualList = virtualList(
                            groupedMessages,
                            MessageGroup::getKey,
                            ChatMessageHeightCalculator::calculateHeight,
                            item -> new MessageGroupView(item, store, service))
                                    .grow()
                                    .gap(unit(0.75f))
                                    .overscan(3)
                                    .onReachTop(50f, () -> {
                                        String activeId = store.activeChannelId().peek();
                                        var msgs = store.activeMessages().peek();
                                        if (activeId != null && !activeId.isEmpty() && service != null && msgs != null
                                                && !msgs.isEmpty() && !Boolean.TRUE.equals(store.loadingOlder().peek())
                                                && !store.isFullyLoaded(activeId)) {
                                            service.fetchOlderMessages(activeId);
                                        }
                                    });

                    return virtualList.cellPaddingBottom(unit(2));
                } else {
                    return column().padding(unit(4)).top().left().children(() -> {
                        text(Core.bundle.get("feature.chat.ui.empty-messages", "No messages yet."))
                                .color(Color.gray)
                                .fontScale(0.9f)
                                .left();
                    });
                }
            }).grow();
        }).element();
    }

    public void scrollToBottom() {
        if (virtualList != null) {
            Core.app.post(() -> {
                if (virtualList != null && virtualList.pane() != null) {
                    var pane = virtualList.pane();
                    pane.layout();
                    pane.setScrollYForce(pane.getMaxY());
                    pane.updateVisualScroll();
                    Core.app.post(() -> {
                        if (virtualList != null && virtualList.pane() != null) {
                            var p = virtualList.pane();
                            p.layout();
                            p.setScrollYForce(p.getMaxY());
                            p.updateVisualScroll();
                        }
                    });
                }
            });
        }
    }

    static class MessageGroupView extends BaseComponent {
        private final MessageGroup group;
        private final ChatStore store;
        private final @Nullable ChatService service;

        public MessageGroupView(MessageGroup group, ChatStore store, @Nullable ChatService service) {
            this.group = group;
            this.store = store;
            this.service = service;
        }

        @Override
        protected Element build() {
            ParsedChatMessage first = group.getMessage(0);
            ChatMessage firstRaw = first.getRaw();
            String authorId = group.getAuthorId();

            Readable<UserData> user = store.user(authorId);
            Readable<String> authorName = user.map(u -> (u != null && u.getName() != null && !u.getName().isEmpty())
                    ? u.getName()
                    : (authorId != null ? authorId : "Unknown"));

            Readable<Color> authorColor = user.map(u -> {
                if (u != null && u.getHighestRole().isPresent()) {
                    try {
                        String hex = u.getHighestRole().get().getColor();
                        if (hex != null && !hex.isEmpty()) {
                            return Color.valueOf(hex);
                        }
                    } catch (Exception ignored) {
                    }
                }
                return Pal.accent;
            });

            Readable<String> avatarUrl = user
                    .map(u -> (u != null && u.getImageUrl() != null && !u.getImageUrl().isEmpty())
                            ? u.getImageUrl()
                            : null);

            String timeStr = formatTime(group.getCreatedAt());

            return row()
                    .growX()
                    .top().left()
                    .gap(ChatMessageHeightCalculator.AVATAR_GAP)
                    .padding(ChatMessageHeightCalculator.UNIT_1)
                    .children(() -> {
                        // Shared group avatar on the left, pinned to the top
                        new ChatAvatar(authorName, avatarUrl, authorId, ChatMessageHeightCalculator.AVATAR_SIZE);

                        // Right column: header followed by stacked messages
                        column().growX().top().left().children(() -> {
                            // Author and timestamp header + action button
                            row().growX().top().left()
                                    .height(ChatMessageHeightCalculator.HEADER_HEIGHT
                                            + ChatMessageHeightCalculator.HEADER_GAP)
                                    .children(() -> {
                                        text(authorName)
                                                .color(authorColor)
                                                .fontScale(0.95f)
                                                .left();

                                        if (!timeStr.isEmpty()) {
                                            text(timeStr)
                                                    .color(Color.gray)
                                                    .fontScale(0.8f)
                                                    .marginLeft(unit(1))
                                                    .left();
                                        }

                                        spacer();

                                        // Action ellipsis / menu button
                                        Button actionsButton = button(() -> {
                                        });
                                        actionsButton
                                                .onClick(() -> openActions(firstRaw, actionsButton.element()))
                                                .style(Styles.clearNonei)
                                                .size(unit(6), unit(6))
                                                .children(() -> icon(FileIcon.of("ellipsis-vertical.png"))
                                                        .size(unit(6), unit(6)));
                                    });

                            // Stacked message rows
                            column().growX().top().left().children(() -> {
                                var messages = group.getMessages();
                                for (int i = 0; i < messages.size(); i++) {
                                    buildMessageRow(messages.get(i), i > 0);
                                }
                            });
                        });
                    }).element();
        }

        private void buildMessageRow(ParsedChatMessage parsed, boolean hasPrevious) {
            ChatMessage raw = parsed.getRaw();
            String msgId = raw.getId();
            boolean mentioned = (parsed instanceof TextMessage) && ((TextMessage) parsed).isMentionsCurrentUser();

            Readable<Set<String>> pendingIds = store.pendingMessageIds();
            Readable<Set<String>> failedIds = store.failedMessageIds();
            Readable<Boolean> isPending = pendingIds.map(set -> set != null && set.contains(msgId));
            Readable<Boolean> isFailed = failedIds.map(set -> set != null && set.contains(msgId));

            var card = card().growX().top().left();
            card.onClick(() -> openActions(raw, card.element()));
            if (hasPrevious) {
                card.cellPaddingTop(ChatMessageHeightCalculator.MESSAGE_GAP);
            }
            card.children(() -> {
                row().growX().top().left()
                        .padding(ChatMessageHeightCalculator.MESSAGE_CARD_PADDING / 2f)
                        .children(() -> {
                            if (mentioned) {
                                divider(Direction.Y).color(Pal.accent).width(unit(1)).cellPaddingRight(unit(1));
                            }

                            if (raw.getReplyTo() != null && !raw.getReplyTo().isEmpty()) {
                                column().growX().top().left().children(() -> {
                                    buildReplyPreview(raw.getReplyTo());
                                    column().growX().top().left().cellPaddingTop(ChatMessageHeightCalculator.REPLY_GAP)
                                            .children(() -> {
                                                buildMessageBody(parsed, isPending, isFailed);
                                            });
                                });
                            } else {
                                buildMessageBody(parsed, isPending, isFailed);
                            }
                        });
            });
        }

        private void openActions(ChatMessage message, @Nullable Element anchor) {
            float x = -1f;
            float y = -1f;
            if (anchor != null) {
                try {
                    Vec2 stage = anchor.localToStageCoordinates(new Vec2(0f, anchor.getHeight()));
                    x = stage.x;
                    y = stage.y;
                } catch (Throwable ignored) {
                }
            }
            ChatActionPopup.showFor(message, x, y);
        }

        private void buildReplyPreview(String replyToId) {
            ChatMessage target = null;
            var list = store.activeMessages().peek();
            if (list != null) {
                for (ChatMessage m : list) {
                    if (Objects.equals(m.getId(), replyToId)) {
                        target = m;
                        break;
                    }
                }
            }

            String targetSnippet;
            if (target != null && target.getContent() != null) {
                String clean = target.getContent().replace('\n', ' ').trim();
                targetSnippet = clean.length() > 40 ? clean.substring(0, 37) + "..." : clean;
            } else {
                targetSnippet = replyToId;
            }

            final String displaySnippet = targetSnippet;
            row().growX().top().left().height(ChatMessageHeightCalculator.REPLY_PREVIEW_HEIGHT).children(() -> {
                icon(Icon.rightSmall).size(unit(4), unit(4)).color(Color.gray).cellPaddingRight(unit(1));
                text(displaySnippet)
                        .color(Color.gray)
                        .fontScale(0.8f)
                        .ellipsis()
                        .left();
            });
        }

        private void buildMessageBody(ParsedChatMessage parsed, Readable<Boolean> isPending,
                Readable<Boolean> isFailed) {
            Readable<Color> bodyColor = new Computed<>(() -> {
                if (Boolean.TRUE.equals(isFailed.get())) {
                    return Color.scarlet;
                }
                if (Boolean.TRUE.equals(isPending.get())) {
                    return new Color(1f, 1f, 1f, 0.5f);
                }
                return Color.white;
            });

            if (parsed instanceof RoomInviteMessage) {
                RoomInviteMessage invite = (RoomInviteMessage) parsed;
                final String link = invite.getConnectLink();
                card().growX().top().left().children(() -> {
                    column().growX().top().left().padding(unit(1.5f)).gap(unit(1)).children(() -> {
                        row().growX().top().left().gap(unit(1)).children(() -> {
                            icon(Icon.host).size(unit(5), unit(5)).color(Pal.accent);
                            text(Core.bundle.get("feature.chat.ui.room-invite", "Room Invite"))
                                    .color(Pal.accent)
                                    .fontScale(0.95f)
                                    .left();
                        });
                        text(link).color(Color.lightGray).fontScale(0.85f).ellipsis().left();
                        row().top().left().gap(unit(1)).children(() -> {
                            button(Core.bundle.get("button.copy", "Copy Link"), () -> {
                                Core.app.setClipboardText(link);
                                Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied!"));
                            }).style(Styles.defaultt).height(unit(7));
                        });
                    });
                });
                return;
            }

            if (parsed instanceof ImageMessage) {
                ImageMessage img = (ImageMessage) parsed;
                final String imageUrl = img.getImageUrl();
                column().growX().top().left().gap(unit(1)).children(() -> {
                    text(imageUrl).color(Color.lightGray).fontScale(0.85f).wrap().left().growX();
                    networkImage(imageUrl)
                            .placeholder(Icon.image)
                            .fallback(Icon.cancel)
                            .size(unit(40), unit(30))
                            .top().left();
                });
                return;
            }

            if (parsed instanceof MindustryToolLinkMessage) {
                MindustryToolLinkMessage toolLink = (MindustryToolLinkMessage) parsed;
                final String fullUrl = toolLink.getFullUrl();
                final String type = toolLink.getType();
                final String itemId = toolLink.getItemId();

                card().growX().top().left().children(() -> {
                    column().growX().top().left().padding(unit(1.5f)).gap(unit(1)).children(() -> {
                        row().growX().top().left().gap(unit(1)).children(() -> {
                            icon("maps".equals(type) ? Icon.map : Icon.paste).size(unit(5), unit(5)).color(Pal.accent);
                            text(("maps".equals(type) ? "Map: " : "Schematic: ") + itemId)
                                    .color(Pal.accent)
                                    .fontScale(0.95f)
                                    .ellipsis()
                                    .left();
                        });
                        row().top().left().gap(unit(1)).children(() -> {
                            button(Core.bundle.get("button.copy", "Copy Link"), () -> {
                                Core.app.setClipboardText(fullUrl);
                                Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied!"));
                            }).style(Styles.defaultt).height(unit(7));

                            button(Core.bundle.get("feature.chat.ui.view-web", "View in Browser"), () -> {
                                Core.app.openURI(fullUrl);
                            }).style(Styles.defaultt).height(unit(7));
                        });
                    });
                });
                return;
            }

            if (parsed instanceof SchematicMessage) {
                SchematicMessage schemMsg = (SchematicMessage) parsed;
                if (schemMsg.getPrefixText() != null && !schemMsg.getPrefixText().isEmpty()) {
                    text(schemMsg.getPrefixText()).color(bodyColor).fontScale(1.0f).wrap().left().growX();
                }

                buildSchematicCard(schemMsg.getSchematic());

                if (schemMsg.getSuffixText() != null && !schemMsg.getSuffixText().isEmpty()) {
                    text(schemMsg.getSuffixText()).color(bodyColor).fontScale(1.0f).wrap().left().growX();
                }
                return;
            }

            if (parsed instanceof TextMessage) {
                TextMessage txt = (TextMessage) parsed;
                buildMessageText(txt.getText(), bodyColor);
                final String translatedId = parsed.getId();
                dynamic(store.translation(translatedId), translated -> {
                    if (translated == null || translated.isEmpty()) {
                        return null;
                    }
                    final String translatedText = translated;
                    return column().growX().top().left().cellPaddingTop(unit(1)).children(() -> {
                        text(Core.bundle.get("feature.chat.ui.translated-badge", "Translated"))
                                .color(Pal.accent)
                                .fontScale(0.8f)
                                .left();
                        buildMessageText(translatedText, bodyColor);
                    });
                });
                dynamic(store.translatingMessageId(), translatingId -> {
                    if (translatingId == null || !translatingId.equals(translatedId)) {
                        return null;
                    }
                    return row().growX().top().left().children(() -> {
                        text(Core.bundle.get("feature.chat.ui.translating", "Translating..."))
                                .color(Color.gray)
                                .fontScale(0.8f)
                                .left();
                    });
                });
                return;
            }

            // Fallback
            String fallback = parsed.getContent() != null ? parsed.getContent() : "";
            text(fallback)
                    .color(bodyColor)
                    .fontScale(1.0f)
                    .left()
                    .wrap()
                    .growX();
        }

        private void buildSchematicCard(Schematic schematic) {
            card().top().left().children(() -> {
                column().top().left().gap(unit(1)).children(() -> {
                    // Header: just the name
                    row().growX().top().left().gap(unit(1)).children(() -> {
                        text(schematic.name())
                                .color(Pal.accent)
                                .fontScale(0.95f)
                                .ellipsis()
                                .growX()
                                .left();

                        spacer();
                    });

                    // Preview image button
                    float ratio = schematic.height > 0 ? (float) schematic.width / (float) schematic.height : 1f;
                    float width = Math.max(unit(10), ratio * unit(35));

                    button(() -> useSchematic(schematic))
                            .style(Styles.flatt)
                            .height(unit(35))
                            .width(width)
                            .children(() -> {
                                arc(new SchematicImage(schematic).setScaling(Scaling.fit));
                            });

                    // Action buttons row below preview
                    row().growX().top().left().gap(unit(1)).children(() -> {
                        button(() -> Vars.ui.schematics.showInfo(schematic))
                                .style(Styles.clearNonei)
                                .size(unit(6), unit(6))
                                .tooltip(Core.bundle.get("info.title", "Info"))
                                .children(() -> icon(Icon.infoSmall).size(unit(5), unit(5)));

                        button(() -> Vars.ui.schematics.showExport(schematic))
                                .style(Styles.clearNonei)
                                .size(unit(6), unit(6))
                                .tooltip(Core.bundle.get("editor.export", "Export"))
                                .children(() -> icon(Icon.upload).size(unit(5), unit(5)));

                        button(() -> Vars.ui.schematics.showEdit(schematic))
                                .style(Styles.clearNonei)
                                .size(unit(6), unit(6))
                                .tooltip(Core.bundle.get("schematic.edit", "Edit"))
                                .children(() -> icon(Icon.pencil).size(unit(5), unit(5)));

                        button(() -> useSchematic(schematic))
                                .style(Styles.clearNonei)
                                .size(unit(6), unit(6))
                                .tooltip(Core.bundle.get("feature.chat.ui.schematic.use", "Use"))
                                .children(() -> icon(Icon.play).size(unit(5), unit(5)));
                    });
                });
            });
        }

        private Component buildMessageText(String messageText, Readable<Color> bodyColor) {
            if (messageText == null || messageText.isEmpty()) {
                return text("").color(bodyColor).fontScale(1.0f).left().wrap().growX();
            }

            Matcher m = URL_PATTERN.matcher(messageText);
            List<String> urls = new ArrayList<>();
            StringBuilder marked = new StringBuilder();
            int prev = 0;
            String linkHex = WebStyles.Colors.PRIMARY.toString().substring(0, 6);
            while (m.find()) {
                if (m.start() > prev) {
                    marked.append(messageText, prev, m.start());
                }
                String url = m.group();
                urls.add(url);
                marked.append("[#").append(linkHex).append("]").append(url).append("[]");
                prev = m.end();
            }

            if (urls.isEmpty()) {
                return text(messageText).color(bodyColor).fontScale(1.0f).left().wrap().growX();
            }

            // Bare link fast path: the whole message is one clickable link.
            if (urls.size() == 1 && messageText.trim().equals(urls.get(0))) {
                final String onlyUrl = urls.get(0);
                Text linkText = text(onlyUrl)
                        .color(WebStyles.Colors.PRIMARY)
                        .fontScale(1.0f)
                        .left()
                        .wrap()
                        .growX();
                linkText.label().addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        event.stop();
                        openLinkDialog(onlyUrl);
                    }
                });
                return linkText;
            }

            if (prev < messageText.length()) {
                marked.append(messageText.substring(prev));
            }
            final String highlighted = marked.toString();
            final List<String> linkUrls = new ArrayList<>(urls);
            return column().growX().top().left().children(() -> {
                text(highlighted).color(bodyColor).fontScale(1.0f).left().wrap().growX();

                // Clickable link chips below the text; the inline label itself is not
                // clickable.
                row().growX().top().left().gap(unit(1)).children(() -> {
                    for (String linkUrl : linkUrls) {
                        final String chipUrl = linkUrl;
                        Text chip = text(chipUrl)
                                .color(WebStyles.Colors.PRIMARY)
                                .fontScale(0.85f)
                                .ellipsis()
                                .left();
                        chip.label().addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                event.stop();
                                openLinkDialog(chipUrl);
                            }
                        });
                    }
                });
            });
        }

        private void openLinkDialog(String url) {
            SolimDialog dialog = new SolimDialog(Core.bundle.get("chat.link.dialog.title"));
            dialog.addCloseButton();
            dialog.maxWidth(400f);
            dialog.children(() -> {
                column().growX().padding(unit(3)).gap(unit(2)).children(() -> {
                    text(Core.bundle.format("chat.link.dialog.prompt", url))
                            .wrap()
                            .growX()
                            .left();
                    row().gap(unit(1)).children(() -> {
                        button(Core.bundle.get("chat.link.dialog.cancel"), dialog::hide)
                                .style(Styles.defaultt)
                                .height(unit(7));
                        button(Core.bundle.get("chat.link.dialog.open"), () -> {
                            Core.app.openURI(url);
                            dialog.hide();
                        }).style(Styles.defaultb).height(unit(7));
                    });
                });
            });
            dialog.show();
        }

        private void useSchematic(Schematic schematic) {
            if (Vars.state.isMenu()) {
                Vars.ui.schematics.showInfo(schematic);
            } else {
                if (!Vars.state.rules.schematicsAllowed) {
                    Vars.ui.showInfo(Core.bundle.get("schematic.disabled", "Schematics are disabled."));
                } else {
                    Vars.control.input.useSchematic(schematic);
                }
            }
        }
    }

    private static String formatTime(@Nullable String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) {
            return "";
        }
        try {
            Instant instant = Instant.parse(createdAt);
            return DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
        } catch (Throwable ignored) {
            return "";
        }
    }
}
