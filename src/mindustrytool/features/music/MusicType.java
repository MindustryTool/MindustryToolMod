package mindustrytool.features.music;

public enum MusicType {
    AMBIENT,
    DARK,
    BOSS;

    public String getKey() {
        return "mindustrytool.music." + name().toLowerCase();
    }
}
