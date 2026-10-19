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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.game.EventType.MusicRegisterEvent;
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

public class MusicFeature extends Feature {

    public static final Seq<String> VALID_EXTENSIONS = Seq.with("mp3", "ogg", "wav");
    private static final int MAX_NAME_SUFFIX = 1000;

    public final ConfigGroup config;
    public final ConfigValue<Seq<String>> ambientPaths;
    public final ConfigValue<Seq<String>> darkPaths;
    public final ConfigValue<Seq<String>> bossPaths;
    public final ConfigValue<Seq<String>> menuPaths;
    public final ConfigValue<Seq<String>> editorPaths;

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

        Timer.schedule(() -> {
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
        }, 0, 0.1f);

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                captureOriginalMusic();
                loadAllCustomMusic();
            }
        }));

        Events.on(MusicRegisterEvent.class, e -> Core.app.post(() -> {
            if (isEnabled()) {
                recaptureOriginalMusic();
                loadAllCustomMusic();
            }
        }));
    }

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

    public Readable<Music> currentMusic() {
        return currentMusic;
    }

    @Override
    public void onEnable() {
        captureOriginalMusic();
        loadAllCustomMusic();
    }

    @Override
    public void onDisable() {
        restoreOriginalMusic();
    }

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

    private void recaptureOriginalMusic() {

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

        Vars.control.sound.stop();
    }

    public void loadAllCustomMusic() {
        for (MusicType type : MusicType.values()) {
            loadMusicType(type);
        }
    }

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

    private void assignSlot(MusicType type, Seq<TrackState> states) {
        Seq<Music> playable = new Seq<>();
        for (TrackState state : states) {
            if (!state.disabled()) {
                playable.add(state.music);
            }
        }

        setSlotMusic(type, pickSlotMusic(playable, originalSlot(type), slotMusic(type)));
    }

    private void rerollSlot(MusicType type) {
        if (!isEnabled()) {
            return;
        }

        assignSlot(type, trackSignals.get(type).peek());
    }

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

    static boolean isReadable(@Nullable Fi file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return true;
        }
        try (InputStream in = file.read()) {
            return in != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    static String getAndroidDisplayName(Fi file) {
        if (!Vars.android) {
            return null;
        }
        try {
            Object uriObj = null;
            for (Field field : file.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(file);
                if (val != null && val.getClass().getName().contains("Uri")) {
                    uriObj = val;
                    break;
                }
            }
            if (uriObj == null) {
                return null;
            }

            Method getContentResolver = Core.app.getClass().getMethod("getContentResolver");
            Object resolver = getContentResolver.invoke(Core.app);
            if (resolver == null) {
                return null;
            }

            Method queryMethod = null;
            for (Method m : resolver.getClass().getMethods()) {
                if ("query".equals(m.getName()) && m.getParameterTypes().length == 5) {
                    queryMethod = m;
                    break;
                }
            }
            if (queryMethod == null) {
                return null;
            }

            String[] projection = new String[] { "_display_name" };
            Object cursor = queryMethod.invoke(resolver, uriObj, projection, null, null, null);
            if (cursor == null) {
                return null;
            }

            try {
                Method moveToFirst = cursor.getClass().getMethod("moveToFirst");
                Method getColumnIndex = cursor.getClass().getMethod("getColumnIndex", String.class);
                Method getString = cursor.getClass().getMethod("getString", int.class);

                boolean hasFirst = (Boolean) moveToFirst.invoke(cursor);
                if (hasFirst) {
                    int colIndex = (Integer) getColumnIndex.invoke(cursor, "_display_name");
                    if (colIndex >= 0) {
                        return (String) getString.invoke(cursor, colIndex);
                    }
                }
            } finally {
                Method close = cursor.getClass().getMethod("close");
                close.invoke(cursor);
            }
        } catch (Throwable t) {
            Log.warn("Failed to query Android display name: " + t.getMessage());
        }
        return null;
    }

    static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    static String sanitizeFileName(String name) {
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        while (sanitized.startsWith("_")) {
            sanitized = sanitized.substring(1);
        }
        while (sanitized.endsWith("_")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.isEmpty() ? "track" : sanitized;
    }

    static String resolveTrackName(Fi file) {
        String name = getAndroidDisplayName(file);
        if (name == null || name.trim().isEmpty()) {
            name = file.name();
        }
        name = sanitizeFileName(name);
        String ext = extensionOf(name);
        if (ext.isEmpty()) {

            name = name + ".mp3";
        }
        return name;
    }

    public void addTrack(MusicType type, @Nullable Fi file) {
        if (!isReadable(file)) {
            Vars.ui.showErrorMessage(Core.bundle.get("feature.music.error.invalid-file"));
            return;
        }

        String resolvedName = resolveTrackName(file);
        String ext = extensionOf(resolvedName);
        if (!VALID_EXTENSIONS.contains(ext)) {
            Vars.ui.showErrorMessage(
                    Core.bundle.format("feature.music.error.invalid-type", VALID_EXTENSIONS.toString(", ")));
            return;
        }

        try {
            Fi copy = uniqueTarget(Folders.musicsDir.child(resolvedName));
            copy.write(file.read(), false);

            Seq<String> paths = pathsFor(type).copy();
            paths.add(copy.name());
            setPaths(type, paths);
            loadMusicType(type);
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }

    public void showRenameDialog(TrackState state) {
        if (!state.isCustom || state.music == null || state.music.file == null) {
            return;
        }
        Vars.ui.showTextInput(
                Core.bundle.get("feature.music.dialog.rename.title"),
                Core.bundle.get("feature.music.dialog.rename.text"),
                state.name,
                newName -> renameTrack(state.type, state, newName));
    }

    public void renameTrack(MusicType type, TrackState state, @Nullable String newName) {
        if (newName == null) {
            return;
        }
        String cleanName = sanitizeFileName(newName.trim());
        if (cleanName.isEmpty() || cleanName.equalsIgnoreCase(state.name)) {
            return;
        }

        Fi oldFile = state.music.file;
        if (oldFile == null || !state.isCustom) {
            return;
        }

        String ext = oldFile.extension();
        String newFileName = ext.isEmpty()
                ? cleanName
                : (cleanName.toLowerCase().endsWith("." + ext.toLowerCase()) ? cleanName : cleanName + "." + ext);
        Fi newFile = uniqueTarget(Folders.musicsDir.child(newFileName));
        if (oldFile.equals(newFile)) {
            return;
        }

        try {
            if (Vars.control != null && Vars.control.sound != null && Vars.control.sound.getCurrent() == state.music) {
                Vars.control.sound.stop();
            }

            oldFile.moveTo(newFile);

            Seq<String> paths = pathsFor(type).copy();
            int index = paths.indexOf(oldFile.name());
            if (index < 0) {
                index = paths.indexOf(oldFile.nameWithoutExtension());
            }
            if (index >= 0) {
                paths.set(index, newFile.name());
                setPaths(type, paths);
            }

            Seq<String> disabled = disabledTracks.get();
            if (disabled != null) {
                String oldKey = state.disabledKey();
                String newKey = TrackState.disabledKey(type, newFile.nameWithoutExtension(), true);
                if (disabled.contains(oldKey)) {
                    Seq<String> nextDisabled = new Seq<>(disabled);
                    nextDisabled.remove(oldKey);
                    nextDisabled.add(newKey);
                    disabledTracks.set(nextDisabled);
                }
            }

            state.music.dispose();
            musicCache.remove(oldFile.name());
            trackStates.remove(state.disabledKey());

            loadMusicType(type);
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }

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

        loadMusicType(type);
    }

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

    public void togglePlay(TrackState state) {
        if (Vars.control.sound.getCurrent() == state.music) {
            Vars.control.sound.stop();
        } else {
            Vars.control.sound.playMusic(state.music, true);
        }
    }

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
