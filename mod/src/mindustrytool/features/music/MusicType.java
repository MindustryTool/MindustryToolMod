package mindustrytool.features.music;

/**
 * Music categories that custom tracks can be injected into.
 */
public enum MusicType {
    AMBIENT,
    DARK,
    BOSS;

    /** Bundle key for the human-readable section label of this music type. */
    public String labelKey() {
        return "feature.music.section." + name().toLowerCase();
    }
}
