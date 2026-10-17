package mindustrytool.features.quickaccess;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.maxWidth;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.scene.Element;
import java.util.List;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import solim.core.Component;
import solim.reactive.Readable;

public class QuickAccessSettingsView extends Component {

    private final QuickAccessFeature feature;

    public QuickAccessSettingsView(QuickAccessFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {

        return column().props(grow(), maxWidth(750f), top(), center()).children(() -> {
            scroll().props(grow()).scrollX(false).children(() -> {
                column().props(growX(), padding(unit(2)), top(), center(), gap(unit(3))).children(() -> {
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
                            feature.hideDragHandleConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.quick-access.settings.collapsible"),
                            feature.collapsibleConfig.signal()).props(growX());
                });
    }

    private Component featuresCard() {
        return SettingCard.section(Icon.list,
                Core.bundle.get("feature.quick-access.settings.visible-features"), () -> {
                    Readable<List<Feature>> orderedRows = feature.orderedFeaturesSignal();

                    reactiveGrid(orderedRows)
                            .key(f -> f.getMetadata().getId())
                            .props(growX(), gap(unit(2)))
                            .items(f -> new FeatureOrderRow(feature, f));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.quick-access.settings.reset-position"),
                    feature::resetPosition)
                    .style(WebStyles.outlineText())
                    .props(height(unit(11)), prefHeight(unit(11)), growX());
        });
    }

    private static final class FeatureOrderRow extends Component {
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

            return row().props(growX(), gap(unit(2))).children(() -> {
                checkbox(f.getName(), feature.isFeatureVisible(id),
                        visible -> feature.setFeatureVisible(id, visible))
                        .props(growX());

                button(() -> feature.moveUp(id))
                        .style(WebStyles.ghost())
                        .props(size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.quick-access.settings.move-up")))
                        .disabled(() -> !canMoveUp.peek())
                        .children(() -> icon(FileIcon.of("chevron-up.png")).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                button(() -> feature.moveDown(id))
                        .style(WebStyles.ghost())
                        .props(size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.quick-access.settings.move-down")))
                        .disabled(() -> !canMoveDown.peek())
                        .children(() -> icon(FileIcon.of("chevron-down.png")).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
            }).element();
        }
    }
}