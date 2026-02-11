package mindustrytool.features.music;

import arc.Core;
import arc.struct.Seq;

@SuppressWarnings("unchecked")
public class MusicConfig {
    private static final String PREFIX = "mindustrytool.music.";
    private static final String DISABLED_KEY = PREFIX + "disabled";

    public static Seq<String> getPaths(MusicType type) {
        return Core.settings.getJson(type.getKey(), Seq.class, String.class, Seq::new);
    }

    public static void savePaths(MusicType type, Seq<String> paths) {
        Core.settings.putJson(type.getKey(), String.class, paths);
    }

    public static Seq<String> getDisabledSounds() {
        return Core.settings.getJson(DISABLED_KEY, Seq.class, String.class, Seq::new);
    }

    public static void saveDisabledSounds(Seq<String> names) {
        Core.settings.putJson(DISABLED_KEY, String.class, names);
    }
}
