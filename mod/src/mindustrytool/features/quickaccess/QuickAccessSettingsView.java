package mindustrytool.features.quickaccess;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.List;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import solim.core.BaseComponent;
import solim.signal.Readable;
import solim.signal.Signal;

public class QuickAccessSettingsView extends BaseComponent {

    private final QuickAccessFeature feature;

    public QuickAccessSettingsView(QuickAccessFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        feature.healDisplayOrder();
        return column().grow().center().children(() -> {
            scroll().growX().center()
                    .maxWidth(dvw(90f).map(w -> Math.min(w, 750f)))
                    .children(() -> {
                        column().growX().gap(unit(2)).children(() -> {
                            row().growX().gap(unit(2)).children(() -> {
                                text(Core.bundle.get("feature.quick-access.settings.opacity")).left();

                                spacer();
                                slider(feature.opacityConfig.signal(), 0.5f, 1.0f, 0.05f);

                                row().width(unit(14)).children(() -> {
                                    text(feature.opacityConfig.signal().map(v -> String.format("%.0f%%", v * 100)));
                                });
                            });

                            row().growX().gap(unit(2)).children(() -> {
                                text(Core.bundle.get("feature.quick-access.settings.scale")).left();
                                spacer();
                                slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);

                                row().width(unit(14)).children(() -> {
                                    text(feature.scaleConfig.signal().map(v -> String.format("%.0f%%", v * 100)));
                                });
                            });

                            row().growX().gap(unit(2)).children(() -> {
                                text(Core.bundle.get("feature.quick-access.settings.columns")).left();
                                spacer();
                                slider(feature.colsConfig.signal(), 1, 9, 1);

                                row().width(unit(14)).children(() -> {
                                    text(feature.colsConfig.signal().map(String::valueOf));
                                });
                            });

                            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                                    feature.hideDragHandleConfig.signal()).growX();

                            divider();

                            text(Core.bundle.get("feature.quick-access.settings.visible-features")).left().growX()
                                    .color(Color.white);

                            Readable<List<Feature>> orderedRows = feature.orderedFeaturesSignal();

                            reactiveGrid(Signal.of(1), orderedRows, f -> f.getMetadata().getId(),
                                    f -> new FeatureOrderRow(feature, f))
                                            .growX()
                                            .gap(unit(2));

                            divider();

                            button(Core.bundle.get("feature.quick-access.settings.reset-position"),
                                    feature::resetPosition)
                                            .style(Styles.defaultb).growX();
                        });
                    });
        }).element();
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
                        .children(() -> icon(FileIcon.of("chevron-up.png")));

                button(() -> feature.moveDown(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-access.settings.move-down"))
                        .disabled(() -> !canMoveDown.peek())
                        .children(() -> icon(FileIcon.of("chevron-down.png")));
            }).element();
        }
    }
}
