package mindustrytool.features.music;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.files.Fi;
import arc.scene.ui.Dialog;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.events.MusicRegisterEvent;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

public class MusicFeature implements Feature {
    private final Seq<Music> allAmbient = new Seq<>();
    private final Seq<Music> allDark = new Seq<>();
    private final Seq<Music> allBoss = new Seq<>();

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
            // Capture original music first
            allAmbient.set(Vars.control.sound.ambientMusic);
            allDark.set(Vars.control.sound.darkMusic);
            allBoss.set(Vars.control.sound.bossMusic);

            loadCustomMusic();
        });
    }

    @Override
    public void onEnable() {
        allAmbient.set(Vars.control.sound.ambientMusic);
        allDark.set(Vars.control.sound.darkMusic);
        allBoss.set(Vars.control.sound.bossMusic);
    }

    @Override
    public void onDisable() {
        // Restore original music on disable?
        Vars.control.sound.ambientMusic.set(allAmbient);
        Vars.control.sound.darkMusic.set(allDark);
        Vars.control.sound.bossMusic.set(allBoss);
    }

    public void loadCustomMusic() {
        Log.info("Loading custom music...");

        loadType(MusicConfig.getAmbientPaths(), Vars.control.sound.ambientMusic, allAmbient);
        loadType(MusicConfig.getDarkPaths(), Vars.control.sound.darkMusic, allDark);
        loadType(MusicConfig.getBossPaths(), Vars.control.sound.bossMusic, allBoss);

        MusicConfig.saveBossPaths(MusicConfig.getBossPaths());
        MusicConfig.saveDarkPaths(MusicConfig.getDarkPaths());
        MusicConfig.saveAmbientPaths(MusicConfig.getAmbientPaths());

        Vars.control.sound.playRandom();
    }

    private void loadType(Seq<String> paths, Seq<Music> masterList, Seq<Music> original) {
        var deleted = new Seq<String>();

        masterList.clear();
        masterList.addAll(original);

        for (String path : paths) {
            try {
                Fi file = Core.files.absolute(path);

                if (file.exists()) {
                    try {
                        Log.info("Loading music file: @", file.absolutePath());
                        Music music = new Music(file);
                        masterList.add(music);
                    } catch (Exception e) {
                        Log.err("Failed to create music instance for @", path, e);
                        Vars.ui.showException(e);
                    }
                } else {
                    Log.warn("Music file not found: @", path);
                    Vars.ui.showInfoFade("File not exists: " + path);
                    deleted.add(path);
                }
            } catch (Exception e) {
                Log.err("Failed to load music: " + path, e);
            }
        }

        paths.removeAll(deleted);

        Seq<String> disabled = MusicConfig.getDisabledSounds();
        masterList.removeAll(m -> disabled.contains(getMusicName(m)));
    }

    public Seq<Music> getAllAmbient() {
        return allAmbient;
    }

    public Seq<Music> getAllDark() {
        return allDark;
    }

    public Seq<Music> getAllBoss() {
        return allBoss;
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
