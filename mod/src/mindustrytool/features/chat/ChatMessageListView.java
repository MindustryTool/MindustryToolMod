package mindustrytool.features.chat;

import static solim.UI.*;

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
import solim.core.BaseComponent;
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

        return column().grow().top().left().gap(unit(1)).padding(unit(2)).children(() -> {
            dynamic(hasChannel, channelSelected -> {
                if (!Boolean.TRUE.equals(channelSelected)) {
                    return dynamic(store.channels().loading(), chanLoading -> {
                        if (Boolean.TRUE.equals(chanLoading)) {
                            return Loader.centered();
                        }

                        return dynamic(store.channels().error(), chanErr -> {
                            if (chanErr != null && !chanErr.trim().isEmpty()) {
                                return column().grow().center().gap(unit(2)).padding(unit(4)).children(() -> {
                                    icon(Icon.warning).size(unit(6)).color(Color.scarlet);
                                    text(Core.bundle.get("feature.chat.ui.channels-failed", "Channels failed to load."))
                                            .color(Color.scarlet)
                                            .fontScale(1.0f)
                                            .wrap()
                                            .center();
                                    text(chanErr).color(Color.gray).fontScale(0.85f).wrap().center();
                                    button(Core.bundle.get("feature.chat.ui.retry-channels", "Retry Channels"), () -> {
                                        if (service != null) {
                                            service.refreshChannels();
                                        }
                                    })
                                            .style(WebStyles.secondary())
                                            .height(unit(10))
                                            .children(() -> {
                                                icon(Icon.refresh).size(unit(4));
                                                text(Core.bundle.get("feature.chat.ui.retry-channels",
                                                        "Retry Channels"));
                                            });
                                });
                            }

                            return column().padding(unit(4)).top().left().children(() -> {
                                text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                        .color(Color.gray)
                                        .fontScale(0.9f)
                                        .left();
                            });
                        }).grow();
                    }).grow();
                }

                return column().grow().top().left().gap(unit(1)).children(() -> {
                    dynamic(showRefreshErrorBanner, show -> {
                        if (Boolean.TRUE.equals(show)) {
                            return card().growX().padding(unit(1.5f)).children(() -> {
                                row().growX().gap(unit(1)).center().children(() -> {
                                    icon(Icon.warning).size(unit(4)).color(Color.scarlet);
                                    text(Core.bundle.get("feature.chat.ui.refresh-failed",
                                            "Failed to refresh messages."))
                                                    .color(Color.scarlet)
                                                    .fontScale(0.85f)
                                                    .growX()
                                                    .left();
                                    button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                                        String activeId = store.channels().currentActiveId();
                                        if (activeId != null && service != null) {
                                            service.loadMessages(activeId);
                                        }
                                    })
                                            .style(WebStyles.secondary())
                                            .height(unit(8))
                                            .children(() -> {
                                                icon(Icon.refresh).size(unit(4));
                                                text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                            });
                                });
                            });
                        }
                        return null;
                    });

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

                    dynamic(store.messages().loadingOlder(), loading -> {
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
                                                String activeId = store.channels().currentActiveId();
                                                List<ChatMessage> msgs = store.messages().currentActive();
                                                if (activeId != null && !activeId.isEmpty() && service != null
                                                        && msgs != null
                                                        && !msgs.isEmpty() && !store.messages().isLoadingOlder()
                                                        && !store.messages().isFullyLoaded(activeId)) {
                                                    service.fetchOlderMessages(activeId);
                                                }
                                            });

                            return virtualList.marginBottom(unit(2)).grow();
                        }

                        return dynamic(store.messages().activeLoadingInitial(), isLoading -> {
                            if (Boolean.TRUE.equals(isLoading)) {
                                return Loader.centered();
                            }

                            return dynamic(store.messages().activeError(), err -> {
                                if (err != null && !err.trim().isEmpty()) {
                                    return column().grow().center().gap(unit(2)).padding(unit(4)).children(() -> {
                                        icon(Icon.warning).size(unit(6)).color(Color.scarlet);
                                        text(Core.bundle.get("feature.chat.ui.error.messages",
                                                "Failed to load messages."))
                                                        .color(Color.scarlet)
                                                        .fontScale(1.0f)
                                                        .wrap()
                                                        .center();
                                        text(err).color(Color.gray).fontScale(0.85f).wrap().center();
                                        button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                                            String activeId = store.channels().currentActiveId();
                                            if (activeId != null && service != null) {
                                                service.loadMessages(activeId);
                                            }
                                        })
                                                .style(WebStyles.secondary())
                                                .height(unit(10))
                                                .children(() -> {
                                                    icon(Icon.refresh).size(unit(4));
                                                    text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                                });
                                    });
                                }

                                return column().padding(unit(4)).top().left().children(() -> {
                                    text(Core.bundle.get("feature.chat.ui.empty-messages", "No messages yet."))
                                            .color(Color.gray)
                                            .fontScale(0.9f)
                                            .left();
                                });
                            }).grow();
                        }).grow();
                    }).grow();
                }).grow();
            }).grow();
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
                                    .gap(unit(2))
                                    .height(ChatMessageHeightCalculator.HEADER_HEIGHT
                                            + ChatMessageHeightCalculator.HEADER_GAP)
                                    .children(() -> {
                                        text(authorName)
                                                .color(authorColor)
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
                                                .onClick(() -> openActions(first, actionsButton.element()))
                                                .style(Styles.clearNonei)
                                                .size(unit(6), unit(6))
                                                .children(() -> icon(FileIcon.of("ellipsis-vertical.png"))
                                                        .size(unit(6), unit(6)));
                                    });

                            // Stacked message rows
                            column().growX().top().left().children(() -> {
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

            Card card = card().growX().top().left();

            card.onClick(() -> openActions(parsed, card.element()));
            if (hasPrevious) {
                card.marginTop(ChatMessageHeightCalculator.MESSAGE_GAP);
            }

            card.children(() -> {
                row().growX().top().left()
                        .children(() -> {
                            if (mentioned) {
                                divider(Direction.Y).color(Pal.accent).width(unit(1)).marginRight(unit(1));
                            }

                            if (raw.getReplyTo() != null && !raw.getReplyTo().isEmpty()) {
                                column().growX().top().left().children(() -> {
                                    buildReplyPreview(raw.getReplyTo());
                                    column().growX().top().left().marginTop(ChatMessageHeightCalculator.REPLY_GAP)
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
            row().growX().top().left().height(ChatMessageHeightCalculator.REPLY_PREVIEW_HEIGHT).children(() -> {
                icon(Icon.rightSmall).size(unit(4), unit(4)).color(Color.gray).marginRight(unit(1));
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
                buildRoomInviteCard(invite.getConnectLink());
                return;
            }

            if (parsed instanceof ImageMessage) {
                ImageMessage img = (ImageMessage) parsed;
                final String imageUrl = img.getImageUrl();
                column().growX().top().left().gap(unit(1)).children(() -> {
                    text(imageUrl).color(Color.lightGray).fontScale(0.85f).wrap().left().growX();
                    networkImage(imageUrl)
                            .onClick(() -> new ChatImagePreviewDialog(imageUrl).show())
                            .stopClickPropagation(true)
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

            if (parsed instanceof CommandMessage) {
                buildCommandCard(((CommandMessage) parsed).getKind());
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
                dynamic(store.translations().get(translatedId), translated -> {
                    if (translated == null || translated.isEmpty()) {
                        return null;
                    }
                    final String translatedText = translated;
                    return column().growX().top().left().marginTop(unit(1)).children(() -> {
                        text(Core.bundle.get("feature.chat.ui.translated-badge", "Translated"))
                                .color(Pal.accent)
                                .fontScale(0.8f)
                                .left();
                        buildMessageText(translatedText, bodyColor);
                    });
                });
                dynamic(store.ui().translatingMessageId(), translatingId -> {
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

        private void buildRoomInviteCard(String link) {
            PlayerConnectFeature pc = FeatureManager.getFeature(PlayerConnectFeature.class);
            Readable<PlayerConnectRoom> roomSignal = (pc != null && pc.isEnabled())
                    ? pc.getRooms().map(rooms -> findRoomByLink(rooms, link))
                    : Signal.of(null);

            dynamic(roomSignal, room -> {
                if (room != null) {
                    return new RoomCard(room, false);
                } else {
                    return buildFallbackRoomCardContent(link);
                }
            })
                    .height(ChatMessageHeightCalculator.INVITE_CARD_HEIGHT)
                    .growX()
                    .name("pc-card-wrapper");
        }

        private Component buildFallbackRoomCardContent(String link) {
            return column()
                    .grow()
                    .gap(unit(1))
                    .children(() -> {
                        text(Core.bundle.get("feature.chat.ui.room-invite", "Room Invite"))
                                .color(Pal.accent)
                                .fontScale(0.95f)
                                .ellipsis()
                                .growX()
                                .left();

                        text(Core.bundle.get("feature.chat.ui.unlisted-offline", "Unlisted or offline"))
                                .color(Color.scarlet)
                                .fontScale(0.85f)
                                .left();

                        text(link.replace(PlayerConnectFeature.PLAYER_CONNECT_PROTOCOL, ""))
                                .color(Color.lightGray).fontScale(0.85f).ellipsis().growX().left();

                        spacer();

                        row().growX().gap(unit(1)).children(() -> {
                            dynamic(FeatureManager.get(PlayerConnectFeature.class).enabled(), enabled -> {
                                if (Boolean.TRUE.equals(enabled)) {
                                    return button(Core.bundle.get("feature.chat.ui.try-connect", "Try Connect"),
                                            () -> promptDirectJoin(link))
                                                    .style(WebStyles.secondary())
                                                    .growX()
                                                    .height(unit(11));
                                }

                                return button(
                                        Core.bundle.get("feature.chat.ui.enable-player-connect",
                                                "Enable Player Connect"),
                                        () -> FeatureManager.getFeature(PlayerConnectFeature.class).enable())
                                                .style(WebStyles.secondary())
                                                .growX()
                                                .height(unit(11));
                            }).growX();
                            button(() -> {
                                Core.app.setClipboardText(link);
                                Vars.ui.showInfoFade("@copied");
                            })
                                    .style(WebStyles.outline())
                                    .size(unit(11))
                                    .children(() -> icon(Icon.copy).size(unit(5)));
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
                                .children(() -> icon(FileIcon.of("info.png")).size(unit(5), unit(5)));

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

        private void buildCommandCard(CommandKind kind) {
            boolean schematic = kind == CommandKind.SCHEMATIC;
            String title = schematic
                    ? Core.bundle.get("feature.chat.ui.command.schematic.title", "Browse Schematics")
                    : Core.bundle.get("feature.chat.ui.command.map.title", "Browse Maps");
            String openLabel = schematic
                    ? Core.bundle.get("feature.chat.ui.command.schematic.open", "Open Schematic Browser")
                    : Core.bundle.get("feature.chat.ui.command.map.open", "Open Map Browser");

            card().name("command-card").growX().top().left().height(ChatMessageHeightCalculator.COMMAND_CARD_HEIGHT)
                    .children(() -> {
                        column().growX().top().left().padding(unit(1.5f)).gap(unit(1)).children(() -> {
                            row().growX().top().left().gap(unit(1)).children(() -> {
                                icon(schematic ? Icon.paste : Icon.map).size(unit(5), unit(5)).color(Pal.accent);
                                text(title)
                                        .color(Pal.accent)
                                        .ellipsis()
                                        .left();
                            });
                            row().growX().top().left().children(() -> {
                                button(openLabel, () -> openCommandBrowser(kind))
                                        .style(WebStyles.secondary())
                                        .growX()
                                        .height(unit(11));
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
