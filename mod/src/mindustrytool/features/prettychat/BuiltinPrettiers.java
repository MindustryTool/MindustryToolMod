package mindustrytool.features.prettychat;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;

import java.util.Locale;

/**
 * Built-in text transformers for Pretty Chat.
 * Fully compatible with Java 8 and Android.
 */
public final class BuiltinPrettiers {

    private BuiltinPrettiers() {
    }

    public static Seq<Prettier> createDefaultPrettiers() {
        Seq<Prettier> list = new Seq<>();
        list.add(new RainbowPrettier());
        list.add(new GradientPrettier());
        list.add(new UwuPrettier());
        list.add(new MockingPrettier());
        list.add(new CapsPrettier());
        list.add(new LowercasePrettier());
        list.add(new ReversePrettier());
        list.add(new SmallCapsPrettier());
        list.add(new BubblePrettier());
        list.add(new MonoPrettier());
        list.add(new StrikethroughPrettier());
        list.add(new UnderlinePrettier());
        list.add(new FancyBracketsPrettier());
        list.add(new CustomPrettier());
        return list;
    }

    /** Rainbow color tags across words with adaptive clustering to respect message length limits. */
    public static class RainbowPrettier implements Prettier {
        private static final String[] COLORS = {
                "[red]", "[gold]", "[lime]", "[cyan]", "[sky]", "[pink]"
        };

        @Override
        public String id() {
            return "rainbow";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.rainbow.name";
        }

        @Override
        public String defaultName() {
            return "Rainbow";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.rainbow.desc";
        }

        @Override
        public String defaultDescription() {
            return "Colors words with dynamic rainbow tags.";
        }

        @Override
        public String category() {
            return "color";
        }

        @Override
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

        @Override
        public String transform(String input, int maxLength) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            if (maxLength <= 0) {
                return input;
            }

            int closingTagLen = 2; // "[]"
            int availableForTags = maxLength - input.length() - closingTagLen;
            if (availableForTags < 5) { // Minimum tag length is 5 ("[red]")
                return input;
            }

            String[] words = input.split(" ");
            if (words.length == 0) {
                return input;
            }
            if (words.length == 1) {
                return COLORS[0] + words[0] + "[]";
            }

            // Calculate maximum number of color tags that fit within available budget
            int k = 0;
            int currentTagCost = 0;
            for (int i = 0; i < words.length; i++) {
                int nextCost = COLORS[i % COLORS.length].length();
                if (currentTagCost + nextCost <= availableForTags) {
                    currentTagCost += nextCost;
                    k++;
                } else {
                    break;
                }
            }

            if (k <= 0) {
                return input;
            }

            // If we can afford a color tag for every word, color every word individually
            if (k >= words.length) {
                StringBuilder sb = new StringBuilder(input.length() + currentTagCost + closingTagLen);
                for (int i = 0; i < words.length; i++) {
                    if (i > 0) {
                        sb.append(' ');
                    }
                    sb.append(COLORS[i % COLORS.length]).append(words[i]);
                }
                sb.append("[]");
                return sb.toString();
            }

