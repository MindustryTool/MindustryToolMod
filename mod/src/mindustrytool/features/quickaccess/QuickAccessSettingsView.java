package mindustrytool.features.quickaccess;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import java.util.List;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;

public class QuickAccessSettingsView extends BaseComponent {

    private final QuickAccessFeature feature;

    public QuickAccessSettingsView(QuickAccessFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        // Granted wider-viewport exception (unify-remaining-settings-ui): dense reorderable grid needs up to 750f.
        return column().grow().top().center().maxWidth(750f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().top().center().gap(unit(3)).padding(unit(2)).children(() -> {
                    headerCard();
                    appearanceCard();
                    featuresCard();
                    actionsCard();
                });
            });
        }).element();
    }

    private Component headerCard() {
        return SettingCard.header(feature, "HUD OVERLAY");
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.settings,
                Core.bundle.get("team-resources.settings.section.appearance", "Appearance & Behavior"), () -> {
                    SettingSlider.of(
                            Core.bundle.get("feature.quick-access.settings.opacity"),
                            feature.opacityConfig.signal(),
                            0.5f, 1.0f, 0.05f,
                            feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-access.settings.scale"),
                            feature.scaleConfig.signal(),
                            0.5f, 1.5f, 0.1f,
                            feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-access.settings.columns"),
                            feature.colsConfig.signal(),
                            1, 9, 1,
                            feature.colsConfig.signal().map(v -> String.valueOf(v)));

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.quick-access.settings.collapsible"),
                            feature.collapsibleConfig.signal()).growX();
                });
    }

    private Component featuresCard() {
        return SettingCard.section(Icon.list,
                Core.bundle.get("feature.quick-access.settings.visible-features"), () -> {
                    Readable<List<Feature>> orderedRows = feature.orderedFeaturesSignal();

                    reactiveGrid(orderedRows).key(f -> f.getMetadata().getId()).growX().gap(unit(2))
                            .children(f -> new FeatureOrderRow(feature, f));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.quick-access.settings.reset-position"),
                    feature::resetPosition)
                            .style(WebStyles.outlineText())
                            .height(unit(11))
                            .growX();
        });
    }

    private static final class FeatureOrderRow extends BaseComponent {
        private final QuickAccessFeature feature;
        private final Feature f;

        FeatureOrderRow(QuickAccessFeature feature, Feature f) {
            this.feature = feature;
            this.f = f;
        }

        @Override
        protected Element build() {
            String id = f.getMetadata().getId();
            Readable<Boolean> canMoveUp = feature.canMoveUpSignal(id);
            Readable<Boolean> canMoveDown = feature.canMoveDownSignal(id);

            return row().growX().gap(unit(2)).children(() -> {
                checkbox(f.getName(), feature.isFeatureVisible(id),
                        visible -> feature.setFeatureVisible(id, visible))
                                .growX();

                button(() -> feature.moveUp(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-access.settings.move-up"))
                        .disabled(() -> !canMoveUp.peek())
                        .children(() -> icon(FileIcon.of("chevron-up.png")).size(unit(6)));

                button(() -> feature.moveDown(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-access.settings.move-down"))
                        .disabled(() -> !canMoveDown.peek())
                        .children(() -> icon(FileIcon.of("chevron-down.png")).size(unit(6)));
            }).element();
        }
    }
}
