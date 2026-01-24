package mindustrytool.features.chat.pretty;

import arc.Core;
import arc.struct.Seq;

public class PrettyChatConfig {
    public static final String KEY_CONFIG = "mindustrytool.prettychat.config";
    public static final String DEFAULT_CONFIG = "default";
    public static final String KEY_SCRIPT_PREFIX = "mindustrytool.prettychat.script.";

    public static String getScript(String id, String defaultScript) {
        return Core.settings.getString(KEY_SCRIPT_PREFIX + id, defaultScript);
    }

    public static void setScript(String id, String script) {
        Core.settings.put(KEY_SCRIPT_PREFIX + id, script);
    }

    public static void resetScript(String id) {
        Core.settings.remove(KEY_SCRIPT_PREFIX + id);
    }

    public static boolean hasCustomScript(String id) {
        return Core.settings.has(KEY_SCRIPT_PREFIX + id);
    }

    public static Seq<String> getEnabledIds() {
        String data = Core.settings.getString(KEY_CONFIG, DEFAULT_CONFIG);
        Seq<String> result = new Seq<>();

        if (data == null || data.isEmpty()) {
            return result;
        }

        for (String part : data.split(",")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    public static void setEnabledIds(Seq<String> ids) {
        Core.settings.put(KEY_CONFIG, ids.toString(","));
    }
}
