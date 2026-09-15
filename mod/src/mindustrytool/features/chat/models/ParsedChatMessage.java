package mindustrytool.features.chat.models;

import arc.util.Nullable;
import mindustry.game.Schematic;
import mindustrytool.models.response.ChatMessage;

/**
 * Domain model representing a pre-parsed chat message with its resolved payload type.
 */
public abstract class ParsedChatMessage {

    private final ChatMessage raw;

    protected ParsedChatMessage(ChatMessage raw) {
        this.raw = raw;
    }

    public ChatMessage getRaw() {
        return raw;
    }

    public String getId() {
        return raw != null ? raw.getId() : "";
    }

    public @Nullable String getCreatedBy() {
        return raw != null ? raw.getCreatedBy() : null;
    }

    public @Nullable String getCreatedAt() {
        return raw != null ? raw.getCreatedAt() : null;
    }

    public @Nullable String getReplyTo() {
        return raw != null ? raw.getReplyTo() : null;
    }

    public @Nullable String getChannelId() {
        return raw != null ? raw.getChannelId() : null;
    }

    public @Nullable String getContent() {
        return raw != null ? raw.getContent() : null;
    }

    public static class TextMessage extends ParsedChatMessage {
        private final String text;
        private final boolean mentionsCurrentUser;

        public TextMessage(ChatMessage raw, String text, boolean mentionsCurrentUser) {
            super(raw);
            this.text = text;
            this.mentionsCurrentUser = mentionsCurrentUser;
        }

        public String getText() {
            return text;
        }

        public boolean isMentionsCurrentUser() {
            return mentionsCurrentUser;
        }
    }

    public static class SchematicMessage extends ParsedChatMessage {
        private final Schematic schematic;
        private final @Nullable String prefixText;
        private final @Nullable String suffixText;

        public SchematicMessage(
                ChatMessage raw,
                Schematic schematic,
                @Nullable String prefixText,
                @Nullable String suffixText) {
            super(raw);
            this.schematic = schematic;
            this.prefixText = prefixText;
            this.suffixText = suffixText;
        }

        public Schematic getSchematic() {
            return schematic;
        }

        public @Nullable String getPrefixText() {
            return prefixText;
        }

        public @Nullable String getSuffixText() {
            return suffixText;
        }
    }

    public static class ImageMessage extends ParsedChatMessage {
        private final String imageUrl;

        public ImageMessage(ChatMessage raw, String imageUrl) {
            super(raw);
            this.imageUrl = imageUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }

    public static class RoomInviteMessage extends ParsedChatMessage {
        private final String connectLink;

        public RoomInviteMessage(ChatMessage raw, String connectLink) {
            super(raw);
            this.connectLink = connectLink;
        }

        public String getConnectLink() {
            return connectLink;
        }
    }

    public static class MindustryToolLinkMessage extends ParsedChatMessage {
        private final String fullUrl;
        private final String type;
        private final String itemId;

        public MindustryToolLinkMessage(ChatMessage raw, String fullUrl, String type, String itemId) {
            super(raw);
            this.fullUrl = fullUrl;
            this.type = type;
            this.itemId = itemId;
        }

        public String getFullUrl() {
            return fullUrl;
        }

        public String getType() {
            return type;
        }

        public String getItemId() {
            return itemId;
        }
    }
}
