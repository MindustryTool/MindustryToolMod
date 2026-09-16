package mindustrytool.features.music;

import arc.audio.Music;
import solim.signal.Signal;

/**
 * Reactive wrapper around a single {@link Music} track shown in the settings UI.
 * Playing and disabled states are signals so row buttons update without a rebuild.
 */
public class TrackState {

    public final Music music;
    /** Music category this track belongs to, e.g. "AMBIENT". Part of the disabled-track identity. */
    public final String typeName;
    /** Display name of the track, derived from the file name. */
    public final String name;
    /** True when the track was imported by the user rather than being an original game track. */
    public final boolean isCustom;

    public final Signal<Boolean> isPlaying = Signal.of(false);
    public final Signal<Boolean> isDisabled = Signal.of(false);

    public TrackState(Music music, String typeName, String name, boolean isCustom) {
        this.music = music;
        this.typeName = typeName;
        this.name = name;
        this.isCustom = isCustom;
    }

    /** Music category enum this track belongs to. */
    public MusicType type() {
        return MusicType.valueOf(typeName);
    }

    /** Unique config key for the disabled set, combining the music type and track name. */
    public String disabledKey() {
        return typeName + "_" + name;
    }
}
