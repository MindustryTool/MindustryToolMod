package mindustrytool.features.chat.translation;

import arc.Core;

public class ChatTranslationConfig {
    private static final String PREFIX = "mindustrytool.chat-translation.";
    public static final String SHOW_ORIGINAL = PREFIX + "show.original";
    public static final String PROVIDER = PREFIX + "provider";
    public static final String GEMINI_API_KEY = "mindustrytool.chat-translation.gemini.api-key";
    public static final String GEMINI_MODEL = "mindustrytool.chat-translation.gemini.model";
    public static final String GEMINI_TIMEOUT = "mindustrytool.chat-translation.gemini.timeout";
    public static final String GEMINI_MAX_HISTORY = "mindustrytool.chat-translation.gemini.max-history";

    public static final String DEEPL_API_KEY = "mindustrytool.chat-translation.deepl.api-key";
    public static final String DEEPL_TIMEOUT = "mindustrytool.chat-translation.deepl.timeout";

    public static String getProviderId() {
        return Core.settings.getString(PROVIDER, "noop");
    }

    public static void setProviderId(String id) {
        Core.settings.put(PROVIDER, id);
    }
}
