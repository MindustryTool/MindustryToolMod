package mindustrytool.features.settings;

import static solim.UI.*;
import static solim.modifier.Values.backgroundColor;
import static solim.modifier.Values.border;
import static solim.modifier.Values.color;
import static solim.modifier.Values.ellipsis;
import static solim.modifier.Values.enabled;
import static solim.modifier.Values.fontScale;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.onClick;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.wrap;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import java.util.HashSet;
import java.util.Set;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import solim.core.Component;

import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class FeatureCard extends Component {

    private final Feature feature;
    private final Readable<Boolean> reorderAllowed;

    public FeatureCard(Feature feature, Readable<Boolean> reorderAllowed) {
        this.feature = feature;
        this.reorderAllowed = reorderAllowed;
    }

    @Override
    protected Element build() {
        FeatureMetadata metadata = feature.getMetadata();
        boolean inDevelopment = metadata.isDevelopment();

        Readable<Color> statusColor = inDevelopment
                ? Readable.of(Color.orange)
                : feature.enabled().map(value -> Boolean.TRUE.equals(value) ? Color.green : Color.scarlet);

        Readable<String> statusText = inDevelopment
                ? Readable.of(Core.bundle.get("feature.status.in-development"))
                : feature.enabled().map(val -> Boolean.TRUE.equals(val) ? Core.bundle.get("feature.status.enabled")
                        : Core.bundle.get("feature.status.disabled"));

        Readable<Boolean> canMoveLeft = inDevelopment
                ? Readable.of(false)
                : Signal.computed(() -> Boolean.TRUE.equals(reorderAllowed.get())
                        && Boolean.TRUE.equals(FeatureManager.canMoveLeftSignal(feature).get()));

        Readable<Boolean> canMoveRight = inDevelopment
                ? Readable.of(false)
                : Signal.computed(() -> Boolean.TRUE.equals(reorderAllowed.get())
                        && Boolean.TRUE.equals(FeatureManager.canMoveRightSignal(feature).get()));

        Prov<SolimDialog> mainDialog = feature.getMainDialog();
        Prov<SolimDialog> settingDialog = feature.getSettingDialog();

        String id = metadata.getId();
        Readable<Boolean> isFavorite = ModSettings.favoriteFeatures.signal()
                .map(set -> set != null && set.contains(id));
        Readable<Color> starColor = isFavorite.map(fav -> Boolean.TRUE.equals(fav) ? Pal.accent : Color.gray);
        Readable<String> favoriteTooltip = isFavorite.map(fav -> Boolean.TRUE.equals(fav)
                ? Core.bundle.get("feature.button.favorite.remove")
                : Core.bundle.get("feature.button.favorite.add"));

        return card()
                .name("FeatureCard-" + metadata.getId()).props(height(unit(60)), prefHeight(unit(60)), growX(), rounded(unit(4)), border(1.5f, Color.darkGray), padding(unit(1)), backgroundColor(Color.black), onClick(() -> {
                    if (!inDevelopment) {
                        feature.setEnabled(!feature.isEnabled());
                    }
                })).children(() -> {
                    column().props(grow(), padding(unit(2)), gap(unit(2))).children(() -> {
                        row().props(growX(), center()).children(() -> {
                            row().props(gap(unit(2)), center()).children(() -> {
                                icon(metadata.getIcon()).props(size(unit(7)), prefWidth(unit(7)), prefHeight(unit(7)));
                                text(feature.getName()).props(style(Styles.defaultLabel), color(Color.white), ellipsis(true), left());
                            });

                            spacer();

                            button(() -> toggleFavorite(id))
                                    .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(favoriteTooltip))
                                    .children(() -> icon(Icon.star != null ? Icon.star : new TextureRegionDrawable())
                                            .props(size(unit(7)), prefWidth(unit(7)), prefHeight(unit(7)), color(starColor)));

                            if (mainDialog != null) {
                                button(() -> showDialog(mainDialog))
                                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.open-dialog")))
                                        .children(() -> icon(FileIcon.of("external-link.png")).props(size(unit(7)), prefWidth(unit(7)), prefHeight(unit(7))));
                            }

                            if (settingDialog != null) {
                                button(() -> showDialog(settingDialog))
                                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.settings")))
                                        .children(() -> icon(Icon.settings).props(size(unit(7)), prefWidth(unit(7)), prefHeight(unit(7))));
                            }

                            button(() -> new FeatureHelpDialog(feature).show())
                                    .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.help")))
                                    .children(() -> icon(FileIcon.of("info.png")).props(size(unit(7)), prefWidth(unit(7)), prefHeight(unit(7))));
                        });

                        text(feature.getDescription()).props(color(Color.lightGray), fontScale(0.9f), wrap(true), left());

                        spacer();

                        row().props(growX(), center()).children(() -> {
                            text(statusText).props(style(Styles.defaultLabel), color(statusColor), left());

                            spacer();

                            if (!inDevelopment) {
                                button(() -> FeatureManager.moveLeft(feature))
                                        .props(style(WebStyles.ghost()), size(unit(9)), prefWidth(unit(9)), prefHeight(unit(9)), enabled(canMoveLeft), tooltip(Core.bundle.get("feature.button.move-left")))
                                        .children(() -> icon(FileIcon.of("chevron-left.png", Icon.left)).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                                button(() -> FeatureManager.moveRight(feature))
                                        .props(style(WebStyles.ghost()), size(unit(9)), prefWidth(unit(9)), prefHeight(unit(9)), enabled(canMoveRight), tooltip(Core.bundle.get("feature.button.move-right")))
                                        .children(
                                                () -> icon(FileIcon.of("chevron-right.png", Icon.right)).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                            }
                        });

                        divider().props(color(statusColor));
                    });
                }).element();
    }

    private void showDialog(Prov<SolimDialog> dialogProvider) {
        @Nullable
        SolimDialog dialog = dialogProvider.get();
        if (dialog != null) {
            dialog.show();
        }
    }

    private void toggleFavorite(String id) {
        Set<String> current = ModSettings.favoriteFeatures.get();
        Set<String> updated = current != null ? new HashSet<>(current) : new HashSet<>();
        if (updated.contains(id)) {
            updated.remove(id);
        } else {
            updated.add(id);
        }
        ModSettings.favoriteFeatures.set(updated);
    }
}
