package mindustrytool.features.music;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.files.Fi;
import arc.scene.ui.Dialog;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.Main;
import mindustrytool.events.MusicRegisterEvent;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

public class MusicFeature implements Feature {
    private final ObjectMap<MusicType, Seq<Music>> originalMusic = new ObjectMap<>();
    private final ObjectMap<MusicType, Seq<Music>> allMusic = new ObjectMap<>();
    private final ObjectMap<String, Music> musicCache = new ObjectMap<>();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.music")
                .description("Custom Music Loader")
                .icon(Icon.play)
                .build();
    }

    @Override
    public void init() {
        Events.on(MusicRegisterEvent.class, e -> {
            captureOriginalMusic();
            loadAllCustomMusic();
        });
    }

    @Override
    public void onEnable() {
        if (originalMusic.isEmpty()) {
            captureOriginalMusic();
        }
        loadAllCustomMusic();
    }

    @Override
    public void onDisable() {
        restoreOriginalMusic();
    }

    private void captureOriginalMusic() {
        if (!originalMusic.isEmpty())
            return;

        originalMusic.put(MusicType.AMBIENT, new Seq<>(Vars.control.sound.ambientMusic));
        originalMusic.put(MusicType.DARK, new Seq<>(Vars.control.sound.darkMusic));
        originalMusic.put(MusicType.BOSS, new Seq<>(Vars.control.sound.bossMusic));
    }

    private void restoreOriginalMusic() {
        if (originalMusic.isEmpty())
            return;

        Vars.control.sound.ambientMusic.set(originalMusic.get(MusicType.AMBIENT));
        Vars.control.sound.darkMusic.set(originalMusic.get(MusicType.DARK));
        Vars.control.sound.bossMusic.set(originalMusic.get(MusicType.BOSS));
    }

    public void loadAllCustomMusic() {
        Log.info("Loading custom music...");
        for (MusicType type : MusicType.values()) {
            loadMusicType(type);
        }
    }

    private void loadMusicType(MusicType type) {
        Seq<Music> targetList = getTargetList(type);
        Seq<Music> originals = originalMusic.get(type);
        Seq<String> customPaths = MusicConfig.getPaths(type);
        Seq<String> disabled = MusicConfig.getDisabledSounds();
        Seq<String> deleted = new Seq<>();

        if (targetList == null || originals == null) {
            return;
        }

        Seq<Music> completeList = new Seq<>();
        completeList.addAll(originals);

        for (String path : customPaths) {
            try {
                Fi file = Main.musicsDir.child(path);
                if (!file.exists()) {
                    file = Core.files.absolute(path);
                }

                if (file.exists()) {
                    String absPath = file.absolutePath();
                    Music music = musicCache.get(absPath);

                    if (music == null) {
                        try {
                            music = new Music(file);
                            musicCache.put(absPath, music);
                        } catch (Exception e) {
                            Log.err("Failed to create music instance for @", path, e);
                            Vars.ui.showException(e);
                            continue;
                        }
                    }
                    completeList.add(music);
                } else {
                    Log.warn("Music file not found: @", path);
                    deleted.add(path);
                }
            } catch (Exception e) {
                Log.err("Failed to load music: " + path, e);
            }
        }

        if (!deleted.isEmpty()) {
            customPaths.removeAll(deleted);
            MusicConfig.savePaths(type, customPaths);
        }

        allMusic.put(type, completeList);

        // Update target list
        targetList.clear();
        for (Music m : completeList) {
            if (!disabled.contains(getMusicName(m))) {
                targetList.add(m);
            }
        }
    }

    public Seq<Music> getMusicList(MusicType type) {
        if (!allMusic.containsKey(type)) {
            return originalMusic.get(type, new Seq<>());
        }
        return allMusic.get(type);
    }

    private Seq<Music> getTargetList(MusicType type) {
        return switch (type) {
            case AMBIENT -> Vars.control.sound.ambientMusic;
            case DARK -> Vars.control.sound.darkMusic;
            case BOSS -> Vars.control.sound.bossMusic;
        };
    }

    public void addTrack(MusicType type, Fi file) {
        if (file == null || !file.exists()) {
            Vars.ui.showErrorMessage("Invalid file, " + file);
            return;
        }

        String ext = file.extension();
        if (!ext.equals("ogg") && !ext.equals("mp3")) {
            Vars.ui.showErrorMessage("Invalid file type, only .ogg and .mp3 are supported");
            return;
        }

        try {
            Fi copy = Main.musicsDir.child(file.name());
            file.copyTo(copy);

            Seq<String> paths = MusicConfig.getPaths(type);
            if (!paths.contains(copy.name())) {
                paths.add(copy.name());
                MusicConfig.savePaths(type, paths);
                loadMusicType(type);
            }
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }

    public void removeTrack(MusicType type, Music music) {
        Fi file = getMusicFile(music);
        if (file == null)
            return;

        Seq<String> paths = MusicConfig.getPaths(type);
        boolean removed = paths.remove(file.name()) || paths.remove(file.absolutePath());

        if (removed) {
            if (music.isPlaying())
                music.stop();

            // Dispose and remove from cache
            String absPath = file.absolutePath();
            if (musicCache.containsKey(absPath)) {
                musicCache.remove(absPath);
                music.dispose();
            }

            MusicConfig.savePaths(type, paths);
            loadMusicType(type);
        }
    }

    public void disableAllOriginals(MusicType type) {
        Seq<Music> originals = originalMusic.get(type);
        Seq<String> disabled = MusicConfig.getDisabledSounds();
        boolean changed = false;

        for (Music m : originals) {
            String name = getMusicName(m);
            if (!disabled.contains(name)) {
                disabled.add(name);
                changed = true;
            }
        }

        if (changed) {
            MusicConfig.saveDisabledSounds(disabled);
            loadMusicType(type);
            Vars.control.sound.playRandom();
        }
    }

    public void removeAllCustom(MusicType type) {
        Seq<String> paths = MusicConfig.getPaths(type);

        // Dispose all custom music for this type
        for (String path : paths) {
            Fi file = Main.musicsDir.child(path);
            if (file.exists()) {
                String absPath = file.absolutePath();
                Music m = musicCache.get(absPath);
                if (m != null) {
                    if (m.isPlaying())
                        m.stop();
                    m.dispose();
                    musicCache.remove(absPath);
                }
            }
        }

        paths.clear();
        MusicConfig.savePaths(type, paths);
        loadMusicType(type);
        Vars.control.sound.playRandom();
    }

    public void toggleTrack(Music music) {
        String name = getMusicName(music);
        Seq<String> disabled = MusicConfig.getDisabledSounds();

        if (disabled.contains(name)) {
            disabled.remove(name);
        } else {
            disabled.add(name);
            if (music.isPlaying())
                music.stop();
        }

        MusicConfig.saveDisabledSounds(disabled);
        loadAllCustomMusic();
    }

    public boolean isTrackDisabled(Music music) {
        return MusicConfig.getDisabledSounds().contains(getMusicName(music));
    }

    public boolean isCustomTrack(Music music) {
        Fi file = getMusicFile(music);
        if (file == null)
            return false;
        return file.parent().equals(Main.musicsDir);
    }

    public String getMusicName(Music music) {
        Fi file = getMusicFile(music);
        if (file == null)
            return "Unknown";
        return file.nameWithoutExtension();
    }

    public Fi getMusicFile(Music music) {
        try {
            return Reflect.get(music, "file");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new MusicSettingsDialog(this));
    }
}
