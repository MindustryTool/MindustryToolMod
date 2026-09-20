package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.Set;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.web.WebFeature;
import mindustrytool.features.web.WebFeatureCard;
import solim.core.BaseComponent;
import solim.reactive.Computed;
import solim.reactive.Signal;

public final class FeatureSettingsView extends BaseComponent {
    private final Signal<String> filter = Signal.of("");
    private final Computed<Float> contentWidth = dvw(90f).map(w -> w - unit(10));
    private final Computed<Integer> columnCount = contentWidth.map(w -> Math.max(1, (int) (w / 400f)));

    private final Computed<Seq<Feature>> filteredFeatures = Signal.computed(() -> {
        String q = filter.get();
        String query = q != null ? q.trim().toLowerCase() : "";
        return FeatureManager.features().get().select(f -> matchesFilter(f, query));
    });

    private final Computed<Seq<Feature>> favoritedFeatures = Signal.computed(() -> {
        Seq<Feature> all = filteredFeatures.get();
        Set<String> favs = ModSettings.favoriteFeatures.signal().get();
        if (all == null || favs == null || favs.isEmpty()) {
            return new Seq<>();
        }
        return all.select(f -> favs.contains(f.getMetadata().getId()));
    });

    private final Computed<Seq<Feature>> standardFeatures = Signal.computed(() -> {
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

    private final Computed<Boolean> reorderAllowed = filter.map(q -> q == null || q.trim().isEmpty());

    private final Computed<Seq<WebFeature>> filteredWebFeatures = filter.map(
            q -> WebFeature.defaults.select(w -> matchesWebFilter(w, q != null ? q.trim().toLowerCase() : "")));

    private final Computed<Boolean> hasFavorites = favoritedFeatures.map(seq -> seq != null && !seq.isEmpty());
    private final Computed<Boolean> hasStandardFeatures = standardFeatures.map(seq -> seq != null && !seq.isEmpty());
    private final Computed<Boolean> hasFeatures = Signal.computed(() ->
            Boolean.TRUE.equals(hasFavorites.get()) || Boolean.TRUE.equals(hasStandardFeatures.get()));
    private final Computed<Boolean> hasWebFeatures = filteredWebFeatures.map(seq -> seq != null && !seq.isEmpty());

    private final Computed<Boolean> isEmpty = Signal.computed(() -> {
        Boolean hf = hasFeatures.get();
        Seq<WebFeature> w = filteredWebFeatures.get();
        return !Boolean.TRUE.equals(hf) && (w == null || w.isEmpty());
    });

    @Override
    protected Element build() {
        return column().grow().gap(unit(2)).children(() -> {
            toolbar();
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(3)).children(() -> {
                    // Favorites Section
                    column().growX().gap(unit(2)).visible(hasFavorites).children(() -> {
                        row().growX().paddingTop(unit(1)).paddingBottom(unit(1)).children(() -> {
                            text(Core.bundle.get("feature.section.favorites", "Favorites"))
                                    .style(Styles.defaultLabel)
                                    .left();
                        });
                        grid(columnCount,
                                favoritedFeatures,
                                feature -> feature.getMetadata().getId(),
                                feature -> new FeatureCard(feature, reorderAllowed)
                        ).gap(unit(2));
                    });

                    divider().visible(Signal.computed(() ->
                            Boolean.TRUE.equals(hasFavorites.get()) && Boolean.TRUE.equals(hasStandardFeatures.get())));

                    // Mod Features Section
                    column().growX().gap(unit(2)).visible(hasStandardFeatures).children(() -> {
                        row().growX().paddingTop(unit(1)).paddingBottom(unit(1)).children(() -> {
                            text(Core.bundle.get("feature.section.mod-features", "Mod Features"))
                                    .style(Styles.defaultLabel)
                                    .left();
                        });
                        grid(columnCount,
                                standardFeatures,
                                feature -> feature.getMetadata().getId(),
                                feature -> new FeatureCard(feature, reorderAllowed)
                        ).gap(unit(2));
                    });

                    divider().visible(Signal.computed(() ->
                            Boolean.TRUE.equals(hasFeatures.get()) && Boolean.TRUE.equals(hasWebFeatures.get())));

                    // Web Tools Section
                    column().growX().gap(unit(2)).visible(hasWebFeatures).children(() -> {
                        row().growX().paddingTop(unit(3)).paddingBottom(unit(1)).children(() -> {
                            text(Core.bundle.get("web-feature.section.title", "Web Tools & Community"))
                                    .style(Styles.defaultLabel)
                                    .left();
                        });
                        grid(columnCount,
                                filteredWebFeatures,
                                WebFeature::getId,
                                WebFeatureCard::new
                        ).gap(unit(2));
                    });

                    // Empty state when no features or web features match search
                    row().growX().center().visible(isEmpty).children(() -> {
                        text(Core.bundle.get("feature.search.empty", "No features found"))
                                .color(Color.gray)
                                .padding(unit(4));
                    });
                });
            });
        }).element();
    }

    private void toolbar() {
        column().growX().gap(unit(2)).children(() -> {
            row().growX().gap(unit(2))
                    .rounded(unit(3))
                    .paddingLeft(unit(2))
                    .height(unit(12f))
                    .center()
                    .border(1.5f, Color.darkGray).children(() -> {
                        icon(Icon.zoom).size(unit(6));
                        textField(filter).growX().style(WebStyles.clearInput())
                                .placeholder(Core.bundle.get("feature.search.placeholder"));
                    });

            row().growX().top().left().gap(unit(2)).children(() -> {
                spacer();
                button(FeatureManager::reenable).style(WebStyles.secondary()).height(unit(10))
                        .tooltip(Core.bundle.get("feature.button.re-enable.tooltip")).gap(unit(2)).children(() -> {
                            icon(Icon.refresh);
                            text(Core.bundle.get("feature.button.re-enable"));
                        });

                button(() -> new GeneralSettingsDialog().show()).style(WebStyles.primary()).height(unit(10))
                        .tooltip(Core.bundle.get("feature.button.settings")).gap(unit(2)).children(() -> {
                            icon(Icon.settings);
                            text(Core.bundle.get("feature.button.settings"));
                        });
            });

            divider();
        });
    }

    public static boolean matchesFilter(Feature feature, String query) {
        return query == null || query.trim().isEmpty() || feature.getName().toLowerCase().contains(query.trim().toLowerCase());
    }

    public static boolean matchesWebFilter(WebFeature webFeature, String query) {
        return query == null || query.trim().isEmpty() || webFeature.getName().toLowerCase().contains(query.trim().toLowerCase());
    }
}
