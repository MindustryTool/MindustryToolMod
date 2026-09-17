package mindustrytool.features.music;

/**
 * Music categories that custom tracks can be injected into. AMBIENT, DARK and
 * BOSS are game rotations; MENU and EDITOR replace a single vanilla music slot.
 */
public enum MusicType {
    AMBIENT,
    DARK,
    BOSS,
    MENU,
    EDITOR;

    /** True when this type replaces a single vanilla {@code Musics} field rather than a rotation. */
    public boolean isSlot() {
        return this == MENU || this == EDITOR;
    }

    /** Bundle key for the human-readable section label of this music type. */
    public String labelKey() {
        return "feature.music.section." + name().toLowerCase();
    }
}
