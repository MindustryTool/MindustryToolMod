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
import mindustry.Vars;
import mindustry.game.EventType.MusicRegisterEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Musics;
import mindustrytool.Folders;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Custom music loader. Backs up the game's ambient/dark/boss music lists and
 * rebuilds them in place with custom tracks and user-disabled entries removed.
 * Menu and editor music slots are replaced by reassigning {@link Musics} fields.
 * Playback is driven by the game's SoundControl so state stays authoritative.
 */
public class MusicFeature extends Feature {

    public static final Seq<String> VALID_EXTENSIONS = Seq.with("mp3", "ogg", "wav");
    private static final int MAX_NAME_SUFFIX = 1000;

    public final ConfigGroup config;
    public final ConfigValue<Seq<String>> ambientPaths;
    public final ConfigValue<Seq<String>> darkPaths;
    public final ConfigValue<Seq<String>> bossPaths;
    public final ConfigValue<Seq<String>> menuPaths;
    public final ConfigValue<Seq<String>> editorPaths;
    /** Persisted list of disabled track keys, formatted by {@link TrackState#disabledKey}. */
    public final ConfigValue<Seq<String>> disabledTracks;

    private final ObjectMap<MusicType, Seq<Music>> originalMusic = new ObjectMap<>();
    private final ObjectMap<String, Music> musicCache = new ObjectMap<>();
    private final ObjectMap<String, TrackState> trackStates = new ObjectMap<>();
    private final ObjectMap<MusicType, Signal<Seq<TrackState>>> trackSignals = new ObjectMap<>();
    private final Signal<Music> currentMusic = Signal.of((Music) null);
    private boolean captured = false;

    private @Nullable Music originalMenu;
    private @Nullable Music originalEditor;
    private boolean wasInMenu = false;
    private boolean wasInEditor = false;

    private @Nullable MusicSettingsDialog settingsDialog;

