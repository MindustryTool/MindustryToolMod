package mindustrytool.features.chat.translation;

import arc.Core;

public class ChatTranslationConfig {
    private static final String PREFIX = "mindustry-tool.chat.translation.";
    public static final String SHOW_ORIGINAL = PREFIX + "show.original";
    public static final String PROVIDER = PREFIX + "provider";
    public static final String GEMINI_API_KEY = "mindustry-tool.gemini.api.key";
    public static final String GEMINI_MODEL = "mindustry-tool.gemini.model";
    public static final String GEMINI_TIMEOUT = "mindustry-tool.gemini.timeout";
    public static final String GEMINI_MAX_HISTORY = "mindustry-tool.gemini.max-history";

    public static final String DEEPL_API_KEY = "mindustry-tool.deepl.api.key";
    public static final String DEEPL_TIMEOUT = "mindustry-tool.deepl.timeout";

    public static boolean isShowOriginal() {
        return Core.settings.getBool(SHOW_ORIGINAL, true);
    }

    public static void setShowOriginal(boolean showOriginal) {
        Core.settings.put(SHOW_ORIGINAL, showOriginal);
    }

    public static String getProviderId() {
        return Core.settings.getString(PROVIDER, "noop");
    }

    public static void setProviderId(String id) {
        Core.settings.put(PROVIDER, id);
    }
}
