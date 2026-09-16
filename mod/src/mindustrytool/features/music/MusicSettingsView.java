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
import solim.signal.Readable;
import solim.signal.Signal;

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
        return column().grow().center().children(() -> {
            scroll().grow().children(() -> {
                column().growX().gap(unit(3)).padding(unit(2)).children(() -> {
                    section(MusicType.AMBIENT);
                    divider();
                    section(MusicType.DARK);
                    divider();
                    section(MusicType.BOSS);
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

                button()
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.music.tooltip.disable-originals"))
                        .onClick(() -> feature.disableAllOriginals(type))
                        .children(() -> icon(Icon.trash).size(unit(6)));
            });

            grid(Signal.of(1), feature.trackSignal(type),
                    state -> state.disabledKey(),
                    state -> new TrackRow(feature, state))
                            .gap(unit(2))
                            .growX();
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
            Readable<Drawable> playIcon = state.isPlaying.map(playing -> playing
                    ? Icon.pause
                    : Icon.play);
            Readable<Drawable> disableIcon = state.isDisabled.map(disabled -> disabled
                    ? Icon.cancel
                    : Icon.ok);
            Readable<Color> labelColor = state.isDisabled.map(disabled -> disabled
                    ? Color.gray
                    : Color.white);

            return row().growX().gap(unit(1)).center()
                    .children(() -> {
                        text(state.name).growX().left().ellipsis(true).color(labelColor);

                        spacer();

                        if (state.isCustom) {
                            button()
                                    .style(WebStyles.ghost())
                                    .size(unit(11))
                                    .tooltip(Core.bundle.get("feature.music.tooltip.remove-custom"))
                                    .onClick(() -> feature.removeTrack(state.type(), state))
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