    public MusicFeature() {
        super(FeatureMetadata.builder()
                .id("music")
                .icon(FileIcon.of("music.png"))
                .order(22)
                .enabledByDefault(false)
                .build());

        config = configGroup();
        OrderedSeqPersister seqPersister = new OrderedSeqPersister();
        ambientPaths = config.value("ambient", new Seq<>(), seqPersister);
        darkPaths = config.value("dark", new Seq<>(), seqPersister);
        bossPaths = config.value("boss", new Seq<>(), seqPersister);
        menuPaths = config.value("menu", new Seq<>(), seqPersister);
        editorPaths = config.value("editor", new Seq<>(), seqPersister);
        disabledTracks = config.value("disabled", new Seq<>(), seqPersister);

        for (MusicType type : MusicType.values()) {
            trackSignals.put(type, Signal.of(new Seq<>()));
        }

        //keep the UI in sync with the game's actual playback and re-roll slot music on entry
        Events.run(Trigger.update, () -> {
            currentMusic.set(Vars.control.sound.getCurrent());

            boolean inMenu = Vars.state.isMenu();
            if (inMenu && !wasInMenu) {
                rerollSlot(MusicType.MENU);
            }
            wasInMenu = inMenu;

            boolean inEditor = Vars.state.rules.editor;
            if (inEditor && !wasInEditor) {
                rerollSlot(MusicType.EDITOR);
            }
            wasInEditor = inEditor;
        });

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                captureOriginalMusic();
                loadAllCustomMusic();
            }
        }));

        //SoundControl.reload() reassigns the music lists, so re-apply afterwards
        Events.on(MusicRegisterEvent.class, e -> Core.app.post(() -> {
            if (isEnabled()) {
                recaptureOriginalMusic();
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
            return bossPaths;
        case MENU:
            return menuPaths;
        case EDITOR:
        default:
            return editorPaths;
        }
    }

    private void setPaths(MusicType type, Seq<String> paths) {
        pathsConfig(type).set(paths);
    }

    public Readable<Seq<TrackState>> trackSignal(MusicType type) {
        return trackSignals.get(type);
    }

    /** Reactive view of the track the game's player currently owns. */
    public Readable<Music> currentMusic() {
        return currentMusic;
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

    /** Backs up the game's music lists and slots the first time they are available. */
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
        originalMenu = Musics.menu;
        originalEditor = Musics.editor;
        captured = !originalMusic.isEmpty();
    }

    /** Discards the captured backup and captures the freshly reloaded original music. */
    private void recaptureOriginalMusic() {
        //restore slots before capturing so a custom track is never captured as an original
        if (originalMenu != null) {
            Musics.menu = originalMenu;
        }
        if (originalEditor != null) {
            Musics.editor = originalEditor;
        }
        captured = false;
        originalMusic.clear();
        captureOriginalMusic();
    }

    /** Restores the backed-up original music lists and slots, removing all custom tracks. */
    public void restoreOriginalMusic() {
        if (originalMenu != null) {
            Musics.menu = originalMenu;
        }
        if (originalEditor != null) {
            Musics.editor = originalEditor;
        }

        for (MusicType type : MusicType.values()) {
            Seq<Music> originals = originalMusic.get(type);
            if (originals != null) {
                Seq<Music> targetList = getTargetList(type);
                if (targetList != null) {
                    targetList.clear();
                    targetList.addAll(originals);
                }
            }
        }

        //stop whatever the game player is currently previewing
        Vars.control.sound.stop();
    }

    // endregion

    // region Loading

    public void loadAllCustomMusic() {
        for (MusicType type : MusicType.values()) {
            loadMusicType(type);
        }
    }

    /**
     * Rebuilds the game's music for a type: originals plus valid custom files, minus
     * disabled tracks. Also prunes disabled entries for missing custom files.
     */
    public void loadMusicType(MusicType type) {
        if (type.isSlot()) {
            loadSlot(type);
            return;
        }

        Seq<Music> targetList = getTargetList(type);
        Seq<Music> originals = originalMusic.get(type);
        if (targetList == null || originals == null) {
            return;
        }

        Seq<String> paths = pathsFor(type);
        Seq<String> validPaths = new Seq<>();
        Seq<TrackState> states = new Seq<>();
        Seq<String> validCustomKeys = new Seq<>();

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
            TrackState state = stateFor(type, nameWithoutExtension(path), true, music);
            states.add(state);
            validCustomKeys.add(state.disabledKey());
        }

        if (validPaths.size != paths.size) {
            setPaths(type, validPaths);
        }
        pruneDisabled(type, validCustomKeys);

        Seq<Music> playable = new Seq<>();
        for (TrackState state : states) {
            if (!state.disabled()) {
                playable.add(state.music);
            }
        }

        trackSignals.get(type).set(states);

        targetList.clear();
        targetList.addAll(playable);
    }

    /**
     * Loads a single-slot type (menu/editor): only custom tracks exist, and the slot
     * is assigned a random enabled track or restored to vanilla.
     */
    private void loadSlot(MusicType type) {
        Seq<String> paths = pathsFor(type);
        Seq<String> validPaths = new Seq<>();
        Seq<TrackState> states = new Seq<>();
        Seq<String> validCustomKeys = new Seq<>();

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
            TrackState state = stateFor(type, nameWithoutExtension(path), true, music);
            states.add(state);
            validCustomKeys.add(state.disabledKey());
        }

        if (validPaths.size != paths.size) {
            setPaths(type, validPaths);
        }
        pruneDisabled(type, validCustomKeys);

        trackSignals.get(type).set(states);
        assignSlot(type, states);
    }

    /** Assigns a random enabled custom track to the slot, or restores the vanilla track. */
    private void assignSlot(MusicType type, Seq<TrackState> states) {
        Seq<Music> playable = new Seq<>();
        for (TrackState state : states) {
            if (!state.disabled()) {
                playable.add(state.music);
            }
        }
        setSlotMusic(type, pickSlotMusic(playable, originalSlot(type), slotMusic(type)));
    }

    /** Re-rolls the slot track when the player enters the menu/editor. */
    private void rerollSlot(MusicType type) {
        if (!isEnabled()) {
            return;
        }
        assignSlot(type, trackSignals.get(type).peek());
    }

    /**
     * Picks a random playable track, avoiding the one already assigned when possible.
     * Falls back to the vanilla track when nothing is playable.
     */
    static Music pickSlotMusic(Seq<Music> playable, Music fallback, Music current) {
        if (playable == null || playable.isEmpty()) {
            return fallback;
        }
        if (playable.size == 1) {
            return playable.first();
        }

        Seq<Music> candidates = new Seq<>();
        for (Music music : playable) {
            if (music != current) {
                candidates.add(music);
            }
        }
        return candidates.isEmpty() ? playable.first() : candidates.random();
    }

    /**
     * Removes disabled entries of the given type that refer to custom tracks which no
     * longer exist. Original-track entries and entries of other types are kept.
     */
    static Seq<String> pruneDisabled(Seq<String> disabled, MusicType type, Seq<String> validCustomKeys) {
        Seq<String> result = new Seq<>();
        String customPrefix = type.name() + "_c:";
        for (String key : disabled) {
            if (key.startsWith(customPrefix) && !validCustomKeys.contains(key)) {
                continue;
            }
            result.add(key);
        }
        return result;
    }

    /** Prunes stale disabled entries for a type, persisting only when something changed. */
    private void pruneDisabled(MusicType type, Seq<String> validCustomKeys) {
        Seq<String> disabled = disabledTracks.get();
        if (disabled == null) {
            return;
        }
        Seq<String> pruned = pruneDisabled(disabled, type, validCustomKeys);
        if (pruned.size != disabled.size) {
            disabledTracks.set(pruned);
        }
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

        if (Vars.control.sound.getCurrent() == state.music) {
            Vars.control.sound.stop();
        }
        state.music.dispose();
        musicCache.remove(file.name());
        trackStates.remove(state.disabledKey());

        setPaths(type, remaining);
        //also drops the disabled entry through pruning
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

        Seq<String> disabled = disabledTracks.get();
        Seq<String> next = new Seq<>(disabled != null ? disabled : new Seq<>());
        boolean changed = false;
        for (Music music : originals) {
            String key = TrackState.disabledKey(type, trackName(music), false);
            if (!next.contains(key)) {
                next.add(key);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        disabledTracks.set(next);
        loadMusicType(type);
        playRandom();
    }

    /**
     * Toggles a track's disabled state by adding or removing its key from the
     * persisted disabled list, then rebuilds the music for its type.
     */
    public void toggleDisabled(TrackState state) {
        Seq<String> disabled = disabledTracks.get();
        Seq<String> next = new Seq<>(disabled != null ? disabled : new Seq<>());
        String key = state.disabledKey();

        boolean nowDisabled;
        if (next.contains(key)) {
            next = without(next, key);
            nowDisabled = false;
        } else {
            next.add(key);
            nowDisabled = true;
        }
        disabledTracks.set(next);

        if (nowDisabled && Vars.control.sound.getCurrent() == state.music) {
            Vars.control.sound.stop();
        }
        loadMusicType(state.type);
        if (!state.type.isSlot()) {
            playRandom();
        }
    }

    /** Plays or stops the track through the game's player, keeping state authoritative. */
    public void togglePlay(TrackState state) {
        if (Vars.control.sound.getCurrent() == state.music) {
            Vars.control.sound.stop();
        } else {
            Vars.control.sound.playMusic(state.music, true);
        }
    }

    // endregion

    // region Helpers

    public String trackName(Music music) {
        Fi file = music.file;
        return file != null ? file.nameWithoutExtension() : "Unknown";
    }

    private String nameWithoutExtension(String path) {
        Fi file = Folders.musicsDir.child(path);
        return file.nameWithoutExtension();
    }

    private Music originalSlot(MusicType type) {
        return type == MusicType.MENU ? originalMenu : originalEditor;
    }

    private Music slotMusic(MusicType type) {
        return type == MusicType.MENU ? Musics.menu : Musics.editor;
    }

    private void setSlotMusic(MusicType type, Music music) {
        if (music == null) {
            return;
        }
        if (type == MusicType.MENU) {
            Musics.menu = music;
        } else {
            Musics.editor = music;
        }
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
        String key = TrackState.disabledKey(type, name, isCustom);
        TrackState existing = trackStates.get(key);
        if (existing != null && existing.music == music) {
            return existing;
        }
        TrackState state = new TrackState(music, type, name, isCustom, disabledTracks.signal(), currentMusic);
        trackStates.put(key, state);
        return state;
    }

    private @Nullable Seq<Music> getTargetList(MusicType type) {
        switch (type) {
        case AMBIENT:
            return Vars.control.sound.ambientMusic;
        case DARK:
            return Vars.control.sound.darkMusic;
        case BOSS:
            return Vars.control.sound.bossMusic;
        case MENU:
        case EDITOR:
        default:
            return null;
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
}
