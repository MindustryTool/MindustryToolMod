package mindustrytool.features.music;

import arc.audio.Music;
import arc.struct.Seq;
import solim.reactive.Readable;

/**
 * Reactive view over a single {@link Music} track shown in the settings UI.
 * Disabled and playing states are derived readables: the disabled set lives in a
 * persisted config list, and playing state comes from the game's current track.
 */
public class TrackState {

    public final Music music;
    /** Music category this track belongs to. */
    public final MusicType type;
    /** Display name of the track, derived from the file name. */
    public final String name;
    /** True when the track was imported by the user rather than being an original game track. */
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

    /** Derives the disabled state from the persisted disabled-track list. */
    public Readable<Boolean> isDisabled() {
        return disabledTracks.map(list -> list != null && list.contains(disabledKey()));
    }

    /** Derives the playing state from the track currently owned by SoundControl. */
    public Readable<Boolean> isPlaying() {
        return currentMusic.map(current -> current == music);
    }

    public boolean disabled() {
        return Boolean.TRUE.equals(isDisabled().get());
    }

    /** Persisted disabled-list key, unique per music type and original/custom role. */
    public String disabledKey() {
        return disabledKey(type, name, isCustom);
    }

    /** Builds the persisted disabled-list key for a track identity. */
    public static String disabledKey(MusicType type, String name, boolean isCustom) {
        return type.name() + "_" + (isCustom ? "c:" : "o:") + name;
    }
}
