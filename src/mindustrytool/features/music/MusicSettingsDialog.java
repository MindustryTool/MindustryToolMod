package mindustrytool.features.music;

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

        for (MusicType type : MusicType.values()) {
            renderSection(table, type);
            table.row();
        }

        ScrollPane pane = new ScrollPane(table);
        cont.add(pane).maxWidth(1000)
                .scrollX(false)
                .scrollY(true)
                .grow();
    }

    private void renderSection(Table table, MusicType type) {
        String title = switch (type) {
            case AMBIENT -> Core.bundle.get("music.type.ambient", "Ambient");
            case DARK -> Core.bundle.get("music.type.dark", "Dark");
            case BOSS -> Core.bundle.get("music.type.boss", "Boss");
        };

        Seq<Music> list = feature.getMusicList(type);

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
                                feature.addTrack(type, f);
                            }
                        }
                    } else {
                        feature.addTrack(type, file);
                    }

                    Core.app.post(this::rebuild);
                }, "ogg", "mp3");
            }).size(btnSize).padRight(5).tooltip(Core.bundle.get("music.tooltip.add", "Add custom music"));

            t.button(Icon.trash, () -> {
                feature.disableAllOriginals(type);
                rebuild();
            }).size(btnSize).tooltip(Core.bundle.get("music.tooltip.remove-original", "Disable all original sounds"));
        }).growX().row();

        table.table(t -> {
            t.left();

            for (Music music : list) {
                String name = feature.getMusicName(music);
                boolean isDisabled = feature.isTrackDisabled(music);
                boolean isCustom = feature.isCustomTrack(music);

                t.table(Styles.grayPanel, item -> {
                    item.left().margin(6);
                    Label label = item.add(name).ellipsis(true).growX().left().minWidth(0).get();
                    label.setEllipsis(true);

                    if (isDisabled) {
                        label.setColor(Color.gray);
                        label.color.a = 0.5f;
                    }

                    float itemBtnSize = Vars.mobile ? 40 : 32;

                    if (isCustom) {
                        item.button(Icon.trash, Styles.clearNonei, () -> {
                            feature.removeTrack(type, music);
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
                        feature.toggleTrack(music);
                        rebuild();
                    }).size(itemBtnSize)
                            .tooltip(Core.bundle.get("music.tooltip.toggle-disabled", "Toggle enabled/disabled"));
                }).growX().pad(2).row();
            }
        }).growX().padLeft(Vars.mobile ? 12 : 24).row();
    }
}
