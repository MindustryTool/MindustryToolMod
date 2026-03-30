package mindustrytool;

import arc.struct.Seq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mindustry.gen.Iconc;

public class IconUtils {

    public static final Seq<IconC> iconcs = getIconc();

    @Data
    @RequiredArgsConstructor
    public static class IconC {
        private final String name;
        private final Character value;
    }

    private static Seq<IconC> getIconc() {
        return Seq.with(Iconc.class.getDeclaredFields())
                .map(f -> {
                    try {
                        Object value = f.get(null);
                        if (value instanceof Character) {
                            return new IconC(f.getName(), (Character) value);
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                }).select(f -> f != null);
    }
}
