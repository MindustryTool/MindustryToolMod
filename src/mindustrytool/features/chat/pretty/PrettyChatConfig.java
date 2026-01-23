package mindustrytool.features.chat.pretty;

import arc.Core;
import arc.struct.Seq;

public class PrettyChatConfig {
    public static final String KEY_CONFIG = "mindustrytool.prettychat.config";
    public static final String DEFAULT_CONFIG = "default";

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
