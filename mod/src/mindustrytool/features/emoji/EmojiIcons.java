package mindustrytool.features.emoji;

import arc.struct.Seq;
import java.lang.reflect.Field;
import mindustry.gen.Iconc;

/**
 * Cached source of {@link Iconc} font glyphs for the Emoji dialog. Reflects
 * once at class load and pairs each character field with its field name.
 */
public final class EmojiIcons {

    private static final Seq<Entry> ENTRIES = load();

    private EmojiIcons() {
    }

    public static Seq<Entry> entries() {
        return ENTRIES;
    }

    private static Seq<Entry> load() {
        Seq<Entry> result = new Seq<>();
        for (Field field : Iconc.class.getDeclaredFields()) {
            try {
                Object value = field.get(null);
                if (value instanceof Character) {
                    result.add(new Entry(field.getName(), (Character) value));
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static final class Entry {
        public final String name;
        public final char glyph;

        public Entry(String name, char glyph) {
            this.name = name;
            this.glyph = glyph;
        }
    }
}
