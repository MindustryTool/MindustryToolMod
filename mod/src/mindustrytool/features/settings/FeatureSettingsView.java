package mindustrytool.features.settings;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.color;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.paddingBottom;
import static solim.modifier.Values.paddingLeft;
import static solim.modifier.Values.paddingTop;
import static solim.modifier.Values.placeholder;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.scrollX;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.visible;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.Set;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.Config;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.web.WebFeature;
import mindustrytool.features.web.WebFeatureCard;
import solim.core.Units;
import solim.core.Component;
import solim.reactive.Computed;
import solim.reactive.Signal;

public final class FeatureSettingsView extends Component {
    private final Signal<String> filter = Signal.of("");

    @Override
    protected Element build() {

        Computed<Float> contentWidth = Units.dvw.map(w -> w * 0.9f - unit(10));
        Computed<Integer> columnCount = contentWidth.map(w -> Math.max(1, (int) (w / 400f)));

        Computed<Seq<Feature>> filteredFeatures = Signal.computed(() -> {
            String q = filter.get();
            String query = q != null ? q.trim().toLowerCase() : "";
            return FeatureManager.features().get().select(f -> matchesFilter(f, query));
        });

        Computed<Seq<Feature>> favoritedFeatures = Signal.computed(() -> {
            Seq<Feature> all = filteredFeatures.get();
            Set<String> favs = ModSettings.favoriteFeatures.signal().get();
            if (all == null || favs == null || favs.isEmpty()) {
                return new Seq<>();
            }
            return all.select(f -> favs.contains(f.getMetadata().getId()));
        });

        Computed<Seq<Feature>> standardFeatures = Signal.computed(() -> {
            Seq<Feature> all = filteredFeatures.get();
            Set<String> favs = ModSettings.favoriteFeatures.signal().get();
            if (all == null) {
                return new Seq<>();
            }
            if (favs == null || favs.isEmpty()) {
                return all;
            }
            return all.select(f -> !favs.contains(f.getMetadata().getId()));
        });

        Computed<Boolean> reorderAllowed = filter.map(q -> q == null || q.trim().isEmpty());

        Computed<Seq<WebFeature>> filteredWebFeatures = filter.map(
                q -> WebFeature.defaults.select(w -> matchesWebFilter(w, q != null ? q.trim().toLowerCase() : "")));

        Computed<Boolean> hasFavorites = favoritedFeatures.map(seq -> seq != null && !seq.isEmpty());
        Computed<Boolean> hasStandardFeatures = standardFeatures.map(seq -> seq != null && !seq.isEmpty());
        Computed<Boolean> hasFeatures = Signal.computed(
                () -> Boolean.TRUE.equals(hasFavorites.get()) || Boolean.TRUE.equals(hasStandardFeatures.get()));
        Computed<Boolean> hasWebFeatures = filteredWebFeatures.map(seq -> seq != null && !seq.isEmpty());

        Computed<Boolean> isEmpty = Signal.computed(() -> {
            Boolean hf = hasFeatures.get();
            Seq<WebFeature> w = filteredWebFeatures.get();
            return !Boolean.TRUE.equals(hf) && (w == null || w.isEmpty());
        });

        return column().props(grow(), gap(unit(2))).children(() -> {
            toolbar();
            scroll().props(grow(), scrollX(false)).children(() -> {
                column().props(growX(), gap(unit(3))).children(() -> {

                    column().props(growX(), visible(hasFavorites), gap(unit(2))).children(() -> {
                        row().props(growX(), paddingTop(unit(1)), paddingBottom(unit(1))).children(() -> {
                            text(Core.bundle.get("feature.section.favorites", "Favorites"))
                                    .props(style(Styles.defaultLabel), left());
                        });
                        reactiveGrid(favoritedFeatures).columns(columnCount)
                                .key(feature -> feature.getMetadata().getId())
                                .props(gap(unit(2)))
                                .items(feature -> new FeatureCard(feature, reorderAllowed));
                    });

                    divider().props(visible(Signal.computed(() -> Boolean.TRUE.equals(hasFavorites.get())
                            && Boolean.TRUE.equals(hasStandardFeatures.get()))));

                    column().props(growX(), visible(hasStandardFeatures), gap(unit(2))).children(() -> {
                        row().props(growX(), paddingTop(unit(1)), paddingBottom(unit(1))).children(() -> {
                            text(Core.bundle.get("feature.section.mod-features", "Mod Features"))
                                    .props(style(Styles.defaultLabel), left());
                        });
                        reactiveGrid(standardFeatures).columns(columnCount)
                                .key(feature -> feature.getMetadata().getId())
                                .props(gap(unit(2)))
                                .items(feature -> new FeatureCard(feature, reorderAllowed));
                    });

                    divider().props(visible(Signal.computed(
                            () -> Boolean.TRUE.equals(hasFeatures.get()) && Boolean.TRUE.equals(hasWebFeatures.get()))));

                    column().props(growX(), visible(hasWebFeatures), gap(unit(2))).children(() -> {
                        row().props(growX(), paddingTop(unit(3)), paddingBottom(unit(1))).children(() -> {
                            text(Core.bundle.get("web-feature.section.title", "Web Tools & Community"))
                                    .props(style(Styles.defaultLabel), left());
                        });
                        reactiveGrid(filteredWebFeatures).columns(columnCount).key(WebFeature::getId)
                                .props(gap(unit(2)))
                                .items(WebFeatureCard::new);
                    });

                    row().props(growX(), visible(isEmpty), center()).children(() -> {
                        text(Core.bundle.get("feature.search.empty", "No features found"))
                                .props(color(Color.gray), margin(unit(4)));
                    });
                });
            });
        }).element();
    }

    private void toolbar() {
        column().props(growX(), gap(unit(2))).children(() -> {
            row().props(growX(), gap(unit(2)), rounded(unit(3)), paddingLeft(unit(2)), height(unit(12f)), prefHeight(unit(12f)), center(), border(1.5f, Color.darkGray)).children(() -> {
                        icon(Icon.zoom).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        textField(filter).props(growX(), style(WebStyles.clearInput()), placeholder(Core.bundle.get("feature.search.placeholder")));
                    });

            wrap().props(growX(), top(), left(), gap(unit(2))).children(() -> {
                button(FeatureManager::reenable).props(style(WebStyles.secondary()), height(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.re-enable.tooltip")), gap(unit(2))).children(() -> {
                            icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                            text(Core.bundle.get("feature.button.re-enable"));
                        });

                button(() -> Core.app.openURI(Config.PROJECT_SUGGESTION_URL))
                        .props(style(WebStyles.secondary()), height(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.create-suggestion.tooltip")), gap(unit(2)))
                        .children(() -> {
                            icon(Icon.link).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                            text(Core.bundle.get("feature.button.create-suggestion"));
                        });

                button(() -> new GeneralSettingsDialog().show()).props(style(WebStyles.primary()), height(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.button.settings")), gap(unit(2))).children(() -> {
                            icon(Icon.settings).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                            text(Core.bundle.get("feature.button.settings"));
                        });
            });

            divider();
        });
    }

    public static boolean matchesFilter(Feature feature, String query) {
        return query == null || query.trim().isEmpty()
                || feature.getName().toLowerCase().contains(query.trim().toLowerCase());
    }

    public static boolean matchesWebFilter(WebFeature webFeature, String query) {
        return query == null || query.trim().isEmpty()
                || webFeature.getName().toLowerCase().contains(query.trim().toLowerCase());
    }
}