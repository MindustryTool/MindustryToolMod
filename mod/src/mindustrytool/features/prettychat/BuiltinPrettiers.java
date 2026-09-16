package mindustrytool.features.prettychat;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
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
        list.add(new UwuPrettier());
        list.add(new MockingPrettier());
        list.add(new CapsPrettier());
        list.add(new LowercasePrettier());
        list.add(new ReversePrettier());
        list.add(new SmallCapsPrettier());
        list.add(new BubblePrettier());
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
        public String transform(String input) {
            return transform(input, Vars.maxTextLength);
        }

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
