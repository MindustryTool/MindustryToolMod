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

    /** Rainbow color tags across words. */
    public static class RainbowPrettier implements Prettier {
        private static final String[] COLORS = {
                "[#ff5555]", "[#ffa500]", "[#ffff55]", "[#55ff55]", "[#55ffff]", "[#5599ff]", "[#cc66ff]"
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
            if (input == null || input.isEmpty()) {
                return "";
            }
            String[] words = input.split(" ");
            StringBuilder sb = new StringBuilder(input.length() + words.length * 10);
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(COLORS[i % COLORS.length]).append(words[i]);
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
            return input.replace("r", "w")
                    .replace("R", "W")
                    .replace("l", "w")
                    .replace("L", "W")
                    .replace("ove", "uv")
                    .replace("OVE", "UV")
                    + " uwu";
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
            StringBuilder sb = new StringBuilder(input.length());
            boolean upper = false;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (Character.isLetter(c)) {
                    sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                    upper = !upper;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
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
            StringBuilder sb = new StringBuilder(input.length());
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                char lower = Character.toLowerCase(c);
                int idx = NORMAL.indexOf(lower);
                if (idx != -1) {
                    sb.append(SMALL.charAt(idx));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
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
            StringBuilder sb = new StringBuilder(input.length());
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
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
}
