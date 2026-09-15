package mindustrytool.features.chat;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.pooling.Pools;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.ui.Fonts;
import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.SchematicMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;

/**
 * Calculates and caches layout heights for chat message items given a container width.
 *
 * <p>Uses static heights for fixed layout structures (headers, avatars, cards, buttons)
 * and calculates wrapped text height using {@link GlyphLayout}.
 */
public final class ChatMessageHeightCalculator {

    // Fixed layout unit constants (matching UI.unit(1) = 4f)
    public static final float UNIT_1 = 4f;
    public static final float AVATAR_SIZE = 48f; // unit(12)
    public static final float AVATAR_GAP = 6f;   // unit(1.5f)
    public static final float HEADER_HEIGHT = 24f; // unit(6) ellipsis button and title row
    public static final float REPLY_PREVIEW_HEIGHT = 24f;
    public static final float REPLY_GAP = 2f; // unit(0.5f): gap between reply preview and message body
    public static final float SCHEMATIC_CARD_HEIGHT = 212f;
    public static final float IMAGE_CARD_HEIGHT = 140f;
    public static final float INVITE_CARD_HEIGHT = 108f; // unit(27)
    public static final float TOOL_LINK_CARD_HEIGHT = 70f;
    public static final float HORIZONTAL_PADDINGS = 70f; // 4px outer left + 48px avatar + 6px gap + 4px inner left + 4px inner right + 4px outer right
    public static final float MENTION_EXTRA_PADDING = 8f; // unit(1) divider width + unit(1) gap
    public static final float MESSAGE_GAP = 3f; // unit(0.75f): gap between messages in a group
    public static final float HEADER_GAP = 2f; // unit(0.5f): gap between group header and first message
    public static final float MESSAGE_CARD_PADDING = 8f; // vertical padding for message item card (4f top + 4f bottom)
    public static final float FONT_SCALE = 1.0f;

    private static final Map<String, Float> HEIGHT_CACHE = new ConcurrentHashMap<>();

    private ChatMessageHeightCalculator() {
    }

    /**
     * Calculates the height for a message group at the specified container width.
     */
    public static float calculateHeight(MessageGroup group, float containerWidth) {
        if (group == null || group.getMessageCount() == 0) {
            return Scl.scl(24f);
        }

        int widthKey = (int) Math.max(100f, containerWidth);
        int scaleKey = (int) (Scl.scl() * 100);
        String cacheKey = group.getKey() + "_" + widthKey + "_" + scaleKey;
        Float cached = HEIGHT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        float height = computeHeight(group, widthKey);
        HEIGHT_CACHE.put(cacheKey, height);
        return height;
    }

    private static float computeHeight(MessageGroup group, float containerWidth) {
        float verticalPadding = Scl.scl(UNIT_1 + UNIT_1);

        float messagesHeight = 0f;
        for (ParsedChatMessage msg : group.getMessages()) {
            messagesHeight += measureMessageHeight(msg, containerWidth);
        }
        if (group.getMessageCount() > 1) {
            messagesHeight += Scl.scl(MESSAGE_GAP) * (group.getMessageCount() - 1);
        }

        float rightColumnHeight = Scl.scl(HEADER_HEIGHT + HEADER_GAP) + messagesHeight;
        float total = Math.max(Scl.scl(AVATAR_SIZE), rightColumnHeight) + verticalPadding;
        return Math.max(Scl.scl(24f), total);
    }

    private static float measureMessageHeight(ParsedChatMessage msg, float containerWidth) {
        float height = Scl.scl(MESSAGE_CARD_PADDING);

        boolean mentioned = (msg instanceof TextMessage) && ((TextMessage) msg).isMentionsCurrentUser();
        float availableTextWidth = Math.max(20f, containerWidth - Scl.scl(HORIZONTAL_PADDINGS) - (mentioned ? Scl.scl(MENTION_EXTRA_PADDING) : 0f));

        // Reply preview row
        if (msg.getReplyTo() != null && !msg.getReplyTo().isEmpty()) {
            height += Scl.scl(REPLY_PREVIEW_HEIGHT + REPLY_GAP);
        }

        // Body height by type
        if (msg instanceof TextMessage) {
            TextMessage txt = (TextMessage) msg;
            height += measureTextHeight(txt.getText(), availableTextWidth, FONT_SCALE);
        } else if (msg instanceof SchematicMessage) {
            SchematicMessage schem = (SchematicMessage) msg;
            height += Scl.scl(SCHEMATIC_CARD_HEIGHT);
            if (schem.getPrefixText() != null && !schem.getPrefixText().isEmpty()) {
                height += measureTextHeight(schem.getPrefixText(), availableTextWidth, FONT_SCALE);
            }
            if (schem.getSuffixText() != null && !schem.getSuffixText().isEmpty()) {
                height += measureTextHeight(schem.getSuffixText(), availableTextWidth, FONT_SCALE);
            }
        } else if (msg instanceof ImageMessage) {
            height += Scl.scl(IMAGE_CARD_HEIGHT);
        } else if (msg instanceof RoomInviteMessage) {
            height += Scl.scl(INVITE_CARD_HEIGHT);
        } else if (msg instanceof MindustryToolLinkMessage) {
            height += Scl.scl(TOOL_LINK_CARD_HEIGHT);
        } else {
            height += Scl.scl(24f);
        }
        return height;
    }

    /**
     * Measures the height of wrapped text using Arc's {@link GlyphLayout}.
     */
    public static float measureTextHeight(@Nullable String text, float wrapWidth, float fontScale) {
        Font font = null;
        try {
            font = Fonts.def;
        } catch (Throwable ignored) {
        }

        if (text == null || text.trim().isEmpty()) {
            if (font != null) {
                return (font.getCapHeight() - font.getDescent() * 2f) * fontScale;
            }
            return Scl.scl(18f) * fontScale;
        }

        if (font != null) {
            try {
                GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                float oldScaleX = font.getScaleX();
                float oldScaleY = font.getScaleY();
                font.getData().setScale(oldScaleX * fontScale, oldScaleY * fontScale);

                layout.setText(font, text, Color.white, wrapWidth, Align.left, true);
                float h = layout.height;
                // Match Arc's Label.getPrefHeight() which accounts for line descent spacing (-2 * descent)
                float descentCorrection = -font.getDescent() * 2f;

                font.getData().setScale(oldScaleX, oldScaleY);
                Pools.free(layout);
                return h + descentCorrection;
            } catch (Throwable ignored) {
            }
        }

        // Fallback for headless tests where Fonts.def is not initialized
        float charsPerLine = Math.max(10f, wrapWidth / (9f * fontScale * Scl.scl()));
        int estimatedLines = (int) Math.ceil(text.length() / charsPerLine);
        return Math.max(Scl.scl(18f) * fontScale, estimatedLines * (Scl.scl(20f) * fontScale));
    }

    public static void clearCache() {
        HEIGHT_CACHE.clear();
    }
}
