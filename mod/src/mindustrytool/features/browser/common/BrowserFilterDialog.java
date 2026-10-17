package mindustrytool.features.browser.common;

import static solim.UI.*;
import static solim.modifier.Values.backgroundColor;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.paddingLeft;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.world.Block;
import solim.graphics.ColoredDrawable;
import mindustrytool.Config;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ModData;
import mindustrytool.models.response.Sort;
import mindustrytool.models.response.TagCategory;
import mindustrytool.models.response.TagData;
import mindustrytool.services.MindustryTool;
import solim.modifier.Values;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Query;
import solim.reactive.QueryKey;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class BrowserFilterDialog extends SolimDialog {

    public BrowserFilterDialog(BrowserState<?> state, String tagGroup, boolean useBlocks, boolean usePlanets) {
        super(Core.bundle.get("browser.filter.title"));

        addCloseButton();
        closeOnBack();
        fillParent(true);
        children(() -> new FilterContent(state, tagGroup, useBlocks, usePlanets));
        dialog().background(ColoredDrawable.of(Color.black, Tex.whiteui));
    }

    private class FilterContent extends Component {
        private final BrowserState<?> state;
        private final boolean useBlocks;
        private final boolean usePlanets;
        private final Query<List<TagCategory>> tagsQuery;
        private final @Nullable Query<List<ModData>> planetsQuery;
        private final Signal<Seq<String>> selectedPlanets = Signal.of(new Seq<String>());
        private final Signal<String> filterText = Signal.of("");
        private final Computed<List<CategoryViewModel>> visibleCategories = new Computed<>(
                this::computeVisibleCategories);

        FilterContent(BrowserState<?> state, String tagGroup, boolean useBlocks, boolean usePlanets) {
            this.state = state;
            this.useBlocks = useBlocks;
            this.usePlanets = usePlanets;
            this.tagsQuery = Query.<List<TagCategory>>builder()
                    .key(QueryKey.of("tags", tagGroup))
                    .fetch(() -> MindustryTool.getTags(tagGroup))
                    .staleTime(Duration.ofMinutes(10))
                    .build();
            this.planetsQuery = usePlanets
                    ? Query.<List<ModData>>builder()
                            .key(QueryKey.of("planets"))
                            .fetch(MindustryTool::getPlanets)
                            .staleTime(Duration.ofMinutes(10))
                            .build()
                    : null;
        }

        @Override
        protected Element build() {
            return column().props(grow(), padding(unit(4)), gap(unit(4))).children(() -> {
                searchRow();
                scroll().props(grow()).children(() -> {
                    column().props(growX(), left(), gap(unit(4))).children(() -> {
                        sectionPanel(Core.bundle.get("browser.filter.sort"), () -> renderSortOptions());

                        sectionPanel(Core.bundle.get("browser.filter.verification"), () -> renderVerificationOptions());

                        if (usePlanets) {
                            sectionPanel(Core.bundle.get("browser.filter.planets"), () -> renderPlanets());
                        }

                        renderTagCategories();

                        if (useBlocks) {
                            sectionPanel(Core.bundle.get("browser.filter.blocks"), () -> renderBlocks());
                        }
                    });
                });
            }).element();
        }

        private void searchRow() {
            row().props(growX(), left(), gap(unit(1)), height(unit(11)), prefHeight(unit(11)), Values.rounded(unit(3)), Values.border(1.5f, Color.darkGray), paddingLeft(unit(2)), center())
                    .children(() -> {
                        icon(Icon.zoom).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))).color(Color.gray);

                        textField(filterText)
                                .props(growX())
                                .style(WebStyles.clearInput())
                                .placeholder(Core.bundle.get("browser.search.placeholder"));
                        divider(Direction.VERTICAL);
                        clearAllButton();
                    });
        }

        private void clearAllButton() {
            button(Core.bundle.get("browser.filter.clear-all"), this::clearAll)
                    .style(WebStyles.clearFiltersText())
                    .props(height(unit(10)), prefHeight(unit(10)));
        }

        private void sectionPanel(String title, Runnable content) {
            sectionPanel(title, content, Color.white);
        }

        private void sectionPanel(String title, Runnable content, Color color) {
            column().props(growX(), left(), Values.rounded(unit(2)), backgroundColor(Color.rgb(24, 24, 24)), padding(unit(3)), gap(unit(2)))
                    .children(() -> {
                        text(title).style(Styles.defaultLabel).color(color).left();
                        content.run();
                    });
        }

        private void renderSortOptions() {
            wrap().props(gap(unit(1)), left()).children(() -> {
                for (Sort sortOption : Config.sorts) {
                    String sortValue = sortOption.getValue();
                    Readable<Boolean> checked = state.sort().map(current -> sortValue.equals(current));
                    button(() -> state.setSort(sortValue))
                            .style(WebStyles.filterChipText())
                            .props(paddingX(unit(3)))
                            .checked(checked)
                            .props(height(unit(9)), prefHeight(unit(9)))
                            .children(() -> {
                                text(sortOption.getName()).color(checked.map(c -> c ? Color.white : Color.gray));
                            });
                }
            });
        }

        private void renderVerificationOptions() {
            String[] options = { "ALL", "PENDING", "VERIFIED", "REJECTED" };
            String[] keys = {
                    "browser.filter.verification.all",
                    "browser.filter.verification.pending",
                    "browser.filter.verification.verified",
                    "browser.filter.verification.rejected"
            };
            wrap().props(gap(unit(1)), left()).children(() -> {
                for (int i = 0; i < options.length; i++) {
                    String optionValue = options[i];
                    String label = Core.bundle.get(keys[i]);
                    Readable<Boolean> checked = state.verification().map(current -> optionValue.equals(current));
                    button(() -> state.setVerification(optionValue))
                            .style(WebStyles.filterChipText())
                            .props(paddingX(unit(3)))
                            .checked(checked)
                            .props(height(unit(9)), prefHeight(unit(9)))
                            .children(() -> {
                                text(label).color(checked.map(c -> c ? Color.white : Color.gray));
                            });
                }
            });
        }

        private void renderPlanets() {
            if (planetsQuery == null) {
                return;
            }

            query(planetsQuery)
                    .props(grow())
                    .loading(() -> row().props(growX(), padding(unit(2)), center()).children(() -> new Loader(unit(6))))
                    .data(mods -> {
                        if (mods == null || mods.isEmpty()) {
                            return row();
                        }

                        List<ModData> sorted = new ArrayList<ModData>(mods);
                        Collections.sort(sorted, new Comparator<ModData>() {
                            @Override
                            public int compare(ModData a, ModData b) {
                                int pa = a.getPosition() != null ? a.getPosition() : 0;
                                int pb = b.getPosition() != null ? b.getPosition() : 0;
                                return pa - pb;
                            }
                        });
                        return wrap().props(left(), gap(unit(1))).children(() -> {
                            for (ModData mod : sorted) {
                                renderPlanet(mod);
                            }
                        });
                    });
        }

        private void renderPlanet(ModData mod) {
            String modId = mod.getId();
            String chipLabel = mod.getName() != null ? mod.getName() : modId;
            Readable<Boolean> checked = selectedPlanets.map(
                    selected -> selected != null && modId != null && selected.contains(modId));
            button(() -> togglePlanet(modId))
                    .style(WebStyles.filterChipText())
                    .props(paddingX(unit(3)))
                    .checked(checked)
                    .props(height(unit(9)), prefHeight(unit(9)))
                    .children(() -> {
                        text(chipLabel).color(checked.map(c -> c ? Color.white : Color.gray));
                    });
        }

        private void togglePlanet(String modId) {
            if (modId == null) {
                return;
            }
            selectedPlanets.update(current -> {
                Seq<String> copy = current != null ? new Seq<String>(current) : new Seq<String>();
                if (copy.contains(modId)) {
                    copy.remove(modId);
                } else {
                    copy.add(modId);
                }
                return copy;
            });
        }

        private class CategoryViewModel {
            final String name;
            final Color color;
            final List<TagData> tags;

            CategoryViewModel(String name, Color color, List<TagData> tags) {
                this.name = name;
                this.color = color;
                this.tags = tags;
            }
        }

        private List<CategoryViewModel> computeVisibleCategories() {
            List<TagCategory> categories = tagsQuery.data().get();
            Seq<String> planetFilter = selectedPlanets.get();
            String query = filterText.get();
            final String loweredQuery = query != null ? query.toLowerCase().trim() : "";

            if (categories == null || categories.isEmpty()) {
                return Collections.<CategoryViewModel>emptyList();
            }

            List<TagCategory> sorted = new ArrayList<TagCategory>(categories);
            Collections.sort(sorted, new Comparator<TagCategory>() {
                @Override
                public int compare(TagCategory a, TagCategory b) {
                    return a.getPosition() - b.getPosition();
                }
            });

            List<CategoryViewModel> result = new ArrayList<CategoryViewModel>();
            for (TagCategory category : sorted) {
                if (category.getTags() == null) {
                    continue;
                }

                List<TagData> visible = visibleTags(category.getTags(), planetFilter, loweredQuery);

                if (!visible.isEmpty()) {
                    result.add(new CategoryViewModel(
                            category.getName() != null ? category.getName() : "",
                            category.color(),
                            visible));
                }
            }
            return result;
        }

        private void renderTagCategories() {
            query(tagsQuery)
                    .props(growX())
                    .loading(() -> row().props(growX(), padding(unit(4)), center()).children(() -> new Loader(unit(6))))
                    .data(tags -> dynamic(visibleCategories, categories -> {
                        column().props(growX(), gap(unit(4))).children(() -> {
                            if (categories == null || categories.isEmpty()) {
                                text(Core.bundle.get("browser.empty")).color(Color.gray).left();
                                return;
                            }

                            for (CategoryViewModel category : categories) {
                                renderCategory(category);
                            }
                        });
                    }).props(growX()));
        }

        private void renderCategory(CategoryViewModel category) {
            sectionPanel(Strings.capitalize(category.name), () -> {
                wrap().props(growX(), left(), gap(unit(1))).children(() -> {
                    for (TagData tag : category.tags) {
                        renderTag(tag);
                    }
                });
            }, category.color);
        }

        private List<TagData> visibleTags(List<TagData> tags, Seq<String> planetFilter, String loweredQuery) {
            List<TagData> visible = new ArrayList<TagData>();
            for (TagData tag : tags) {
                if (tag == null || tag.getName() == null) {
                    continue;
                }
                if (loweredQuery != null && !loweredQuery.isEmpty()
                        && !tag.getName().toLowerCase().contains(loweredQuery)) {
                    continue;
                }
                if (usePlanets && tag.getPlanetIds() != null && !tag.getPlanetIds().isEmpty()
                        && !matchesPlanets(tag.getPlanetIds(), planetFilter)) {
                    continue;
                }
                visible.add(tag);
            }
            Collections.sort(visible, new Comparator<TagData>() {
                @Override
                public int compare(TagData a, TagData b) {
                    int pa = a.getPosition() != null ? a.getPosition() : 0;
                    int pb = b.getPosition() != null ? b.getPosition() : 0;
                    return pa - pb;
                }
            });
            return visible;
        }

        private void renderTag(TagData tag) {
            String key = tagKey(tag);
            String chipLabel = tag.getName() != null ? tag.getName() : "";
            Readable<Boolean> checked = state.selectedTags().map(
                    selected -> selected != null && selected.contains(key));

            button(() -> state.toggleTag(key))
                    .style(WebStyles.filterChipText())
                    .props(paddingX(unit(3)), height(unit(9)), prefHeight(unit(9)), gap(unit(1)))
                    .checked(checked)
                    .children(() -> {
                        if (tag.getIcon() != null) {
                            networkImage(tag.getIcon()).fallback(Icon.imageSmall).size(unit(6));
                        }
                        text(chipLabel).color(checked.map(c -> c ? Color.white : Color.gray));
                    });
        }

        private void renderBlocks() {
            dynamic(filterText, query -> {
                final String loweredQuery = query != null ? query.toLowerCase() : "";
                Seq<Block> blocks = availableBlocks();

                if (blocks.isEmpty()) {
                    text(Core.bundle.get("browser.filter.blocks.empty")).color(Color.gray).left();
                } else {
                    wrap().props(left(), gap(unit(1))).children(() -> {
                        for (int i = 0; i < blocks.size; i++) {
                            Block block = blocks.get(i);
                            if (block == null || block.localizedName == null) {
                                continue;
                            }

                            if (!loweredQuery.isEmpty()
                                    && !block.localizedName.toLowerCase().contains(loweredQuery)) {
                                continue;
                            }

                            String blockName = block.name;
                            String chipLabel = block.localizedName;
                            Readable<Boolean> checked = state.selectedBlocks().map(
                                    selected -> selected != null && selected.contains(blockName));

                            button(() -> state.toggleBlock(blockName))
                                    .style(WebStyles.filterChipText())
                                    .props(paddingX(unit(3)), height(unit(9)), prefHeight(unit(9)), gap(unit(1)))
                                    .checked(checked)
                                    .children(() -> {
                                        if (block.uiIcon != null) {
                                            icon(new TextureRegionDrawable(block.uiIcon)).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                        }
                                        text(chipLabel).color(checked.map(c -> c ? Color.white : Color.gray));
                                    });
                        }
                    });
                }
            });
        }

        private boolean matchesPlanets(List<String> planetIds, Seq<String> planetFilter) {
            if (planetFilter == null || planetFilter.size == 0) {
                return false;
            }
            for (String planetId : planetIds) {
                if (planetId != null && planetFilter.contains(planetId)) {
                    return true;
                }
            }
            return false;
        }

        private void clearAll() {
            state.clearTags();
            state.clearBlocks();
            state.setSort(Config.sorts.get(0).getValue());
            state.setVerification("ALL");
            selectedPlanets.set(new Seq<String>());
            filterText.set("");
        }
    }

    static String tagKey(TagData tag) {
        if (tag.getFullTag() != null && !tag.getFullTag().isEmpty()) {
            return tag.getFullTag();
        }
        return tag.getName() != null ? tag.getName() : "";
    }

    private static Seq<Block> availableBlocks() {
        try {
            return Vars.content.blocks().select(block -> block.isVisible());
        } catch (Exception ignored) {
            return new Seq<Block>();
        }
    }
}