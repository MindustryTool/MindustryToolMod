package mindustrytool.features.displaygroup;

import static solim.UI.*;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.List;
import java.util.Set;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class DisplayGroupPopupView extends Component {

    private final DisplayGroupFeature feature;

    public DisplayGroupPopupView(DisplayGroupFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<List<Feature>> items = Signal.computed(() -> {
            Seq<String> stored = feature.displayOrderConfig.signal().get();
            Set<String> hidden = feature.hiddenFeaturesConfig.signal().get();
            Set<String> shown = feature.shownFeaturesConfig.signal().get();
            Seq<String> normalized = DisplayGroupFeature.normalizeDisplayOrder(
                    feature.displayCandidates(), stored);
            return feature.computeVisibleItems(normalized, hidden, shown);
        });

        Readable<Integer> columns = Signal.computed(() -> {
            Integer cols = feature.colsConfig.signal().get();
            int maxCols = cols != null ? cols : 4;
            List<Feature> currentItems = items.get();
            int count = currentItems != null ? currentItems.size() : 0;
            return Math.max(1, Math.min(maxCols, count));
        });

        return row()
                .props(gap(unit(1)), center())
                .children(() -> {
                    reactiveGrid(items)
                            .props(gap(unit(1)))
                            .key(f -> f.getMetadata().getId())
                            .columns(columns)
                            .items(this::itemButton);
                })
                .element();
    }

    private Component itemButton(Feature f) {
        FeatureMetadata meta = f.getMetadata();

        return button()
                .style(WebStyles.ghost())
                .props(size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(f.getName()))
                .onClick(() -> f.setEnabled(!f.isEnabled()))
                .onLongClick(300L, () -> f.onQuickAccessLongClick(null))
                .children(() -> icon(meta.getIcon()).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)))
                        .color(f.enabled().map(en -> Boolean.TRUE.equals(en) ? Color.white : Color.darkGray)));
    }
}