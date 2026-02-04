package mindustrytool.features.music;

import java.util.function.Consumer;

import arc.Core;
import arc.audio.Music;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.Main;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class MusicSettingsDialog extends BaseDialog {
    private final MusicFeature feature;

    public MusicSettingsDialog(MusicFeature feature) {
        super(Core.bundle.get("feature.music.name", "Music Settings"));
        this.feature = feature;

        addCloseButton();
        onResize(this::rebuild);
        rebuild();
    }

    public void rebuild() {
        cont.clear();

        Table table = new Table();
        table.top().left();
        table.margin(Vars.mobile ? 10f : 20f);
        table.defaults().pad(4).fillX();

        renderSection(table, Core.bundle.get("music.type.ambient", "Ambient"), feature.getAllAmbient(),
                MusicConfig.getAmbientPaths(), MusicConfig::saveAmbientPaths);
        table.row();

        renderSection(table, Core.bundle.get("music.type.dark", "Dark"), feature.getAllDark(),
                MusicConfig.getDarkPaths(), MusicConfig::saveDarkPaths);
        table.row();

        renderSection(table, Core.bundle.get("music.type.boss", "Boss"), feature.getAllBoss(),
                MusicConfig.getBossPaths(), MusicConfig::saveBossPaths);

        ScrollPane pane = new ScrollPane(table);
        cont.add(pane).maxWidth(1000).grow();
    }

    private void renderSection(Table table, String title, Seq<Music> masterList, Seq<String> customPaths,
            Consumer<Seq<String>> pathSaver) {

        table.table(Styles.black6, t -> {
            t.margin(6);
            t.add(title).style(Styles.outlineLabel).left().growX().padLeft(8);

            float btnSize = Vars.mobile ? 48 : 40;

            t.button(Icon.add, () -> {
                Vars.platform.showMultiFileChooser(file -> {
                    if (file == null) {
                        Vars.ui.showErrorMessage("Invalid file");
                        return;
                    }

                    if (file.isDirectory()) {
                        for (Fi f : file.list()) {
                            if (!f.isDirectory() && (f.name().endsWith(".ogg") || f.name().endsWith(".mp3"))) {
                                var copy = Main.musicsDir.child(f.name());
                                f.copyFilesTo(copy);
                                customPaths.add(copy.name());
                            }
                        }
                    } else {
                        if (file.name().endsWith(".ogg") || file.name().endsWith(".mp3")) {
                            var copy = Main.musicsDir.child(file.name());
                            file.copyTo(copy);
                            customPaths.add(copy.name());
                            pathSaver.accept(customPaths);
                        } else {
                            Vars.ui.showErrorMessage("Invalid file type, only .ogg and .mp3 are supported");
                        }
                    }

                    feature.loadCustomMusic();
                    Vars.control.sound.playRandom();
                    Core.app.post(() -> rebuild());
                }, "ogg", "mp3");
            }).size(btnSize).padRight(5).tooltip(Core.bundle.get("music.tooltip.add", "Add custom music"));

            t.button(Icon.trash, () -> {

                masterList.removeAll(m -> {
                    Fi file = feature.getMusicFile(m);
                    return file == null || !customPaths.contains(file.name())
                            && !customPaths.contains(file.absolutePath());
                });
                feature.loadCustomMusic();
                rebuild();
                Vars.control.sound.playRandom();
            }).size(btnSize).tooltip(Core.bundle.get("music.tooltip.remove-original", "Remove all original sounds"));
        }).growX().row();

        table.table(t -> {
            t.left();
            Seq<String> disabled = MusicConfig.getDisabledSounds();

            for (Music music : masterList) {
                String name = feature.getMusicName(music);
                boolean isDisabled = disabled.contains(name);
                Fi musicFile = feature.getMusicFile(music);
                boolean isCustom = musicFile != null && (customPaths.contains(musicFile.name())
                        || customPaths.contains(musicFile.absolutePath()));

                t.table(Styles.grayPanel, item -> {
                    item.left().margin(6);
                    Label label = item.add(name).growX().left().minWidth(0).get();
                    label.setEllipsis(true);

                    if (isDisabled) {
                        label.setColor(Color.gray);
                        label.color.a = 0.5f;
                    }

                    float itemBtnSize = Vars.mobile ? 40 : 32;

                    if (isCustom) {
                        item.button(Icon.trash, Styles.clearNonei, () -> {
                            if (music.isPlaying())
                                music.stop();
                            customPaths.remove(musicFile.name());
                            customPaths.remove(musicFile.absolutePath());
                            pathSaver.accept(customPaths);
                            masterList.remove(music);
                            feature.loadCustomMusic();
                            rebuild();
                        }).size(itemBtnSize)
                                .tooltip(Core.bundle.get("music.tooltip.remove-custom", "Remove custom music"));
                    }

                    item.button(music.isPlaying() ? Icon.pause : Icon.play, Styles.clearNonei, () -> {
                        if (music.isPlaying()) {
                            music.stop();
                        } else {
                            music.play();
                        }
                        rebuild();
                    }).size(itemBtnSize).tooltip(Core.bundle.get("music.tooltip.play-stop", "Play/Stop"));

                    item.button(isDisabled ? Icon.cancel : Icon.ok, Styles.clearNonei, () -> {
                        if (isDisabled) {
                            disabled.remove(name);
                        } else {
                            disabled.add(name);
                            if (music.isPlaying())
                                music.stop();
                        }
                        MusicConfig.saveDisabledSounds(disabled);
                        feature.loadCustomMusic();
                        rebuild();
                    }).size(itemBtnSize)
                            .tooltip(Core.bundle.get("music.tooltip.toggle-disabled", "Toggle enabled/disabled"));
                }).growX().pad(2).row();
            }
        }).growX().padLeft(Vars.mobile ? 12 : 24).row();
    }
}
