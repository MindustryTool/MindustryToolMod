package mindustrytool.features.chat.state;

import arc.util.Nullable;
import solim.reactive.MapSignal;
import solim.reactive.Readable;

public final class ChatTranslations {

    private final MapSignal<String, String> translations = MapSignal.of();
    private final MapSignal<String, Boolean> showOriginal = MapSignal.of();

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
            showOriginal.remove(messageId);
        } else {
            translations.remove(messageId);
            showOriginal.remove(messageId);
        }
    }

    public void remove(@Nullable String messageId) {
        set(messageId, null);
    }

    public Readable<Boolean> isShowingOriginal(@Nullable String messageId) {
        return messageId == null ? Readable.of(false) : showOriginal.readable(messageId).map(b -> Boolean.TRUE.equals(b));
    }

    public boolean isShowingOriginalDirect(@Nullable String messageId) {
        if (messageId == null) return false;
        Boolean val = showOriginal.peek().get(messageId);
        return Boolean.TRUE.equals(val);
    }

    public void toggleOriginal(@Nullable String messageId) {
        if (messageId == null) return;
        boolean current = isShowingOriginalDirect(messageId);
        showOriginal.put(messageId, !current);
    }

    public void setShowOriginal(@Nullable String messageId, boolean show) {
        if (messageId == null) return;
        showOriginal.put(messageId, show);
    }
}
