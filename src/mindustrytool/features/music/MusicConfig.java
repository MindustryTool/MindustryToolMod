package mindustrytool.features.music;

import arc.Core;
import arc.struct.Seq;

@SuppressWarnings("unchecked")
public class MusicConfig {
    private static final String PREFIX = "mindustrytool.music.";
    private static final String AMBIENT_KEY = PREFIX + "ambient";
    private static final String DARK_KEY = PREFIX + "dark";
    private static final String BOSS_KEY = PREFIX + "boss";
    private static final String DISABLED_KEY = PREFIX + "disabled";

    public static Seq<String> getAmbientPaths() {
        return Core.settings.getJson(AMBIENT_KEY, Seq.class, String.class, Seq::new);
    }

    public static void saveAmbientPaths(Seq<String> paths) {
        Core.settings.putJson(AMBIENT_KEY, String.class, paths);
    }

    public static Seq<String> getDarkPaths() {
        return Core.settings.getJson(DARK_KEY, Seq.class, String.class, Seq::new);
    }

    public static void saveDarkPaths(Seq<String> paths) {
        Core.settings.putJson(DARK_KEY, String.class, paths);
    }

    public static Seq<String> getBossPaths() {
        return Core.settings.getJson(BOSS_KEY, Seq.class, String.class, Seq::new);
    }

    public static void saveBossPaths(Seq<String> paths) {
        Core.settings.putJson(BOSS_KEY, String.class, paths);
    }

    public static Seq<String> getDisabledSounds() {
        return Core.settings.getJson(DISABLED_KEY, Seq.class, String.class, Seq::new);
    }

    public static void saveDisabledSounds(Seq<String> names) {
        Core.settings.putJson(DISABLED_KEY, String.class, names);
    }
}
