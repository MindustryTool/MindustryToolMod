package mindustrytool.features.chat;

import arc.util.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.SchematicMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.services.auth.MindustryAuthProvider;

/**
 * Parser that categorizes raw {@link ChatMessage} instances into typed {@link ParsedChatMessage}
 * domain models, caching results to ensure regexes and base64 schematic decoding execute only once.
 */
public final class ChatMessageParser {

    private static final Pattern MINDUSTRY_TOOL_LINK_PATTERN = Pattern
            .compile("https?://[^/]+/(?:[^/]+/)?(schematics|maps)/([a-zA-Z0-9_-]+)");
    private static final Pattern IMAGE_URL_PATTERN = Pattern
            .compile("^https?://.*\\.(?:png|jpg|jpeg|gif|webp)(?:\\?.*)?$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, ParsedChatMessage> CACHE = new ConcurrentHashMap<>();

    private ChatMessageParser() {
    }

    /**
     * Parses a raw chat message into a strongly-typed {@link ParsedChatMessage}.
     * Results are cached by message ID when present.
     */
    public static ParsedChatMessage parse(@Nullable ChatMessage message) {
        if (message == null) {
            return new TextMessage(new ChatMessage(), "", false);
        }
        String id = message.getId();
        if (id != null && !id.isEmpty()) {
            ParsedChatMessage cached = CACHE.get(id);
            if (cached != null) {
                return cached;
            }
        }

        ParsedChatMessage parsed = parseInternal(message);
        if (id != null && !id.isEmpty()) {
            CACHE.put(id, parsed);
        }
        return parsed;
    }

    private static ParsedChatMessage parseInternal(ChatMessage message) {
        String content = message.getContent() != null ? message.getContent().trim() : "";

        // 1. Room invite
        if (content.startsWith("player-connect://")) {
            return new RoomInviteMessage(message, content);
        }

        // 2. Direct network image
        if (IMAGE_URL_PATTERN.matcher(content).matches()) {
            return new ImageMessage(message, content);
        }

        // 3. MindustryTool web links (maps or schematics)
        Matcher matcher = MINDUSTRY_TOOL_LINK_PATTERN.matcher(content);
        if (matcher.find()) {
            String fullUrl = matcher.group(0);
            String type = matcher.group(1);
            String itemId = matcher.group(2);
            return new MindustryToolLinkMessage(message, fullUrl, type, itemId);
        }

        // 4. Mindustry base64 schematic
        if (Vars.schematicBaseStart != null) {
            int schemPos = content.indexOf(Vars.schematicBaseStart);
            if (schemPos != -1) {
                int endPos = content.indexOf(" ", schemPos);
                if (endPos == -1) {
                    endPos = content.length();
                }

                String prev = content.substring(0, schemPos).trim();
                String base64 = content.substring(schemPos, endPos).trim();
                String after = content.substring(endPos).trim();

                Schematic schematic = null;
                try {
                    schematic = Schematics.readBase64(base64);
                } catch (Throwable ignored) {
                }

                if (schematic != null) {
                    return new SchematicMessage(
                            message,
                            schematic,
                            prev.isEmpty() ? null : prev,
                            after.isEmpty() ? null : after
                    );
                }
            }
        }

        // 5. Standard text message
        boolean mentioned = isMentioningCurrentUser(content);
        return new TextMessage(message, content, mentioned);
    }

    public static boolean isMentioningCurrentUser(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        try {
            var session = MindustryAuthProvider.getInstance().getSession();
            if (session == null || session.getName() == null || session.getName().isEmpty()) {
                return false;
            }
            return content.toLowerCase().contains(("@" + session.getName()).toLowerCase());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
