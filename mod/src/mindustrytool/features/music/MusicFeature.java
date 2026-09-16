package mindustrytool.features.music;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.files.Fi;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import arc.util.Timer.Task;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustrytool.Folders;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigPersister;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.signal.Signal;
import solim.signal.Readable;

/**
 * Custom music loader. Backs up the game's ambient/dark/boss music lists and
 * rebuilds them in place with custom tracks and user-disabled entries removed.
 */
public class MusicFeature extends Feature {

    public static final Seq<String> VALID_EXTENSIONS = Seq.with("mp3", "ogg", "wav");
    private static final int MAX_NAME_SUFFIX = 1000;

    public final ConfigGroup config;
    public final ConfigValue<Seq<String>> ambientPaths;
    public final ConfigValue<Seq<String>> darkPaths;
    public final ConfigValue<Seq<String>> bossPaths;
    public final ConfigValue<Seq<String>> disabledTracks;

    private final ObjectMap<MusicType, Seq<Music>> originalMusic = new ObjectMap<>();
    private final ObjectMap<String, Music> musicCache = new ObjectMap<>();
    private final ObjectMap<String, TrackState> trackStates = new ObjectMap<>();
    private final ObjectMap<MusicType, Signal<Seq<TrackState>>> trackSignals = new ObjectMap<>();
    private boolean captured = false;

    private @Nullable MusicSettingsDialog settingsDialog;

