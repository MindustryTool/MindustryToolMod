package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.Map;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatTranslations {

    private final Signal<Map<String, String>> translations = Signal.of(new HashMap<>());
    private final Map<String, Readable<String>> translationComputeds = new HashMap<>();

    public Readable<String> get(@Nullable String messageId) {
        if (messageId == null) {
            return Readable.of(null);
        }
        Readable<String> existing = translationComputeds.get(messageId);
        if (existing == null) {
            existing = translations.map(map -> map.get(messageId));
            translationComputeds.put(messageId, existing);
        }
        return existing;
    }

    public @Nullable String getDirect(@Nullable String messageId) {
        return messageId != null ? translations.peek().get(messageId) : null;
    }

    public void set(@Nullable String messageId, @Nullable String translation) {
        if (messageId == null) return;
        Map<String, String> next = new HashMap<>(translations.peek());
        if (translation != null) {
            next.put(messageId, translation);
        } else {
            next.remove(messageId);
        }
        translations.set(next);
    }

    public void remove(@Nullable String messageId) {
        set(messageId, null);
    }
}
