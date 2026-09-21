package mindustrytool.features.music;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import mindustry.gen.Icon;
import mindustry.ui.FileChooser;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;

/**
 * Settings view for the Custom Music feature: Ambient, Dark, and Boss sections,
 * each listing tracks with reactive play and disable toggles.
 */
public class MusicSettingsView extends BaseComponent {

    private final MusicFeature feature;

    public MusicSettingsView(MusicFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> nowPlaying = feature.currentMusic().map(music -> music == null || music.file == null
                ? Core.bundle.get("feature.music.now-playing.none")
                : Core.bundle.format("feature.music.now-playing", music.file.nameWithoutExtension()));

        return column().grow().center().children(() -> {
            scroll().grow().children(() -> {
                column().growX().gap(unit(3)).padding(unit(2)).children(() -> {
                    text(nowPlaying).growX().left().color(WebStyles.Colors.GHOST_FG);

                    MusicType[] types = MusicType.values();
                    for (int i = 0; i < types.length; i++) {
                        if (i > 0) {
                            divider();
                        }
                        section(types[i]);
                    }
                });
            });
        }).element();
    }

    private Component section(MusicType type) {
        return column().growX().gap(unit(1)).children(() -> {
            row().growX().gap(unit(1)).center().children(() -> {
                text(Core.bundle.get(type.labelKey())).growX().left().color(WebStyles.Colors.GHOST_FG);

                spacer();

                button()
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.music.tooltip.add"))
                        .onClick(() -> openPicker(type))
                        .children(() -> icon(Icon.add).size(unit(6)));

                //slot types (menu/editor) have no original rotation to disable
                if (!type.isSlot()) {
                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.music.tooltip.disable-originals"))
                            .onClick(() -> feature.disableAllOriginals(type))
                            .children(() -> icon(Icon.trash).size(unit(6)));
                }
            });

            reactiveGrid(feature.trackSignal(type)).key(state -> state.disabledKey()).gap(unit(2)).growX()
                    .children(state -> new TrackRow(feature, state));
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

    /**
     * A single track row: name label plus custom-remove, play/pause, and disable
     * buttons. All buttons bind to the row's {@link TrackState} signals and update
     * reactively.
     */
    private static class TrackRow extends BaseComponent {

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

            return row().growX().gap(unit(1)).center()
                    .children(() -> {
                        column().growX().gap(unit(2)).children(() -> {
                            text(state.name).growX().left().ellipsis(true).color(labelColor);

                            dynamic(state.isPlaying(), playing -> {
                                if (Boolean.TRUE.equals(playing)) {
                                    return stack().growX()
                                            .layer(() -> divider().color(Color.white).growX().height(4))
                                            .layer(parent -> divider().color(Color.green).height(4).update(div -> {
                                                    div.setWidth(state.music.getPosition() / state.music.getLength()
                                                            * parent.getWidth());
                                                }));
                                }
                                return null;
                            }).growX();
                        });

                        if (state.isCustom) {
                            button()
                                    .style(WebStyles.ghost())
                                    .size(unit(11))
                                    .tooltip(Core.bundle.get("feature.music.tooltip.rename-custom"))
                                    .onClick(() -> feature.showRenameDialog(state))
                                    .children(() -> icon(Icon.edit).size(unit(6)).color(labelColor));

                            button()
                                    .style(WebStyles.ghost())
                                    .size(unit(11))
                                    .tooltip(Core.bundle.get("feature.music.tooltip.remove-custom"))
                                    .onClick(() -> feature.removeTrack(state.type, state))
                                    .children(() -> icon(Icon.trash).size(unit(6)).color(labelColor));
                        }

                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.music.tooltip.play-stop"))
                                .onClick(() -> feature.togglePlay(state))
                                .children(() -> icon(playIcon).size(unit(6)).color(labelColor));

                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.music.tooltip.toggle-disabled"))
                                .onClick(() -> feature.toggleDisabled(state))
                                .children(() -> icon(disableIcon).size(unit(6)).color(labelColor));
                    }).element();
        }
    }
}
