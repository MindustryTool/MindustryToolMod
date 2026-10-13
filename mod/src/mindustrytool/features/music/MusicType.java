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

    public boolean isSlot() {
        return this == MENU || this == EDITOR;
    }

    public String labelKey() {
        return "feature.music.section." + name().toLowerCase();
    }
}
