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
import mindustrytool.events.MusicRegisterEvent;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

public class MusicFeature implements Feature {
    private final ObjectMap<String, Music> loadedMusic = new ObjectMap<>();
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

        loadType(MusicConfig.getAmbientPaths(), allAmbient);
        loadType(MusicConfig.getDarkPaths(), allDark);
        loadType(MusicConfig.getBossPaths(), allBoss);

        applyDisabledFilter();
    }

    private void loadType(Seq<String> paths, Seq<Music> masterList) {
        for (String path : paths) {
            try {
                if (loadedMusic.containsKey(path)) {
                    if (!masterList.contains(loadedMusic.get(path))) {
                        masterList.add(loadedMusic.get(path));
                    }
                    continue;
                }

                Fi file = Core.files.absolute(path);

                if (file.exists()) {
                    try {
                        Music music = new Music(file);
                        masterList.add(music);
                        loadedMusic.put(path, music);
                    } catch (Exception e) {
                        Vars.ui.showException(e);
                    }
                } else {
                    Vars.ui.showInfoFade("File not exists: " + path);
                }
            } catch (Exception e) {
                Log.err("Failed to load music: " + path, e);
            }
        }
    }

    public void applyDisabledFilter() {
        Seq<String> disabled = MusicConfig.getDisabledSounds();

        syncSequence(Vars.control.sound.ambientMusic, allAmbient, disabled);
        syncSequence(Vars.control.sound.darkMusic, allDark, disabled);
        syncSequence(Vars.control.sound.bossMusic, allBoss, disabled);
    }

    private void syncSequence(Seq<Music> target, Seq<Music> source, Seq<String> disabled) {
        target.clear();
        for (Music m : source) {
            if (!disabled.contains(getMusicName(m))) {
                target.add(m);
            }
        }
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