            // Otherwise, group words into k clusters so the entire message is preserved
            StringBuilder sb = new StringBuilder(input.length() + currentTagCost + closingTagLen);
            int numWords = words.length;
            for (int cluster = 0; cluster < k; cluster++) {
                int startWord = cluster * numWords / k;
                int endWord = (cluster + 1) * numWords / k;

                if (cluster > 0) {
                    sb.append(' ');
                }
                sb.append(COLORS[cluster % COLORS.length]);
                for (int w = startWord; w < endWord; w++) {
                    if (w > startWord) {
                        sb.append(' ');
                    }
                    sb.append(words[w]);
                }
            }
            sb.append("[]");
            return sb.toString();
        }
    }

    /** Smooth color gradient text transformer with customizable palettes. */
    public static class GradientPrettier implements Prettier {
        public static final String DEFAULT_PALETTE = "#ff5e36,#9b51e0";
        private static final String SETTING_KEY = "mindustrytool.pretty-chat.script.gradient";

        @Override
        public String id() {
            return "gradient";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.gradient.name";
        }

        @Override
        public String defaultName() {
            return "Gradient";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.gradient.desc";
        }

        @Override
        public String defaultDescription() {
            return "Smooth color gradient between two colors.";
        }

        @Override
        public String category() {
            return "color";
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public String getScript() {
            return Core.settings != null
                    ? Core.settings.getString(SETTING_KEY, DEFAULT_PALETTE)
                    : DEFAULT_PALETTE;
        }

        @Override
        public void setScript(String script) {
            if (Core.settings != null) {
                if (DEFAULT_PALETTE.equals(script)) {
                    Core.settings.remove(SETTING_KEY);
                } else {
                    Core.settings.put(SETTING_KEY, script);
                }
            }
        }

        @Override
        public void resetScript() {
            if (Core.settings != null) {
                Core.settings.remove(SETTING_KEY);
            }
        }

        @Override
        public String getDefaultScript() {
            return DEFAULT_PALETTE;
        }

        @Override
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

        @Override
        public String transform(String input, int maxLength) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            if (maxLength <= 0) {
                return input;
            }

            Color startColor = Color.valueOf("ff5e36");
            Color endColor = Color.valueOf("9b51e0");

            String script = getScript();
            if (script != null && !script.trim().isEmpty()) {
                String[] parts = script.split(",");
                if (parts.length >= 2) {
                    startColor = parseColor(parts[0], startColor);
                    endColor = parseColor(parts[1], endColor);
                } else if (parts.length == 1) {
                    String preset = parts[0].trim().toLowerCase(Locale.ROOT);
                    if ("cyberpunk".equals(preset)) {
                        startColor = Color.valueOf("00f0ff");
                        endColor = Color.valueOf("ff007f");
                    } else if ("ocean".equals(preset)) {
                        startColor = Color.valueOf("0072ff");
                        endColor = Color.valueOf("00f2fe");
                    } else if ("fire".equals(preset)) {
                        startColor = Color.valueOf("ffe259");
                        endColor = Color.valueOf("ffa751");
                    } else if ("neon".equals(preset)) {
                        startColor = Color.valueOf("a8ff78");
                        endColor = Color.valueOf("78ffd6");
                    } else {
                        startColor = parseColor(parts[0], startColor);
                    }
                }
            }

            int closingTagLen = 2; // "[]"
            int availableForTags = maxLength - input.length() - closingTagLen;
            if (availableForTags < 9) { // At least 1 hex tag "[#123456]"
                return input;
            }

            int maxTags = availableForTags / 9;
            int n = input.length();

            if (maxTags >= n) {
                StringBuilder sb = new StringBuilder(n * 10 + closingTagLen);
                for (int i = 0; i < n; i++) {
                    float t = n > 1 ? (float) i / (n - 1) : 0f;
                    Color cur = new Color(startColor).lerp(endColor, t);
                    sb.append(formatHexTag(cur)).append(input.charAt(i));
                }
                sb.append("[]");
                return sb.toString();
            }

            StringBuilder sb = new StringBuilder(maxTags * 9 + n + closingTagLen);
            int k = maxTags;
            for (int cluster = 0; cluster < k; cluster++) {
                int startIdx = cluster * n / k;
                int endIdx = (cluster + 1) * n / k;
                float t = k > 1 ? (float) cluster / (k - 1) : 0f;
                Color cur = new Color(startColor).lerp(endColor, t);
                sb.append(formatHexTag(cur));
                sb.append(input, startIdx, endIdx);
            }
            sb.append("[]");
            return sb.toString();
        }

        private static Color parseColor(String hex, Color fallback) {
            if (hex == null || hex.trim().isEmpty()) {
                return fallback;
            }
            try {
                String clean = hex.trim();
                if (clean.startsWith("#")) {
                    clean = clean.substring(1);
                }
                if ("sunset".equalsIgnoreCase(clean)) return Color.valueOf("ff5e36");
                if ("cyberpunk".equalsIgnoreCase(clean)) return Color.valueOf("00f0ff");
                if ("ocean".equalsIgnoreCase(clean)) return Color.valueOf("0072ff");
                if ("fire".equalsIgnoreCase(clean)) return Color.valueOf("ffe259");
                if ("neon".equalsIgnoreCase(clean)) return Color.valueOf("a8ff78");
                return Color.valueOf(clean);
            } catch (Exception e) {
                return fallback;
            }
        }

        private static String formatHexTag(Color c) {
            int r = Math.min(255, Math.max(0, (int) (c.r * 255f)));
            int g = Math.min(255, Math.max(0, (int) (c.g * 255f)));
            int b = Math.min(255, Math.max(0, (int) (c.b * 255f)));
            return String.format(Locale.ROOT, "[#%02x%02x%02x]", r, g, b);
        }
    }

    /** UwUifier replaces r/l with w and adds cute particles. */
    public static class UwuPrettier implements Prettier {
        @Override
        public String id() {
            return "uwu";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.uwu.name";
        }

        @Override
        public String defaultName() {
            return "UwUifier";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.uwu.desc";
        }

        @Override
        public String defaultDescription() {
            return "Turns r and l into w and adds uwu.";
        }

        @Override
        public String category() {
            return "fun";
        }

        @Override
        public String transform(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            String base = mapTextOutsideTags(input, text -> text.replace("r", "w")
                    .replace("R", "W")
                    .replace("l", "w")
                    .replace("L", "W")
                    .replace("ove", "uv")
                    .replace("OVE", "UV"));
            if (base.length() + 4 <= Vars.maxTextLength) {
                if (base.endsWith("[]")) {
                    return base.substring(0, base.length() - 2) + " uwu[]";
                }
                return base + " uwu";
            }
            return base;
        }
    }

    /** Alternating caps mocking SpongeBob style. */
    public static class MockingPrettier implements Prettier {
        @Override
        public String id() {
            return "mocking";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.mocking.name";
        }

        @Override
        public String defaultName() {
            return "Mocking";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.mocking.desc";
        }

        @Override
        public String defaultDescription() {
            return "AlTeRnAtInG cAsE mOcKiNg StYlE.";
        }

        @Override
        public String category() {
            return "case";
        }

        @Override
        public String transform(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(text.length());
                boolean upper = false;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (Character.isLetter(c)) {
                        sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                        upper = !upper;
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Converts text to UPPERCASE. */
    public static class CapsPrettier implements Prettier {
        @Override
        public String id() {
            return "caps";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.caps.name";
        }

        @Override
        public String defaultName() {
            return "CAPS LOCK";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.caps.desc";
        }

        @Override
        public String defaultDescription() {
            return "Converts all letters to UPPERCASE.";
        }

        @Override
        public String category() {
            return "case";
        }

        @Override
        public String transform(String input) {
            return input != null ? input.toUpperCase(Locale.ROOT) : "";
        }
    }

    /** Converts text to lowercase. */
    public static class LowercasePrettier implements Prettier {
        @Override
        public String id() {
            return "lowercase";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.lowercase.name";
        }

        @Override
        public String defaultName() {
            return "lowercase";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.lowercase.desc";
        }

        @Override
        public String defaultDescription() {
            return "Converts all letters to lowercase.";
        }

        @Override
        public String category() {
            return "case";
        }

        @Override
        public String transform(String input) {
            return input != null ? input.toLowerCase(Locale.ROOT) : "";
        }
    }

    /** Reverses character order. */
    public static class ReversePrettier implements Prettier {
        @Override
        public String id() {
            return "reverse";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.reverse.name";
        }

        @Override
        public String defaultName() {
            return "esreveR";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.reverse.desc";
        }

        @Override
        public String defaultDescription() {
            return "Reverses text character by character.";
        }

        @Override
        public String category() {
            return "effect";
        }

        @Override
        public String transform(String input) {
            return input != null ? new StringBuilder(input).reverse().toString() : "";
        }
    }

    /** Converts letters to unicode small capitals. */
    public static class SmallCapsPrettier implements Prettier {
        private static final String NORMAL = "abcdefghijklmnopqrstuvwxyz";
        private static final String SMALL = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";

        @Override
        public String id() {
            return "smallcaps";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.smallcaps.name";
        }

        @Override
        public String defaultName() {
            return "Small Caps";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.smallcaps.desc";
        }

        @Override
        public String defaultDescription() {
            return "Converts letters to ᴛɪɴʏ ᴜɴɪᴄᴏᴅᴇ ᴄᴀᴘɪᴛᴀʟs.";
        }

        @Override
        public String category() {
            return "font";
        }

        @Override
        public String transform(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(text.length());
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    char lower = Character.toLowerCase(c);
                    int idx = NORMAL.indexOf(lower);
                    if (idx != -1) {
                        sb.append(SMALL.charAt(idx));
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Converts letters and digits to circled / bubble unicode characters. */
    public static class BubblePrettier implements Prettier {
        @Override
        public String id() {
            return "bubble";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.bubble.name";
        }

        @Override
        public String defaultName() {
            return "Bubble";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.bubble.desc";
        }

        @Override
        public String defaultDescription() {
            return "Converts characters to ⓑⓤⓑⓑⓛⓔ symbols.";
        }

        @Override
        public String category() {
            return "font";
        }

        @Override
        public String transform(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(text.length());
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c >= 'a' && c <= 'z') {
                        sb.append((char) (0x24D0 + (c - 'a')));
                    } else if (c >= 'A' && c <= 'Z') {
                        sb.append((char) (0x24B6 + (c - 'A')));
                    } else if (c >= '1' && c <= '9') {
                        sb.append((char) (0x2460 + (c - '1')));
                    } else if (c == '0') {
                        sb.append('\u24EA');
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Fullwidth monospace text transformer. */
    public static class MonoPrettier implements Prettier {
        @Override
        public String id() {
            return "mono";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.mono.name";
        }

        @Override
        public String defaultName() {
            return "Monospace";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.mono.desc";
        }

        @Override
        public String defaultDescription() {
            return "Converts characters to \uFF46\uFF55\uFF4C\uFF4C\uFF57\uFF49\uFF44\uFF54\uFF48 monospace symbols.";
        }

        @Override
        public String category() {
            return "font";
        }

        @Override
        public String transform(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(text.length());
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c >= '!' && c <= '~') {
                        sb.append((char) (c - 0x21 + 0xFF01));
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Strikethrough text transformer using Unicode combining stroke. */
    public static class StrikethroughPrettier implements Prettier {
        @Override
        public String id() {
            return "strikethrough";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.strikethrough.name";
        }

        @Override
        public String defaultName() {
            return "Strikethrough";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.strikethrough.desc";
        }

        @Override
        public String defaultDescription() {
            return "Adds s\u0336t\u0336r\u0336i\u0336k\u0336e\u0336 line across text.";
        }

        @Override
        public String category() {
            return "effect";
        }

        @Override
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

        @Override
        public String transform(String input, int maxLength) {
            if (input == null || input.isEmpty() || maxLength <= 0) {
                return "";
            }
            if (input.length() > maxLength) {
                input = input.substring(0, maxLength);
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(Math.min(text.length() * 2, maxLength));
                for (int i = 0; i < text.length(); i++) {
                    if (sb.length() >= maxLength) {
                        break;
                    }
                    char c = text.charAt(i);
                    sb.append(c);
                    if (c != ' ' && c != '\t' && c != '\n' && sb.length() + 1 <= maxLength) {
                        sb.append('\u0336');
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Underline text transformer using Unicode combining low line. */
    public static class UnderlinePrettier implements Prettier {
        @Override
        public String id() {
            return "underline";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.underline.name";
        }

        @Override
        public String defaultName() {
            return "Underline";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.underline.desc";
        }

        @Override
        public String defaultDescription() {
            return "Adds u\u0332n\u0332d\u0332e\u0332r\u0332l\u0332i\u0332n\u0332e\u0332 below characters.";
        }

        @Override
        public String category() {
            return "effect";
        }

        @Override
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

        @Override
        public String transform(String input, int maxLength) {
            if (input == null || input.isEmpty() || maxLength <= 0) {
                return "";
            }
            if (input.length() > maxLength) {
                input = input.substring(0, maxLength);
            }
            return mapTextOutsideTags(input, text -> {
                StringBuilder sb = new StringBuilder(Math.min(text.length() * 2, maxLength));
                for (int i = 0; i < text.length(); i++) {
                    if (sb.length() >= maxLength) {
                        break;
                    }
                    char c = text.charAt(i);
                    sb.append(c);
                    if (c != ' ' && c != '\t' && c != '\n' && sb.length() + 1 <= maxLength) {
                        sb.append('\u0332');
                    }
                }
                return sb.toString();
            });
        }
    }

    /** Decorative brackets text transformer. */
    public static class FancyBracketsPrettier implements Prettier {
        public static final String DEFAULT_TEMPLATE = "\u3010 <message> \u3011";
        private static final String SETTING_KEY = "mindustrytool.pretty-chat.script.fancybrackets";

        @Override
        public String id() {
            return "fancybrackets";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.fancybrackets.name";
        }

        @Override
        public String defaultName() {
            return "Fancy Brackets";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.fancybrackets.desc";
        }

        @Override
        public String defaultDescription() {
            return "Wraps message with decorative brackets \u3010 ... \u3011.";
        }

        @Override
        public String category() {
            return "effect";
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public String getScript() {
            return Core.settings != null
                    ? Core.settings.getString(SETTING_KEY, DEFAULT_TEMPLATE)
                    : DEFAULT_TEMPLATE;
        }

        @Override
        public void setScript(String script) {
            if (Core.settings != null) {
                if (DEFAULT_TEMPLATE.equals(script)) {
                    Core.settings.remove(SETTING_KEY);
                } else {
                    Core.settings.put(SETTING_KEY, script);
                }
            }
        }

        @Override
        public void resetScript() {
            if (Core.settings != null) {
                Core.settings.remove(SETTING_KEY);
            }
        }

        @Override
        public String getDefaultScript() {
            return DEFAULT_TEMPLATE;
        }

        @Override
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

        @Override
        public String transform(String input, int maxLength) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            String template = getScript();
            if (template == null || !template.contains("<message>")) {
                template = DEFAULT_TEMPLATE;
            }
            String result = template.replace("<message>", input);
            if (result.length() > maxLength) {
                return input;
            }
            return result;
        }
    }

    /** Customizable prettier with user-defined template or JavaScript execution. */
    public static class CustomPrettier implements Prettier {
        public static final String DEFAULT_TEMPLATE = "<message>";
        private static final String SETTING_KEY = "mindustrytool.pretty-chat.script.custom";

        @Override
        public String id() {
            return "custom";
        }

        @Override
        public String nameKey() {
            return "pretty-chat.prettier.custom.name";
        }

        @Override
        public String defaultName() {
            return "Custom Script";
        }

        @Override
        public String descriptionKey() {
            return "pretty-chat.prettier.custom.desc";
        }

        @Override
        public String defaultDescription() {
            return "User-customized template or JavaScript formatter.";
        }

        @Override
        public String category() {
            return "custom";
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public String getScript() {
            return Core.settings != null
                    ? Core.settings.getString(SETTING_KEY, DEFAULT_TEMPLATE)
                    : DEFAULT_TEMPLATE;
        }

        @Override
        public void setScript(String script) {
            if (Core.settings != null) {
                if (DEFAULT_TEMPLATE.equals(script)) {
                    Core.settings.remove(SETTING_KEY);
                } else {
                    Core.settings.put(SETTING_KEY, script);
                }
            }
        }

        @Override
        public void resetScript() {
            if (Core.settings != null) {
                Core.settings.remove(SETTING_KEY);
            }
        }

        @Override
        public String getDefaultScript() {
            return DEFAULT_TEMPLATE;
        }

        @Override
        public String transform(String input) {
            if (input == null) {
                return "";
            }
            String script = getScript();
            if (script == null || script.trim().isEmpty() || DEFAULT_TEMPLATE.equals(script)) {
                return input;
            }

            // If it's a simple template without complex JS constructs, do fast replace
            if (!script.contains("function") && !script.contains("return") && !script.contains(";")) {
                return script.replace("<message>", input);
            }

            // If JS scripts engine is available in Mindustry Desktop
            if (Vars.mods != null && Vars.mods.getScripts() != null) {
                try {
                    String escaped = input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                    String toRun = script.replace("<message>", '"' + escaped + '"');
                    String res = Vars.mods.getScripts().runConsole(toRun);
                    if (res != null) {
                        return res;
                    }
                } catch (Exception e) {
                    Log.err("Failed to run custom pretty chat script: @", e.getMessage());
                }
            }

            return script.replace("<message>", input);
        }
    }

    /**
     * Applies a mapping function only to text segments outside of square brackets [...].
     * Preserves color tags and icon codes untouched.
     */
    public static String mapTextOutsideTags(String input, TextMapper mapper) {
        if (input == null || !input.contains("[")) {
            return mapper.map(input != null ? input : "");
        }
        StringBuilder sb = new StringBuilder(input.length());
        int len = input.length();
        int i = 0;
        while (i < len) {
            int open = input.indexOf('[', i);
            if (open == -1) {
                sb.append(mapper.map(input.substring(i)));
                break;
            }
            if (open > i) {
                sb.append(mapper.map(input.substring(i, open)));
            }
            int close = input.indexOf(']', open);
            if (close == -1) {
                sb.append(input.substring(open));
                break;
            }
            sb.append(input.substring(open, close + 1));
            i = close + 1;
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface TextMapper {
        String map(String text);
    }
}
