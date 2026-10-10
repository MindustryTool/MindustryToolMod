package mindustrytool.features.translation;

import arc.util.Nullable;
import arc.util.Strings;
import java.util.regex.Pattern;

/**
 * Intelligent filter that identifies messages that should bypass translation
 * (such as system notices, bot bridges, coordinates, schematics, and universal acronyms).
 */
public final class ChatFilter {

    private static final Pattern SYSTEM_OR_BOT_PREFIX = Pattern.compile(
            "^(?:\\[|<|\\()\\s*(?:server|system|host|admin|discord|irc|bridge|bot|telegram|tg|console)\\s*(?:\\]|>|\\))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COORDINATES_PATTERN = Pattern.compile(
            "^(?:x\\s*[:=]\\s*-?\\d+\\s*,?\\s*y\\s*[:=]\\s*-?\\d+|\\(\\s*-?\\d+\\s*[, ]\\s*-?\\d+\\s*\\)|-?\\d+\\s*[, ]\\s*-?\\d+)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?:https?://|www\\.)\\S+$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHEMATIC_BASE64 = Pattern.compile(
            "^bXN[a-zA-Z0-9+/=_-]{20,}$");

    private static final Pattern NUMBERS_ONLY = Pattern.compile(
            "^-?\\d+(?:[.,]\\d+)?$");

    private static final Pattern SYSTEM_EVENT_PHRASES = Pattern.compile(
            "(?:has connected|has disconnected|đã kết nối|đã ngắt kết nối|joined the game|left the game|server restarting|game over|next wave in)",
            Pattern.CASE_INSENSITIVE);

    // Universal gamer acronyms that need no translation
    private static final String[] UNIVERSAL_ACRONYMS = {
            "gg", "ggwp", "glhf", "gl", "hf", "ty", "np", "thx", "pls", "plz", "wp", "afk", "brb", "omg", "wtf", "xd", "lol"
    };

    private ChatFilter() {
    }

    /**
     * Determines whether an incoming multiplayer chat message should be translated.
     */
    public static boolean shouldTranslateIncoming(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        String clean = Strings.stripColors(raw).trim();
        if (clean.isEmpty()) {
            return false;
        }

        // Check if the overall message is a system/bot message or broadcast event
        if (isSystemOrBotMessage(clean) || SYSTEM_EVENT_PHRASES.matcher(clean).find()) {
            return false;
        }

        // Extract content after sender prefix (e.g. "PlayerName: hello")
        String content = extractMessageContent(clean);
        if (content.isEmpty()) {
            return false;
        }

        return !isNoise(content);
    }

    /**
     * Determines whether an outgoing chat message typed by the player should be translated.
     */
    public static boolean shouldTranslateOutgoing(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return false;
        }

        // Escape prefix: //message bypasses translation
        if (text.startsWith("//")) {
            return false;
        }

        // Forced translation commands: /tr <text> or /dich <text>
        if (text.startsWith("/tr ") || text.startsWith("/dich ")) {
            String sub = text.startsWith("/tr ") ? text.substring(4).trim() : text.substring(6).trim();
            return !sub.isEmpty();
        }

        // Team chat (/t ) or Admin chat (/a ) can be translated
        if (text.startsWith("/t ") || text.startsWith("/a ")) {
            String sub = text.substring(3).trim();
            return !sub.isEmpty() && !isNoise(sub);
        }

        // Other commands starting with / are game commands (e.g. /vote, /help)
        if (text.startsWith("/")) {
            return false;
        }

        return !isNoise(text);
    }

    /**
     * Checks if the text starts with a system, bot, or console tag.
     */
    public static boolean isSystemOrBotMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return SYSTEM_OR_BOT_PREFIX.matcher(text.trim()).find();
    }

    /**
     * Checks if the text is pure noise: numbers, punctuation, emoticons, URLs, schematics, or universal acronyms.
     */
    public static boolean isNoise(String text) {
        if (text == null) {
            return true;
        }
        String clean = Strings.stripColors(text).trim();
        if (clean.isEmpty()) {
            return true;
        }

        // Pure numbers
        if (NUMBERS_ONLY.matcher(clean).matches()) {
            return true;
        }

        // Coordinates
        if (COORDINATES_PATTERN.matcher(clean).matches()) {
            return true;
        }

        // URLs
        if (URL_PATTERN.matcher(clean).matches()) {
            return true;
        }

        // Schematics
        if (isSchematic(clean)) {
            return true;
        }

        // Punctuation, symbols, or emoticons only (e.g. ":)", ":D", "^^", "???", "...")
        if (isPunctuationOrEmoticonOnly(clean)) {
            return true;
        }

        // Universal gaming acronyms
        String lower = clean.toLowerCase();
        for (String acr : UNIVERSAL_ACRONYMS) {
            if (lower.equals(acr)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the text matches Mindustry schematic base64 format.
     */
    public static boolean isSchematic(String text) {
        if (text == null || text.length() < 20) {
            return false;
        }
        return SCHEMATIC_BASE64.matcher(text.trim()).matches();
    }

    /**
     * Checks if a string contains only punctuation, math symbols, whitespace, or emoticons.
     */
    public static boolean isPunctuationOrEmoticonOnly(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        boolean hasLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                break;
            }
        }

        if (!hasLetter) {
            return true;
        }

        // Known short letter-based emoticons
        String trimmed = text.trim();
        if (trimmed.length() <= 4) {
            String lower = trimmed.toLowerCase();
            return lower.equals(":d") || lower.equals("xd") || lower.equals(":p") || lower.equals("xp")
                    || lower.equals("d:") || lower.equals("q_q") || lower.equals("t_t") || lower.equals("o_o")
                    || lower.equals("u_u") || lower.equals("o/");
        }

        return false;
    }

    /**
     * Extracts the message body after sender name prefix, e.g. "Player: hello" -> "hello".
     * If no colon is found, returns the clean text.
     */
    public static String extractMessageContent(String cleanText) {
        if (cleanText == null) {
            return "";
        }
        int colonIdx = cleanText.indexOf(':');
        if (colonIdx >= 0 && colonIdx < cleanText.length() - 1) {
            return cleanText.substring(colonIdx + 1).trim();
        }
        return cleanText.trim();
    }
}
