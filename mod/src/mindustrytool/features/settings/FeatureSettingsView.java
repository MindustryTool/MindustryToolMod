package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.signal.Computed;
import solim.signal.Signal;

public final class FeatureSettingsView extends BaseComponent {
    private final Signal<String> filter = Signal.of("");
    private final Computed<Float> contentWidth = dvw(90f).map(w -> w - unit(10));
    private final Computed<Integer> columnCount = contentWidth.map(w -> Math.max(1, (int) (w / 400f)));
    private final Computed<Seq<Feature>> filteredFeatures = filter.map(
            q -> FeatureManager.getFeatures().select(f -> matchesFilter(f, q != null ? q.trim().toLowerCase() : "")));

    @Override
    protected Element build() {
        return column().grow().children(() -> {
            toolbar();
            scroll().grow().children(() -> {
                grid(columnCount, //
                        filteredFeatures, //
                        feature -> feature.getMetadata().getId(), //
                        feature -> new FeatureCard(feature)//
                )//
                        .empty(() -> empty())//
                        .gap(unit(2));
            });
        }).element();
    }

    private void empty() {
        text(Core.bundle.get("feature.search.empty", "No features found")).color(Color.gray).padding(unit(4));
    }

    private void toolbar() {
        row().growX().gap(unit(2))
                .rounded(unit(3))
                .border(1.5f, Color.darkGray).children(() -> {
                    icon(Icon.zoom).size(unit(6)).marginLeft(unit(2));
                    textField(filter).growX().style(WebStyles.clearInput())
                            .placeholder(Core.bundle.get("feature.search.placeholder"));

                    button(FeatureManager::reenable).style(Styles.defaultb).width(unit(50)).height(unit(10))
                            .tooltip(Core.bundle.get("feature.button.re-enable.tooltip")).gap(unit(2)).children(() -> {
                                icon(Icon.refresh);
                                text(Core.bundle.get("feature.button.re-enable"));
                            });
                });
    }

    static boolean matchesFilter(Feature feature, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String q = query.trim().toLowerCase();

        return feature.getName().toLowerCase().contains(q);
    }
}
