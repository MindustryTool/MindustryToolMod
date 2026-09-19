package mindustrytool.features.chat.state;

import arc.util.Nullable;
import solim.reactive.MapSignal;
import solim.reactive.Readable;

public final class ChatTranslations {

    private final MapSignal<String, String> translations = MapSignal.of();

    public Readable<String> get(@Nullable String messageId) {
        return messageId == null ? Readable.of(null) : translations.readable(messageId);
    }

    public @Nullable String getDirect(@Nullable String messageId) {
        return messageId != null ? translations.peek().get(messageId) : null;
    }

    public void set(@Nullable String messageId, @Nullable String translation) {
        if (messageId == null) return;
        if (translation != null) {
            translations.put(messageId, translation);
        } else {
            translations.remove(messageId);
        }
    }

    public void remove(@Nullable String messageId) {
        set(messageId, null);
    }
}
