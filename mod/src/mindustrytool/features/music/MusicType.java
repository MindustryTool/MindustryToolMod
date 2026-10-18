package mindustrytool.features.music;

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
