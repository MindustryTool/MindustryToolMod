package mindustrytool.features.music;

import arc.audio.Music;
import arc.struct.Seq;
import solim.reactive.Readable;

public class TrackState {

    public final Music music;

    public final MusicType type;

    public final String name;

    public final boolean isCustom;

    private final Readable<Seq<String>> disabledTracks;
    private final Readable<Music> currentMusic;

    public TrackState(Music music, MusicType type, String name, boolean isCustom,
            Readable<Seq<String>> disabledTracks, Readable<Music> currentMusic) {
        this.music = music;
        this.type = type;
        this.name = name;
        this.isCustom = isCustom;
        this.disabledTracks = disabledTracks;
        this.currentMusic = currentMusic;
    }

    public Readable<Boolean> isDisabled() {
        return disabledTracks.map(list -> list != null && list.contains(disabledKey()));
    }

    public Readable<Boolean> isPlaying() {
        return currentMusic.map(current -> current == music);
    }

    public boolean disabled() {
        return Boolean.TRUE.equals(isDisabled().get());
    }

    public String disabledKey() {
        return disabledKey(type, name, isCustom);
    }

    public static String disabledKey(MusicType type, String name, boolean isCustom) {
        return type.name() + "_" + (isCustom ? "c:" : "o:") + name;
    }
}
