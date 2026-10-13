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
import mindustrytool.features.chat.models.ParsedChatMessage.CommandKind;
import mindustrytool.features.chat.models.ParsedChatMessage.CommandMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.SchematicMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserSession;
import mindustrytool.services.auth.MindustryAuthProvider;

public final class ChatMessageParser {

    private static final Pattern MINDUSTRY_TOOL_LINK_PATTERN = Pattern
            .compile("https?://[^/]+/(?:[^/]+/)?(schematics|maps)/([a-zA-Z0-9_-]+)");
    private static final Pattern IMAGE_URL_PATTERN = Pattern
            .compile("^https?://.*\\.(?:png|jpg|jpeg|gif|webp)(?:\\?.*)?$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, ParsedChatMessage> CACHE = new ConcurrentHashMap<>();

    private ChatMessageParser() {
    }

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

        CommandKind commandKind = CommandKind.fromToken(content);
        if (commandKind != null) {
            return new CommandMessage(message, commandKind);
        }

        if (PlayerConnectLink.isValid(content)) {
            return new RoomInviteMessage(message, content);
        }

        if (IMAGE_URL_PATTERN.matcher(content).matches()) {
            return new ImageMessage(message, content);
        }

        Matcher matcher = MINDUSTRY_TOOL_LINK_PATTERN.matcher(content);
        if (matcher.find()) {
            String fullUrl = matcher.group(0);
            String type = matcher.group(1);
            String itemId = matcher.group(2);
            return new MindustryToolLinkMessage(message, fullUrl, type, itemId);
        }

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
                            after.isEmpty() ? null : after);
                }
            }
        }

        boolean mentioned = isMentioningCurrentUser(content);
        return new TextMessage(message, content, mentioned);
    }

    public static boolean isMentioningCurrentUser(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        try {
            UserSession session = MindustryAuthProvider.getInstance().getSession();
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
