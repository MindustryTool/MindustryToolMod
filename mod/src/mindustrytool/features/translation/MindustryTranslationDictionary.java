package mindustrytool.features.translation;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Offline tactical dictionary for common Mindustry multiplayer phrases.
 * Provides instant (0ms latency, zero-network) translations for resources,
 * commands, and tactical callouts across major supported languages.
 */
public final class MindustryTranslationDictionary {

    private static class Entry {
        final Map<String, String> translations = new HashMap<>();

        Entry put(String langCode, String text) {
            translations.put(langCode.toLowerCase(Locale.ROOT), text);
            return this;
        }

        @Nullable
        String get(String langCode) {
            return translations.get(langCode.toLowerCase(Locale.ROOT));
        }
    }

    private static final Map<String, Entry> DICTIONARY = new HashMap<>();

    static {
        // Resources & Logistics
        register(new String[]{"need silicon", "cần silicon", "нужен кремний", "需要硅"},
                new String[][]{{"en", "Need silicon"}, {"vi", "Cần silicon"}, {"ru", "Нужен кремний"}, {"zh", "需要硅"}});

        register(new String[]{"need thorium", "cần thorium", "cần thori", "нужен торий", "需要钍"},
                new String[][]{{"en", "Need thorium"}, {"vi", "Cần thorium"}, {"ru", "Нужен торий"}, {"zh", "需要钍"}});

        register(new String[]{"need copper", "cần đồng", "нужна медь", "需要铜"},
                new String[][]{{"en", "Need copper"}, {"vi", "Cần đồng"}, {"ru", "Нужна медь"}, {"zh", "需要铜"}});

        register(new String[]{"need lead", "cần chì", "нужен свинец", "需要铅"},
                new String[][]{{"en", "Need lead"}, {"vi", "Cần chì"}, {"ru", "Нужен свинец"}, {"zh", "需要铅"}});

        register(new String[]{"need plastanium", "cần plastanium", "нужен пластаний", "需要塑钢"},
                new String[][]{{"en", "Need plastanium"}, {"vi", "Cần plastanium"}, {"ru", "Нужен пластаний"}, {"zh", "需要塑钢"}});

        register(new String[]{"need surge", "cần surge", "cần hợp kim", "нужен сплав", "需要合金"},
                new String[][]{{"en", "Need surge alloy"}, {"vi", "Cần hợp kim surge"}, {"ru", "Нужен сплав"}, {"zh", "需要巨浪合金"}});

        register(new String[]{"need phase", "cần phase", "нужна фазовая ткань", "需要相位"},
                new String[][]{{"en", "Need phase fabric"}, {"vi", "Cần vải phase"}, {"ru", "Нужна фазовая ткань"}, {"zh", "需要相位织物"}});

        register(new String[]{"need titanium", "cần titan", "нужен титан", "需要钛"},
                new String[][]{{"en", "Need titanium"}, {"vi", "Cần titan"}, {"ru", "Нужен титан"}, {"zh", "需要钛"}});

        register(new String[]{"need coal", "cần than", "нужен уголь", "需要煤"},
                new String[][]{{"en", "Need coal"}, {"vi", "Cần than"}, {"ru", "Нужен уголь"}, {"zh", "需要煤"}});

        register(new String[]{"need graphite", "cần graphite", "нужен графит", "需要石墨"},
                new String[][]{{"en", "Need graphite"}, {"vi", "Cần graphite"}, {"ru", "Нужен графит"}, {"zh", "需要石墨"}});

        // Tactical Callouts
        register(new String[]{"attack", "tấn công", "атакуем", "атака", "进攻"},
                new String[][]{{"en", "Attack!"}, {"vi", "Tấn công!"}, {"ru", "Атакуем!"}, {"zh", "进攻！"}});

        register(new String[]{"retreat", "rút lui", "отступаем", "отход", "撤退"},
                new String[][]{{"en", "Retreat!"}, {"vi", "Rút lui!"}, {"ru", "Отступаем!"}, {"zh", "撤退！"}});

        register(new String[]{"defend core", "bảo vệ core", "phòng thủ core", "защищайте ядро", "防守核心"},
                new String[][]{{"en", "Defend the core!"}, {"vi", "Bảo vệ core!"}, {"ru", "Защищайте ядро!"}, {"zh", "防守核心！"}});

        register(new String[]{"help", "cứu", "giúp", "помогите", "помощь", "帮助"},
                new String[][]{{"en", "Help!"}, {"vi", "Cứu với!"}, {"ru", "Помогите!"}, {"zh", "帮助！"}});

        register(new String[]{"help me", "giúp tôi", "cứu tôi", "помогите мне", "帮帮我"},
                new String[][]{{"en", "Help me!"}, {"vi", "Giúp tôi với!"}, {"ru", "Помогите мне!"}, {"zh", "帮帮我！"}});

        // Common Communication
        register(new String[]{"yes", "có", "vâng", "да", "是"},
                new String[][]{{"en", "Yes"}, {"vi", "Có"}, {"ru", "Да"}, {"zh", "是"}});

        register(new String[]{"no", "không", "нет", "不"},
                new String[][]{{"en", "No"}, {"vi", "Không"}, {"ru", "Нет"}, {"zh", "不"}});

        register(new String[]{"ready", "sẵn sàng", "готов", "готовы", "准备"},
                new String[][]{{"en", "Ready"}, {"vi", "Sẵn sàng"}, {"ru", "Готов"}, {"zh", "已准备"}});

        register(new String[]{"wait", "chờ đã", "đợi", "подожди", "подождите", "等等"},
                new String[][]{{"en", "Wait"}, {"vi", "Chờ đã"}, {"ru", "Подождите"}, {"zh", "等等"}});

        register(new String[]{"stop", "dừng", "dừng lại", "стоп", "停止"},
                new String[][]{{"en", "Stop"}, {"vi", "Dừng lại"}, {"ru", "Стоп"}, {"zh", "停止"}});
    }

    private static void register(String[] triggers, String[][] translations) {
        Entry entry = new Entry();
        for (String[] t : translations) {
            entry.put(t[0], t[1]);
        }
        for (String trigger : triggers) {
            DICTIONARY.put(normalize(trigger), entry);
        }
    }

    private static String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[!?,.]+$", "")
                .trim();
    }

    private MindustryTranslationDictionary() {
    }

    /**
     * Attempts to find an instant offline translation for the given text.
     *
     * @param text the input message text
     * @param targetLanguage target language name or ISO code (e.g. "vi", "en", "Vietnamese")
     * @return translated text if found in dictionary, or null to fallback to online provider
     */
    public static @Nullable String findTranslation(@Nullable String text, @Nullable String targetLanguage) {
        if (text == null || text.trim().isEmpty() || targetLanguage == null || targetLanguage.trim().isEmpty()) {
            return null;
        }

        String norm = normalize(text);
        Entry entry = DICTIONARY.get(norm);
        if (entry == null) {
            return null;
        }

        String targetCode = resolveCode(targetLanguage);
        String result = entry.get(targetCode);

        // If target translation is identical to input, no translation needed
        if (result != null && normalize(result).equals(norm)) {
            return null;
        }

        return result;
    }

    private static String resolveCode(String lang) {
        String clean = lang.trim().toLowerCase(Locale.ROOT);
        if (clean.length() == 2) {
            return clean;
        }
        if (clean.startsWith("vi") || clean.contains("viet")) {
            return "vi";
        }
        if (clean.startsWith("ru") || clean.contains("russ")) {
            return "ru";
        }
        if (clean.startsWith("zh") || clean.contains("chin")) {
            return "zh";
        }
        return "en";
    }
}
