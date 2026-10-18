package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.border;
import static solim.modifier.Values.color;
import static solim.modifier.Values.ellipsis;
import static solim.modifier.Values.fallback;
import static solim.modifier.Values.fontScale;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.marginBottom;
import static solim.modifier.Values.marginLeft;
import static solim.modifier.Values.marginRight;
import static solim.modifier.Values.marginTop;
import static solim.modifier.Values.onClick;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.placeholder;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.stopClickPropagation;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.width;
import static solim.modifier.Values.wrap;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Scaling;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.FileIcon;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.CommandKind;
import mindustrytool.features.chat.models.ParsedChatMessage.CommandMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.SchematicMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.features.browser.map.MapBrowserFeature;
import mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.features.playerconnect.ui.RoomCard;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.UserData;
import mindustrytool.models.response.ChatUser.SimpleRole;
import solim.core.Component;
import solim.layout.Card;
import solim.layout.Direction;
import solim.layout.VirtualList;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.display.Text;
import solim.input.Button;
import solim.reactive.Readable;
import solim.overlay.SolimDialog;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.ScrollPane;

public class ChatMessageListView extends Component {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"'{}|\\\\^`]+");

    private final ChatStore store;
    private final @Nullable ChatService service;
    private @Nullable VirtualList<MessageGroup> virtualList;

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
        Readable<Boolean> hasChannel = store.channels().activeId().map(id -> id != null && !id.isEmpty());
        Readable<Boolean> hasMessages = store.messages().active().map(list -> list != null && !list.isEmpty());
        Readable<Boolean> showEndOfHistory = new Computed<>(() -> {
            Boolean fully = store.messages().activeFullyLoaded().get();
            Boolean has = hasMessages.get();
            return Boolean.TRUE.equals(fully) && Boolean.TRUE.equals(has);
        });
        Readable<Boolean> showRefreshErrorBanner = new Computed<>(() -> {
            String err = store.messages().activeError().get();
            Boolean has = hasMessages.get();
            return err != null && !err.trim().isEmpty() && Boolean.TRUE.equals(has);
        });

        Readable<List<MessageGroup>> groupedMessages = new Computed<>(() -> {
            List<ChatMessage> msgs = store.messages().active().get();
            if (msgs == null || msgs.isEmpty()) {
                return Collections.emptyList();
            }
            return ChatMessageGrouper.groupRaw(msgs);
        });

        Effect.of(() -> {
            String chanId = store.channels().activeId().get();
            if (!Objects.equals(chanId, lastChannelId)) {
                lastChannelId = chanId;
                lastMessageCount = 0;
                lastFirstMessageId = null;
                scrollToBottom();
            }
        });

        Effect.of(() -> {
            List<ChatMessage> msgs = store.messages().active().get();
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
                        ScrollPane pane = virtualList.pane();
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
                    ScrollPane pane = virtualList.pane();
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

        scrollToBottom();

        return column().name("chat-list").props(grow(), padding(unit(2)), top(), left(), gap(unit(1))).children(() -> {
            dynamic(hasChannel, channelSelected -> {
                if (!Boolean.TRUE.equals(channelSelected)) {
                    query(store.channels().channelsQuery())
                            .props(grow())
                            .loading(Loader::centered)
                            .error(chanErr -> column().props(grow(), padding(unit(4)), center(), gap(unit(2))).children(() -> {
                                icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(Color.scarlet));
                                text(Core.bundle.get("feature.chat.ui.channels-failed", "Channels failed to load."))
                                        .props(color(Color.scarlet), fontScale(1.0f), wrap(true), center());
                                text(chanErr != null ? chanErr.getMessage() : "")
                                        .props(color(Color.gray), fontScale(0.85f), wrap(true), center());
                                button(Core.bundle.get("feature.chat.ui.retry-channels", "Retry Channels"), () -> {
                                    if (service != null) {
                                        service.refreshChannels();
                                    } else {
                                        store.channels().channelsQuery().refetch();
                                    }
                                })
                                        .props(style(WebStyles.secondary()), height(unit(10)), prefHeight(unit(10)))
                                        .children(() -> {
                                            icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                            text(Core.bundle.get("feature.chat.ui.retry-channels",
                                                    "Retry Channels"));
                                        });
                            }))
                            .data(channels -> column().props(padding(unit(4)), top(), left()).children(() -> {
                                text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                        .props(color(Color.gray), fontScale(0.9f), left());
                            })).props(grow());
                } else {

                    column().props(grow(), top(), left(), gap(unit(1))).children(() -> {
                        when(showRefreshErrorBanner)
                                .thenDo(() -> {
                                    card().props(growX(), padding(unit(1.5f))).children(() -> {
                                        row().props(growX(), gap(unit(1)), center()).children(() -> {
                                            icon(Icon.warning).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(Color.scarlet));
                                            text(Core.bundle.get("feature.chat.ui.refresh-failed",
                                                    "Failed to refresh messages."))
                                                    .props(color(Color.scarlet), fontScale(0.85f), growX(), left());
                                            button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                                                String activeId = store.channels().currentActiveId();
                                                if (activeId != null && service != null) {
                                                    service.loadMessages(activeId);
                                                }
                                            })
                                                    .props(style(WebStyles.secondary()), height(unit(8)), prefHeight(unit(8)))
                                                    .children(() -> {
                                                        icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                                        text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                                    });
                                        });
                                    });
                                });

                        when(showEndOfHistory)
                                .thenDo(() -> {
                                    row().props(top(), center(), growX(), padding(unit(2))).children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.end-of-history",
                                                "Beginning of chat history"))
                                                .props(color(Color.gray), fontScale(0.85f));
                                    });
                                });

                        when(store.messages().loadingOlder())
                                .thenDo(() -> {
                                    row().props(top(), left(), padding(unit(2))).children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.loading-older",
                                                "Loading older messages..."))
                                                .props(color(Color.gray), fontScale(0.85f), left());
                                    });
                                });

                        when(hasMessages)
                                .thenDo(() -> {
                                    virtualList = virtualList(
                                            groupedMessages,
                                            ChatMessageHeightCalculator::calculateHeight)
                                            .key(MessageGroup::getKey)
                                            .props(grow(), gap(unit(0.75f)), marginBottom(unit(2)))
                                            .overscan(3)
                                            .onReachTop(50f, () -> {
                                                String activeId = store.channels().currentActiveId();
                                                List<ChatMessage> msgs = store.messages().currentActive();
                                                if (activeId != null && !activeId.isEmpty() && service != null
                                                        && msgs != null
                                                        && !msgs.isEmpty() && !store.messages().isLoadingOlder()
                                                        && !store.messages().isFullyLoaded(activeId)) {
                                                    service.fetchOlderMessages(activeId);
                                                }
                                            })
                                            .items(item -> new MessageGroupView(item, store, service));
                                })
                                .elseDo(() -> {
                                    when(store.messages().activeLoadingInitial())
                                            .thenDo(() -> Loader.centered())
                                            .elseDo(() -> {
                                                dynamic(store.messages().activeError(), err -> {
                                                    if (err != null && !err.trim().isEmpty()) {
                                                        column().props(grow(), padding(unit(4)), center(), gap(unit(2)))
                                                                .children(() -> {
                                                                    icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(Color.scarlet));
                                                                    text(Core.bundle.get(
                                                                            "feature.chat.ui.error.messages",
                                                                            "Failed to load messages."))
                                                                            .props(color(Color.scarlet), fontScale(1.0f), wrap(true), center());
                                                                    text(err).props(color(Color.gray), fontScale(0.85f), wrap(true), center());
                                                                    button(Core.bundle.get("feature.chat.ui.retry",
                                                                            "Retry"), () -> {
                                                                                String activeId = store.channels()
                                                                                        .currentActiveId();
                                                                                if (activeId != null
                                                                                        && service != null) {
                                                                                    service.loadMessages(activeId);
                                                                                }
                                                                            })
                                                                            .props(style(WebStyles.secondary()), height(unit(10)), prefHeight(unit(10)))
                                                                            .children(() -> {
                                                                                icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                                                                text(Core.bundle.get(
                                                                                        "feature.chat.ui.retry",
                                                                                        "Retry"));
                                                                            });
                                                                });
                                                    } else {
                                                        column().props(padding(unit(4)), top(), left()).children(() -> {
                                                            text(Core.bundle.get("feature.chat.ui.empty-messages",
                                                                    "No messages yet."))
                                                                    .props(color(Color.gray), fontScale(0.9f), left());
                                                        });
                                                    }
                                                }).props(grow());
                                            })
                                            .props(grow());
                                })
                                .props(grow());
                    }).props(grow());
                }
            }).props(grow());
        }).element();
    }

    public void scrollToBottom() {
        Core.app.post(() -> {
            if (virtualList != null && virtualList.pane() != null) {
                ScrollPane pane = virtualList.pane();
                pane.layout();
                pane.setScrollYForce(pane.getMaxY());
                pane.updateVisualScroll();
            }
        });
    }

    static class MessageGroupView extends Component {
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
            String authorId = group.getAuthorId();

            Readable<UserData> user = store.users().get(authorId);
            Readable<String> authorName = user.map(u -> (u != null && u.getName() != null && !u.getName().isEmpty())
                    ? u.getName()
                    : (authorId != null ? authorId : "Unknown"));

            Readable<Color> authorColor = user.map(u -> {
                if (u == null) {
                    return Color.white;
                }

                Optional<SimpleRole> role = u.getHighestRole();
                if (role.isPresent()) {
                    try {
                        String hex = u.getHighestRole().get().getColor();
                        if (hex != null && !hex.isEmpty()) {
                            return Color.valueOf(hex);
                        }
                    } catch (Exception ignored) {
                    }
                }
                return Color.white;
            });

            Readable<String> avatarUrl = user
                    .map(u -> (u != null && u.getImageUrl() != null && !u.getImageUrl().isEmpty())
                            ? u.getImageUrl()
                            : null);

            String timeStr = formatTime(group.getCreatedAt());

            return row()
                    .props(growX(), top(), left(), gap(ChatMessageHeightCalculator.AVATAR_GAP), padding(ChatMessageHeightCalculator.UNIT_1))
                    .children(() -> {

                        new ChatAvatar(authorName, avatarUrl, authorId, ChatMessageHeightCalculator.AVATAR_SIZE);

                        column().props(growX(), top(), left()).children(() -> {

                            row().props(growX(), top(), left(), gap(unit(2)),
                                    height(ChatMessageHeightCalculator.HEADER_HEIGHT
                                            + ChatMessageHeightCalculator.HEADER_GAP), prefHeight(ChatMessageHeightCalculator.HEADER_HEIGHT
                                            + ChatMessageHeightCalculator.HEADER_GAP))
                                    .children(() -> {
                                        text(authorName)
                                                .props(color(authorColor), left());

                                        if (!timeStr.isEmpty()) {
                                            text(timeStr)
                                                    .props(color(Color.gray), fontScale(0.8f), marginLeft(unit(1)), left());
                                        }

                                        spacer();

                                        Button actionsButton = button(() -> {
                                        });
                                        actionsButton
                                                .onClick(() -> openActions(first, actionsButton.element()))
                                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)))
                                                .children(() -> icon(FileIcon.of("ellipsis-vertical.png"))
                                                        .props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                                    });

                            column().props(growX(), top(), left()).children(() -> {
                                List<ParsedChatMessage> messages = group.getMessages();
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

            Readable<Boolean> isPending = store.delivery().isPending(msgId);
            Readable<Boolean> isFailed = store.delivery().isFailed(msgId);

            Card card = card().props(growX(), top(), left());

            card.props(onClick(() -> openActions(parsed, card.element())));
            if (hasPrevious) {
                card.props(marginTop(ChatMessageHeightCalculator.MESSAGE_GAP));
            }

            card.children(() -> {
                row().props(growX(), top(), left())
                        .children(() -> {
                            if (mentioned) {
                                divider(Direction.Y).props(color(Pal.accent), width(unit(1)), marginRight(unit(1)));
                            }

                            column().props(growX(), top(), left()).children(() -> {
                                if (raw.getReplyTo() != null && !raw.getReplyTo().isEmpty()) {
                                    buildReplyPreview(raw.getReplyTo());
                                    column().props(growX(), marginTop(ChatMessageHeightCalculator.REPLY_GAP), top(), left())
                                            .children(() -> {
                                                buildMessageBody(parsed, isPending, isFailed);
                                            });
                                } else {
                                    buildMessageBody(parsed, isPending, isFailed);
                                }
                            });
                        });
            });
        }

        private void openActions(ParsedChatMessage message, @Nullable Element anchor) {
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
            List<ChatMessage> list = store.messages().currentActive();
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
            row().props(growX(), height(ChatMessageHeightCalculator.REPLY_PREVIEW_HEIGHT), prefHeight(ChatMessageHeightCalculator.REPLY_PREVIEW_HEIGHT), top(), left()).children(() -> {
                icon(Icon.rightSmall).props(size(unit(4), unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(Color.gray), marginRight(unit(1)));
                text(displaySnippet)
                        .props(color(Color.gray), fontScale(0.8f), ellipsis(true), left());
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
                buildRoomInviteCard(invite.getConnectLink());
                return;
            }

            if (parsed instanceof ImageMessage) {
                ImageMessage img = (ImageMessage) parsed;
                final String imageUrl = img.getImageUrl();
                column().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                    text(imageUrl).props(color(Color.lightGray), fontScale(0.85f), wrap(true), left(), growX());
                    networkImage(imageUrl)
                            .props(onClick(() -> new ChatImagePreviewDialog(imageUrl).show()), stopClickPropagation(true),
                                    placeholder(Icon.image), fallback(Icon.cancel), size(unit(40), unit(30)), top(), left());
                });
                return;
            }

            if (parsed instanceof MindustryToolLinkMessage) {
                MindustryToolLinkMessage toolLink = (MindustryToolLinkMessage) parsed;
                final String fullUrl = toolLink.getFullUrl();
                final String type = toolLink.getType();
                final String itemId = toolLink.getItemId();

                card().props(growX(), height(ChatMessageHeightCalculator.TOOL_LINK_CARD_HEIGHT), prefHeight(ChatMessageHeightCalculator.TOOL_LINK_CARD_HEIGHT), top(), left()).children(() -> {
                    column().props(grow(), padding(unit(1.5f)), top(), left(), gap(unit(1))).children(() -> {
                        row().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                            icon("maps".equals(type) ? Icon.map : Icon.paste).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(Pal.accent));
                            text(("maps".equals(type) ? "Map: " : "Schematic: ") + itemId)
                                    .props(color(Pal.accent), fontScale(0.95f), ellipsis(true), left());
                        });
                        row().props(top(), left(), gap(unit(1))).children(() -> {
                            button(Core.bundle.get("button.copy", "Copy Link"), () -> {
                                Core.app.setClipboardText(fullUrl);
                                Vars.ui.showInfoFade(Core.bundle.get("feature.chat.ui.copied", "Copied!"));
                            }).props(style(Styles.defaultt), height(unit(7)), prefHeight(unit(7)));

                            button(Core.bundle.get("feature.chat.ui.view-web", "View in Browser"), () -> {
                                Core.app.openURI(fullUrl);
                            }).props(style(Styles.defaultt), height(unit(7)), prefHeight(unit(7)));
                        });
                    });
                });
                return;
            }

            if (parsed instanceof CommandMessage) {
                buildCommandCard(((CommandMessage) parsed).getKind());
                return;
            }

            if (parsed instanceof SchematicMessage) {
                SchematicMessage schemMsg = (SchematicMessage) parsed;
                if (schemMsg.getPrefixText() != null && !schemMsg.getPrefixText().isEmpty()) {
                    text(schemMsg.getPrefixText()).props(color(bodyColor), fontScale(1.0f), wrap(true), left(), growX());
                }

                buildSchematicCard(schemMsg.getSchematic());

                if (schemMsg.getSuffixText() != null && !schemMsg.getSuffixText().isEmpty()) {
                    text(schemMsg.getSuffixText()).props(color(bodyColor), fontScale(1.0f), wrap(true), left(), growX());
                }
                return;
            }

            if (parsed instanceof TextMessage) {
                TextMessage txt = (TextMessage) parsed;
                final String translatedId = parsed.getId();

                Readable<TranslationViewState> viewState = new Computed<>(() -> {
                    String tr = store.translations().get(translatedId).get();
                    boolean orig = Boolean.TRUE.equals(store.translations().isShowingOriginal(translatedId).get());
                    String currentTranslating = store.ui().translatingMessageId().get();
                    boolean isTranslating = currentTranslating != null && currentTranslating.equals(translatedId);
                    return new TranslationViewState(tr, orig, isTranslating);
                });

                dynamic(viewState, state -> {
                    if (state != null && state.translated != null && !state.translated.trim().isEmpty()) {
                        String textToDisplay = state.showOriginal ? txt.getText() : state.translated;
                        column().props(growX(), top(), left()).children(() -> {
                            buildMessageText(textToDisplay, bodyColor);
                            buildTranslationToggleChip(translatedId, state.showOriginal);
                        });
                    } else {
                        column().props(growX(), top(), left()).children(() -> {
                            buildMessageText(txt.getText(), bodyColor);
                            if (state != null && state.translating) {
                                row().props(growX(), marginTop(unit(1)), top(), left(), gap(unit(1))).children(() -> {
                                    icon(FileIcon.of("translate.png", Icon.refresh))
                                            .props(size(unit(3.5f), unit(3.5f)), prefWidth(unit(3.5f)), prefHeight(unit(3.5f)), color(Color.lightGray));
                                    text(Core.bundle.get("feature.chat.ui.translating", "Translating..."))
                                            .props(color(Color.lightGray), fontScale(0.8f), left());
                                });
                            }
                        });
                    }
                }).props(growX());
                return;
            }

            String fallback = parsed.getContent() != null ? parsed.getContent() : "";
            text(fallback)
                    .props(color(bodyColor), fontScale(1.0f), left(), wrap(true), growX());
        }

        private void buildTranslationToggleChip(String messageId, boolean showingOriginal) {
            button(() -> {
                store.translations().toggleOriginal(messageId);
                ChatMessageHeightCalculator.clearCache();
            })
                    .props(style(WebStyles.ghost()), padding(unit(0.5f), unit(1.5f), unit(0.5f), unit(1.5f)), height(unit(6)), prefHeight(unit(6)), marginTop(unit(1)), left())
                    .children(() -> {
                        row().props(gap(unit(1)), center()).children(() -> {
                            icon(FileIcon.of("translate.png", Icon.refresh))
                                    .props(size(unit(3.5f), unit(3.5f)), prefWidth(unit(3.5f)), prefHeight(unit(3.5f)), color(Pal.accent));
                            text(showingOriginal
                                    ? Core.bundle.get("feature.chat.ui.show-translated", "Show translation")
                                    : Core.bundle.get("feature.chat.ui.show-original", "Translated • Show original"))
                                    .props(color(Pal.accent), fontScale(0.8f), left());
                        });
                    });
        }

        private static final class TranslationViewState {
            final @Nullable String translated;
            final boolean showOriginal;
            final boolean translating;

            TranslationViewState(@Nullable String translated, boolean showOriginal, boolean translating) {
                this.translated = translated;
                this.showOriginal = showOriginal;
                this.translating = translating;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof TranslationViewState))
                    return false;
                TranslationViewState that = (TranslationViewState) o;
                return showOriginal == that.showOriginal &&
                        translating == that.translating &&
                        Objects.equals(translated, that.translated);
            }

            @Override
            public int hashCode() {
                return Objects.hash(translated, showOriginal, translating);
            }
        }

        private void buildRoomInviteCard(String link) {
            PlayerConnectFeature pc = FeatureManager.getFeature(PlayerConnectFeature.class);
            Readable<PlayerConnectRoom> roomSignal = (pc != null && pc.isEnabled())
                    ? pc.getRooms().map(rooms -> findRoomByLink(rooms, link))
                    : Signal.of(null);

            dynamic(roomSignal, room -> {
                if (room != null) {
                    new RoomCard(room, false);
                } else {
                    buildFallbackRoomCardContent(link);
                }
            })
                    .props(height(ChatMessageHeightCalculator.INVITE_CARD_HEIGHT), prefHeight(ChatMessageHeightCalculator.INVITE_CARD_HEIGHT), growX())
                    .name("pc-card-wrapper");
        }

        private Component buildFallbackRoomCardContent(String link) {
            return card()
                    .name("pc-card-fallback")
                    .props(background(Styles.black8), border(1.5f, Color.darkGray), gap(unit(1.5f)), padding(unit(2)), left(), grow())
                    .children(() -> {

                        row().props(growX(), height(unit(6)), prefHeight(unit(6)), gap(unit(2))).children(() -> {
                            text(Core.bundle.get("feature.chat.ui.room-invite", "Room Invite"))
                                    .props(style(Styles.outlineLabel), fontScale(0.95f), ellipsis(true), growX(), left());
                            spacer();
                        });

                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            text(Core.bundle.get("feature.chat.ui.unlisted-offline", "Unlisted or offline"))
                                    .props(color(Color.scarlet), fontScale(0.9f), growX(), ellipsis(true), left());
                        });

                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            text(link.replace(PlayerConnectFeature.PLAYER_CONNECT_PROTOCOL, ""))
                                    .props(color(Color.lightGray), fontScale(0.9f), growX(), ellipsis(true), left());
                        });

                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            text(Core.bundle.get("feature.player-connect.title", "Player Connect"))
                                    .props(color(Color.gray), fontScale(0.85f), growX(), ellipsis(true), left());
                        });

                        row().props(gap(unit(1)), growX(), height(unit(11)), prefHeight(unit(11))).children(() -> {

                            PlayerConnectFeature connect = FeatureManager.getFeature(PlayerConnectFeature.class);
                            Readable<Boolean> connectEnabled = connect != null
                                    ? connect.enabled()
                                    : Readable.of(false);
                            when(connectEnabled)
                                    .thenDo(() -> button(Core.bundle.get("feature.chat.ui.try-connect", "Try Connect"),
                                            () -> promptDirectJoin(link))
                                            .props(style(WebStyles.secondary()), growX(), height(unit(11)), prefHeight(unit(11))))
                                    .elseDo(() -> button(
                                            Core.bundle.get("feature.chat.ui.enable-player-connect",
                                                    "Enable Player Connect"),
                                            () -> FeatureManager.getFeature(PlayerConnectFeature.class).enable())
                                            .props(style(WebStyles.secondary()), growX(), height(unit(11)), prefHeight(unit(11))))
                                    .props(growX());

                            button(() -> {
                                Core.app.setClipboardText(link);
                                Vars.ui.showInfoFade("@copied");
                            })
                                    .props(style(WebStyles.outline()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)))
                                    .children(() -> icon(Icon.copy).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
                        });
                    });
        }

        private void promptDirectJoin(String link) {
            try {
                PlayerConnectLink parsed = PlayerConnectLink.fromString(link);
                PlayerConnectClient.join(parsed, "", () -> {
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage() != null ? e.getMessage() : "Failed to parse link");
            }
        }

        private static @Nullable PlayerConnectRoom findRoomByLink(@Nullable List<PlayerConnectRoom> list, String link) {
            if (list == null || list.isEmpty() || link == null) {
                return null;
            }

            String trimmed = link.trim();
            for (PlayerConnectRoom room : list) {
                if (room != null && trimmed.equals(room.getLink())) {
                    return room;
                }
            }
            try {
                PlayerConnectLink parsed = PlayerConnectLink.fromString(trimmed);
                if (parsed != null && parsed.roomId != null) {
                    for (PlayerConnectRoom room : list) {
                        if (room != null && parsed.roomId.equals(room.getRoomId())) {
                            return room;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        private void buildSchematicCard(Schematic schematic) {
            card().props(height(ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT), prefHeight(ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT), top(), left()).children(() -> {
                column().props(grow(), top(), left(), gap(unit(1))).children(() -> {

                    row().props(growX(), height(unit(6)), prefHeight(unit(6)), top(), left(), gap(unit(1))).children(() -> {
                        text(schematic.name())
                                .props(color(Pal.accent), fontScale(0.95f), ellipsis(true), growX(), left());

                        spacer();
                    });

                    float ratio = schematic.height > 0 ? (float) schematic.width / (float) schematic.height : 1f;
                    float width = Math.max(unit(10), ratio * unit(35));

                    button(() -> useSchematic(schematic))
                            .props(style(Styles.flatt), height(unit(35)), prefHeight(unit(35)), width(width), prefWidth(width))
                            .children(() -> {
                                if (Vars.schematics != null) {
                                    arc(new SchematicImage(schematic).setScaling(Scaling.fit));
                                } else {
                                    icon(Icon.paste).props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)));
                                }
                            });

                    row().props(growX(), height(unit(7)), prefHeight(unit(7)), top(), left(), gap(unit(1))).children(() -> {
                        button(() -> Vars.ui.schematics.showInfo(schematic))
                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), tooltip(Core.bundle.get("info.title", "Info")))
                                .children(() -> icon(FileIcon.of("info.png")).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                        button(() -> Vars.ui.schematics.showExport(schematic))
                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), tooltip(Core.bundle.get("editor.export")))
                                .children(() -> icon(Icon.upload).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                        button(() -> Vars.ui.schematics.showEdit(schematic))
                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), tooltip(Core.bundle.get("schematic.edit")))
                                .children(() -> icon(Icon.pencil).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                        button(() -> useSchematic(schematic))
                                .props(style(Styles.clearNonei), size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), tooltip(Core.bundle.get("feature.chat.ui.schematic.use", "Use")))
                                .children(() -> icon(Icon.play).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
                    });
                });
            });
        }

        private void buildCommandCard(CommandKind kind) {
            boolean schematic = kind == CommandKind.SCHEMATIC;
            String title = schematic
                    ? Core.bundle.get("feature.chat.ui.command.schematic.title", "Browse Schematics")
                    : Core.bundle.get("feature.chat.ui.command.map.title", "Browse Maps");
            String openLabel = schematic
                    ? Core.bundle.get("feature.chat.ui.command.schematic.open", "Open Schematic Browser")
                    : Core.bundle.get("feature.chat.ui.command.map.open", "Open Map Browser");

            card().name("command-card").props(growX(), height(ChatMessageHeightCalculator.COMMAND_CARD_HEIGHT), prefHeight(ChatMessageHeightCalculator.COMMAND_CARD_HEIGHT), top(), left())
                    .children(() -> {
                        column().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                            row().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                                icon(schematic ? Icon.paste : Icon.map).props(size(unit(5), unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(Pal.accent));
                                text(title)
                                        .props(color(Pal.accent), ellipsis(true), left());
                            });
                            row().props(growX(), top(), left()).children(() -> {
                                button(openLabel, () -> openCommandBrowser(kind))
                                        .props(style(WebStyles.secondary()), growX(), height(unit(11)), prefHeight(unit(11)));
                            });
                        });
                    });
        }

        private void openCommandBrowser(CommandKind kind) {
            if (kind == CommandKind.SCHEMATIC) {
                SchematicBrowserFeature feature = FeatureManager.getFeature(SchematicBrowserFeature.class);
                if (feature != null) {
                    feature.showDialog();
                }
            } else {
                MapBrowserFeature feature = FeatureManager.getFeature(MapBrowserFeature.class);
                if (feature != null) {
                    feature.showDialog();
                }
            }
        }

        private Component buildMessageText(String messageText, Readable<Color> bodyColor) {
            if (messageText == null || messageText.isEmpty()) {
                return text("").props(color(bodyColor), fontScale(1.0f), left(), wrap(true), growX());
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
                return text(messageText).props(color(bodyColor), fontScale(1.0f), left(), wrap(true), growX());
            }

            if (urls.size() == 1 && messageText.trim().equals(urls.get(0))) {
                final String onlyUrl = urls.get(0);
                Text linkText = text(onlyUrl)
                        .props(color(WebStyles.Colors.PRIMARY), fontScale(1.0f), left(), wrap(true), growX());
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
            return column().props(growX(), top(), left()).children(() -> {
                text(highlighted).props(color(bodyColor), fontScale(1.0f), left(), wrap(true), growX());

                row().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                    for (String linkUrl : linkUrls) {
                        final String chipUrl = linkUrl;
                        Text chip = text(chipUrl)
                                .props(color(WebStyles.Colors.PRIMARY), fontScale(0.85f), ellipsis(true), left());
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
            SolimDialog dialog = new SolimDialog(Core.bundle.get("feature.chat.link.dialog.title"));
            dialog.addCloseButton();
            dialog.maxWidth(400f);
            dialog.children(() -> {
                column().props(growX(), padding(unit(3)), gap(unit(2))).children(() -> {
                    text(Core.bundle.format("feature.chat.link.dialog.prompt", url))
                            .props(wrap(true), growX(), left());
                    row().props(gap(unit(1))).children(() -> {
                        button(Core.bundle.get("feature.chat.link.dialog.cancel"), dialog::hide)
                                .props(style(Styles.defaultt), height(unit(7)), prefHeight(unit(7)));
                        button(Core.bundle.get("feature.chat.link.dialog.open"), () -> {
                            Core.app.openURI(url);
                            dialog.hide();
                        }).props(style(Styles.defaultb), height(unit(7)), prefHeight(unit(7)));
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
                    Vars.ui.showInfo(Core.bundle.get("schematic.disabled"));
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