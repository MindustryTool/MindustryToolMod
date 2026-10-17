package mindustrytool.features.displaygroup;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
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
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import solim.core.Component;
import solim.reactive.Readable;

public class DisplayGroupSettingsView extends Component {

    private final DisplayGroupFeature feature;

    public DisplayGroupSettingsView(DisplayGroupFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layoutCard();
            overlaysCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "DISPLAY");
    }

    private Component layoutCard() {
        return SettingCard.section(Icon.settings,
                Core.bundle.get("team-resources.settings.section.layout", "Layout"), () -> {
                    SettingSlider.of(
                            Core.bundle.get("feature.display-group.settings.columns"),
                            feature.colsConfig.signal(),
                            1, 8, 1,
                            feature.colsConfig.signal().map(v -> String.valueOf(v)));
                });
    }

    private Component overlaysCard() {
        return SettingCard.section(Icon.list,
                Core.bundle.get("feature.display-group.settings.visible-features"), () -> {
                    Readable<List<Feature>> orderedRows = feature.orderedFeaturesSignal();

                    reactiveGrid(orderedRows).key(f -> f.getMetadata().getId()).props(growX(), gap(unit(2)))
                            .items(f -> new FeatureOrderRow(feature, f));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.display-group.settings.reset-defaults"),
                    feature::resetToDefaults)
                            .style(WebStyles.outlineText())
                            .props(height(unit(11)), prefHeight(unit(11)), growX());
        });
    }

    private static final class FeatureOrderRow extends Component {
        private final DisplayGroupFeature feature;
        private final Feature f;

        FeatureOrderRow(DisplayGroupFeature feature, Feature f) {
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