    public MusicFeature() {
        super(FeatureMetadata.builder()
                .id("music")
                .icon(FileIcon.of("music.png"))
                .order(22)
                .enabledByDefault(false)
                .build());

        config = configGroup();
        StringSeqPersister seqPersister = new StringSeqPersister();
        ambientPaths = config.value("ambient", new Seq<>(), seqPersister);
        darkPaths = config.value("dark", new Seq<>(), seqPersister);
        bossPaths = config.value("boss", new Seq<>(), seqPersister);
        disabledTracks = config.value("disabled", new Seq<>(), seqPersister);

        for (MusicType type : MusicType.values()) {
            trackSignals.put(type, Signal.of(new Seq<>()));
        }

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                captureOriginalMusic();
                loadAllCustomMusic();
            }
        }));
    }

    // region Config helpers

    public Seq<String> pathsFor(MusicType type) {
        ConfigValue<Seq<String>> value = pathsConfig(type);
        Seq<String> paths = value.get();
        return paths != null ? paths : new Seq<>();
    }

    public ConfigValue<Seq<String>> pathsConfig(MusicType type) {
        switch (type) {
        case AMBIENT:
            return ambientPaths;
        case DARK:
            return darkPaths;
        case BOSS:
        default:
            return bossPaths;
        }
    }

    private void setPaths(MusicType type, Seq<String> paths) {
        pathsConfig(type).set(paths);
    }

    public Readable<Seq<TrackState>> trackSignal(MusicType type) {
        return trackSignals.get(type);
    }

    // endregion

    // region Lifecycle

    @Override
    public void onEnable() {
        captureOriginalMusic();
        loadAllCustomMusic();
    }

    @Override
    public void onDisable() {
        restoreOriginalMusic();
    }

    /** Backs up the game's music lists the first time they are populated. */
    public void captureOriginalMusic() {
        if (captured) {
            return;
        }
        if (Vars.control.sound.ambientMusic.size != 0) {
            originalMusic.put(MusicType.AMBIENT, new Seq<>(Vars.control.sound.ambientMusic));
        }
        if (Vars.control.sound.darkMusic.size != 0) {
            originalMusic.put(MusicType.DARK, new Seq<>(Vars.control.sound.darkMusic));
        }
        if (Vars.control.sound.bossMusic.size != 0) {
            originalMusic.put(MusicType.BOSS, new Seq<>(Vars.control.sound.bossMusic));
        }
        captured = !originalMusic.isEmpty();
    }

    /** Restores the backed-up original music lists, removing all custom tracks. */
    public void restoreOriginalMusic() {
        if (originalMusic.isEmpty()) {
            return;
        }
        for (MusicType type : MusicType.values()) {
            Seq<Music> originals = originalMusic.get(type);
            if (originals != null) {
                Seq<Music> targetList = getTargetList(type);
                targetList.clear();
                targetList.addAll(originals);
            }
        }
        for (Signal<Seq<TrackState>> statesSignal : trackSignals.values()) {
            for (TrackState state : statesSignal.peek()) {
                state.isPlaying.set(false);
                if (state.music.isPlaying()) {
                    state.music.stop();
                }
            }
        }
    }

    // endregion

    // region Loading

    public void loadAllCustomMusic() {
        for (MusicType type : MusicType.values()) {
            loadMusicType(type);
        }
    }

    /**
     * Rebuilds the game's music list for a type: originals plus valid custom files,
     * minus disabled tracks.
     */
    public void loadMusicType(MusicType type) {
        Seq<Music> targetList = getTargetList(type);
        Seq<Music> originals = originalMusic.get(type);
        if (targetList == null || originals == null) {
            return;
        }

        Seq<String> paths = pathsFor(type);
        Seq<String> validPaths = new Seq<>();
        Seq<TrackState> states = new Seq<>();

        for (Music music : originals) {
            states.add(stateFor(type, trackName(music), false, music));
        }

        for (String path : paths) {
            Fi file = Folders.musicsDir.child(path);
            if (!file.exists()) {
                Log.warn("Music file not found: " + path);
                continue;
            }

            Music music = musicCache.get(path);
            if (music == null) {
                try {
                    music = new Music(file);
                } catch (Exception e) {
                    Log.err("Failed to load music: " + path, e);
                    continue;
                }
                musicCache.put(path, music);
            }
            validPaths.add(path);
            states.add(stateFor(type, nameWithoutExtension(path), true, music));
        }

        if (validPaths.size != paths.size) {
            setPaths(type, validPaths);
        }

        Seq<Music> playable = new Seq<>();
        for (TrackState state : states) {
            boolean disabled = isDisabled(state);
            state.isDisabled.set(disabled);
            if (!disabled) {
                playable.add(state.music);
            }
        }

        trackSignals.get(type).set(states);

        targetList.clear();
        targetList.addAll(playable);
    }

    // endregion

    // region Track operations

    /**
     * Imports a file into the mod's music directory and registers it for a music
     * type.
     */
    public void addTrack(MusicType type, @Nullable Fi file) {
        if (file == null || !file.exists()) {
            Vars.ui.showErrorMessage(Core.bundle.get("feature.music.error.invalid-file"));
            return;
        }
        if (!VALID_EXTENSIONS.contains(file.extension().toLowerCase())) {
            Vars.ui.showErrorMessage(
                    Core.bundle.format("feature.music.error.invalid-type", VALID_EXTENSIONS.toString(", ")));
            return;
        }

        try {
            Fi copy = uniqueTarget(Folders.musicsDir.child(file.name()));
            file.copyTo(copy);

            Seq<String> paths = pathsFor(type).copy();
            paths.add(copy.name());
            setPaths(type, paths);
            loadMusicType(type);
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }

    /**
     * Picks a non-existing destination file, appending _1, _2, ... before the
     * extension on conflicts.
     */
    private Fi uniqueTarget(Fi target) {
        if (!target.exists()) {
            return target;
        }
        String name = target.nameWithoutExtension();
        String extension = "." + target.extension();
        for (int i = 1; i <= MAX_NAME_SUFFIX; i++) {
            Fi candidate = target.parent().child(name + "_" + i + extension);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return target;
    }

    /** Removes a custom track from a music type, disposing its audio resource. */
    public void removeTrack(MusicType type, TrackState state) {
        Fi file = state.music.file;
        if (file == null || !state.isCustom) {
            return;
        }

        Seq<String> paths = pathsFor(type);
        String storedName = paths.contains(file.name()) ? file.name() : file.nameWithoutExtension();
        Seq<String> remaining = without(paths, storedName);
        boolean removed = remaining.size != paths.size;

        if (!removed) {
            return;
        }

        if (state.music.isPlaying()) {
            state.music.stop();
        }
        state.music.dispose();
        musicCache.remove(file.name());
        trackStates.remove(state.typeName + "_" + state.name);

        setPaths(type, remaining);
        loadMusicType(type);
    }

    /**
     * Disables every original game track of a music type, leaving only custom
     * tracks.
     */
    public void disableAllOriginals(MusicType type) {
        Seq<Music> originals = originalMusic.get(type);
        if (originals == null) {
            return;
        }

        Seq<String> disabled = disabledTracks.get().copy();
        boolean changed = false;
        for (Music music : originals) {
            String key = type.name() + "_" + trackName(music);
            if (!disabled.contains(key)) {
                disabled.add(key);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        disabledTracks.set(disabled);
        loadMusicType(type);
        playRandom();
    }

    /**
     * Toggles a track's disabled state, persisting it and rebuilding the playable
     * list.
     */
    public void toggleDisabled(TrackState state) {
        String key = state.disabledKey();
        Seq<String> disabled = disabledTracks.get().copy();
        if (disabled.contains(key)) {
            disabled = without(disabled, key);
        } else {
            disabled.add(key);
            if (state.music.isPlaying()) {
                state.music.stop();
            }
        }

        disabledTracks.set(disabled);
        state.isDisabled.set(disabled.contains(key));
        loadMusicType(state.type());
        playRandom();
    }

    /** Plays or stops a track preview, updating the reactive playing state. */
    public void togglePlay(TrackState state) {
        try {
            if (state.music.isPlaying()) {
                state.music.stop();
            } else {
                state.music.play();
                Task[] task = { null };

                task[0] = Timer.schedule(() -> {
                    if (!state.music.isPlaying()) {
                        task[0].cancel();
                        Core.app.post(() -> state.isPlaying.set(state.music.isPlaying()));
                    }

                }, 0, 1);
            }
        } catch (Exception e) {
            Log.err("Failed to toggle music preview", e);
        }
        state.isPlaying.set(state.music.isPlaying());
    }

    // endregion

    // region Helpers

    public boolean isDisabled(TrackState state) {
        Seq<String> disabled = disabledTracks.get();
        return disabled != null && disabled.contains(state.disabledKey());
    }

    public String trackName(Music music) {
        Fi file = music.file;
        return file != null ? file.nameWithoutExtension() : "Unknown";
    }

    private String nameWithoutExtension(String path) {
        Fi file = Folders.musicsDir.child(path);
        return file.nameWithoutExtension();
    }

    /** Returns a new sequence without the given value, preserving order. */
    private static Seq<String> without(Seq<String> source, String value) {
        Seq<String> result = new Seq<>();
        for (String entry : source) {
            if (!entry.equals(value)) {
                result.add(entry);
            }
        }
        return result;
    }

    private TrackState stateFor(MusicType type, String name, boolean isCustom, Music music) {
        String key = type.name() + "_" + name;
        TrackState existing = trackStates.get(key);
        if (existing != null && existing.music == music && existing.isCustom == isCustom) {
            return existing;
        }
        TrackState state = new TrackState(music, type.name(), name, isCustom);
        trackStates.put(key, state);
        return state;
    }

    private Seq<Music> getTargetList(MusicType type) {
        switch (type) {
        case AMBIENT:
            return Vars.control.sound.ambientMusic;
        case DARK:
            return Vars.control.sound.darkMusic;
        case BOSS:
        default:
            return Vars.control.sound.bossMusic;
        }
    }

    private void playRandom() {
        Vars.control.sound.playRandom();
    }

    // endregion

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new MusicSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    /**
     * Persists {@link Seq} of strings through Core.settings JSON, matching the
     * legacy config format.
     */
    private static class StringSeqPersister implements ConfigPersister<Seq<String>> {
        @SuppressWarnings("unchecked")
        @Override
        public Seq<String> load(String key, @Nullable Seq<String> defaultValue) {
            return Core.settings.getJson(key, Seq.class, String.class,
                    () -> defaultValue != null ? defaultValue : new Seq<>());
        }

        @Override
        public void save(String key, @Nullable Seq<String> value) {
            Core.settings.putJson(key, String.class, value != null ? value : new Seq<>());
        }
    }
}
