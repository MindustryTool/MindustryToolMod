package mindustrytool.features.music;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import mindustry.gen.Icon;
import mindustry.ui.FileChooser;
import mindustrytool.components.SettingCard;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;

public class MusicSettingsView extends Component {

    private final MusicFeature feature;

    public MusicSettingsView(MusicFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> nowPlaying = feature.currentMusic().map(music -> music == null || music.file == null
                ? Core.bundle.get("feature.music.now-playing.none")
                : Core.bundle.format("feature.music.now-playing", music.file.nameWithoutExtension()));

        return column().props(grow(), maxWidth(750f), top(), center()).children(() -> {
            scroll().props(grow(), scrollX(false)).children(() -> {
                column().props(growX(), padding(unit(2)), top(), center(), gap(unit(3))).children(() -> {
                    headerCard();

                    text(nowPlaying).props(color(WebStyles.Colors.GHOST_FG), growX(), left());

                    for (MusicType type : MusicType.values()) {
                        section(type);
                    }
                });
            });
        }).element();
    }

    private Component headerCard() {
        return SettingCard.header(feature, "MEDIA");
    }

    private Component section(MusicType type) {
        return SettingCard.section(Icon.play, Core.bundle.get(type.labelKey()), () -> {
            row().props(growX(), gap(unit(1)), center()).children(() -> {
                spacer();

                button()
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.add")))
                        .onClick(() -> openPicker(type))
                        .children(() -> icon(Icon.add).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                if (!type.isSlot()) {
                    button()
                            .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.disable-originals")))
                            .onClick(() -> feature.disableAllOriginals(type))
                            .children(() -> icon(Icon.trash).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                }
            });

            reactiveGrid(feature.trackSignal(type))
                    .key(state -> state.disabledKey())
                    .props(growX(), gap(unit(2)))
                    .items(state -> new TrackRow(feature, state));
        });
    }

    private void openPicker(MusicType type) {
        FileChooser.open("ogg", "mp3", "wav").submit(file -> {
            if (file == null) {
                return;
            }
            if (file.isDirectory()) {
                for (Fi child : file.list()) {
                    if (!child.isDirectory()
                            && MusicFeature.VALID_EXTENSIONS.contains(child.extension().toLowerCase())) {
                        feature.addTrack(type, child);
                    }
                }
            } else {
                feature.addTrack(type, file);
            }
        });
    }

    private static class TrackRow extends Component {

        private final MusicFeature feature;
        private final TrackState state;

        TrackRow(MusicFeature feature, TrackState state) {
            this.feature = feature;
            this.state = state;
        }

        @Override
        protected Element build() {
            Readable<Drawable> playIcon = state.isPlaying().map(playing -> playing
                    ? Icon.pause
                    : Icon.play);
            Readable<Drawable> disableIcon = state.isDisabled().map(disabled -> disabled
                    ? Icon.cancel
                    : Icon.ok);
            Readable<Color> labelColor = state.isDisabled().map(disabled -> disabled
                    ? Color.gray
                    : Color.white);

            return row().props(growX(), gap(unit(1)), center())
                    .children(() -> {
                        column().props(growX(), gap(unit(2))).children(() -> {
                            text(state.name)
                                    .props(color(labelColor), growX(), left(), ellipsis(true));

                            when(state.isPlaying())
                                    .thenDo(() -> stack().props(growX())
                                            .layer(() -> divider().props(color(Color.white), growX(), height(4f)))
                                            .layer(parent -> divider().props(color(Color.green), height(4f), update(div -> {
                                                div.setWidth(state.music.getPosition() / state.music.getLength()
                                                        * parent.getWidth());
                                            }))))
                                    .props(growX());
                        });

                        if (state.isCustom) {
                            button()
                                    .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.rename-custom")))
                                    .onClick(() -> feature.showRenameDialog(state))
                                    .children(() -> icon(Icon.edit).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(labelColor)));

                            button()
                                    .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.remove-custom")))
                                    .onClick(() -> feature.removeTrack(state.type, state))
                                    .children(() -> icon(Icon.trash).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(labelColor)));
                        }

                        button()
                                .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.play-stop")))
                                .onClick(() -> feature.togglePlay(state))
                                .children(() -> icon(playIcon).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(labelColor)));

                        button()
                                .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.music.tooltip.toggle-disabled")))
                                .onClick(() -> feature.toggleDisabled(state))
                                .children(() -> icon(disableIcon).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(labelColor)));
                    }).element();
        }
    }
}