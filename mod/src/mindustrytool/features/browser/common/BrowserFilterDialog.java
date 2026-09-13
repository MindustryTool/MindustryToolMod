package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustrytool.Config;
import mindustrytool.models.response.ModData;
import mindustrytool.models.response.Sort;
import mindustrytool.models.response.TagCategory;
import mindustrytool.models.response.TagData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Filter dialog with sort selection, dynamic tag categories fetched from the
 * API, an optional block selector for schematics, and an optional planet
 * selector for maps that narrows the visible tags.
 */
public class BrowserFilterDialog extends SolimDialog {

    public BrowserFilterDialog(BrowserState<?> state, String tagGroup, boolean useBlocks, boolean usePlanets) {
        super(Core.bundle.get("browser.filter.title"));

        addCloseButton();
        closeOnBack();
        fillParent(true);
        children(() -> new FilterContent(state, tagGroup, useBlocks, usePlanets));
    }

    private static class FilterContent extends BaseComponent {
        private final BrowserState<?> state;
        private final String tagGroup;
        private final boolean useBlocks;
        private final boolean usePlanets;
        private final Signal<List<TagCategory>> tagCategories = Signal.of(Collections.<TagCategory>emptyList());
        private final Signal<List<ModData>> planets = Signal.of(Collections.<ModData>emptyList());
        private final Signal<Seq<String>> selectedPlanets = Signal.of(new Seq<String>());
        private final Signal<String> filterText = Signal.of("");
        private final Computed<List<CategoryViewModel>> visibleCategories = new Computed<>(this::computeVisibleCategories);
        private final Readable<Integer> tagColumns = isPortrait().map(p -> Boolean.TRUE.equals(p) ? 2 : 4);

        FilterContent(BrowserState<?> state, String tagGroup, boolean useBlocks, boolean usePlanets) {
            this.state = state;
            this.tagGroup = tagGroup;
            this.useBlocks = useBlocks;
            this.usePlanets = usePlanets;
            fetchTags();
            if (usePlanets) {
                fetchPlanets();
            }
        }

        @Override
        protected Element build() {
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                row().growX().gap(unit(1)).children(() -> {
                    icon(Icon.zoom).size(unit(5));
                    textField(filterText)
                            .growX()
                            .style(WebStyles.clearInput())
                            .placeholder(Core.bundle.get("browser.search.placeholder"));
                });

                scroll().grow().children(() -> {
                    column().growX().gap(unit(2)).children(() -> {
                        sectionTitle(Core.bundle.get("browser.filter.sort"));
                        renderSortOptions();

                        if (usePlanets) {
                            sectionTitle(Core.bundle.get("browser.filter.planets"));
                            renderPlanets();
                        }

                        sectionTitle(Core.bundle.get("browser.filter.tags"));
                        renderTagCategories();

                        if (useBlocks) {
                            sectionTitle(Core.bundle.get("browser.filter.blocks"));
                            renderBlocks();
                        }
                    });
                });

                button(Core.bundle.get("browser.filter.clear-all"), this::clearAll)
                        .style(Styles.defaultb)
                        .growX()
                        .height(unit(10));
            }).element();
        }

        private void sectionTitle(String title) {
            text(title).style(Styles.defaultLabel).color(Color.white).left();
        }

        private void renderSortOptions() {
            for (Sort sortOption : Config.sorts) {
                String sortValue = sortOption.getValue();
                Readable<Boolean> checked = state.sort().map(current -> sortValue.equals(current));
                button(sortOption.getName(), () -> state.setSort(sortValue))
                        .style(Styles.togglet)
                        .checked(checked)
                        .growX()
                        .height(unit(10));
            }
        }

        private void renderPlanets() {
            dynamic(planets, mods -> {
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
                return column().growX().gap(unit(1)).children(() -> {
                    for (ModData mod : sorted) {
                        renderPlanet(mod);
                    }
                });
            });
        }

        private void renderPlanet(ModData mod) {
            String modId = mod.getId();
            Readable<Boolean> checked = selectedPlanets.map(
                    selected -> selected != null && modId != null && selected.contains(modId));
            button(mod.getName() != null ? mod.getName() : modId, () -> togglePlanet(modId))
                    .style(Styles.togglet)
                    .checked(checked)
                    .growX()
                    .height(unit(10));
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

        private static class CategoryViewModel {
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
            List<TagCategory> categories = tagCategories.get();
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
            dynamic(visibleCategories, categories -> {
                return column().growX().gap(unit(2)).children(() -> {
                    if (categories == null || categories.isEmpty()) {
                        if (tagCategories.peek().isEmpty()) {
                            text(Core.bundle.get("browser.filter.tags.loading")).color(Color.gray).left();
                        } else {
                            text(Core.bundle.get("browser.empty")).color(Color.gray).left();
                        }
                        return;
                    }
                    for (CategoryViewModel category : categories) {
                        renderCategory(category);
                    }
                });
            });
        }

        private void renderCategory(CategoryViewModel category) {
            column().growX().gap(unit(1)).children(() -> {
                text(category.name)
                        .color(category.color)
                        .style(Styles.defaultLabel)
                        .left();

                grid(tagColumns).growX().gap(unit(1)).children(() -> {
                    for (TagData tag : category.tags) {
                        renderTag(tag);
                    }
                });
            });
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
            Readable<Boolean> checked = state.selectedTags().map(
                    selected -> selected != null && selected.contains(key));
            button(tag.getName(), () -> state.toggleTag(key))
                    .style(Styles.togglet)
                    .checked(checked)
                    .growX()
                    .height(unit(8));
        }

        private void renderBlocks() {
            dynamic(filterText, query -> {
                final String loweredQuery = query != null ? query.toLowerCase() : "";
                return column().growX().gap(unit(1)).children(() -> {
                    Seq<Block> blocks = availableBlocks();
                    if (blocks.isEmpty()) {
                        text(Core.bundle.get("browser.filter.blocks.empty")).color(Color.gray).left();
                        return;
                    }
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
                        Readable<Boolean> checked = state.selectedTags().map(
                                selected -> selected != null && selected.contains(blockName));
                        button(block.localizedName, () -> state.toggleTag(blockName))
                                .style(Styles.togglet)
                                .checked(checked)
                                .growX()
                                .height(unit(9));
                    }
                });
            });
        }

        private static boolean matchesPlanets(List<String> planetIds, Seq<String> planetFilter) {
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
            state.setSort(Config.sorts.get(0).getValue());
            selectedPlanets.set(new Seq<String>());
            filterText.set("");
        }

        private void fetchTags() {
            MindustryTool.getTags(tagGroup).whenComplete((result, throwable) -> {
                Core.app.post(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    tagCategories.set(result != null ? result : Collections.<TagCategory>emptyList());
                });
            });
        }

        private void fetchPlanets() {
            MindustryTool.getPlanets().whenComplete((result, throwable) -> {
                Core.app.post(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    planets.set(result != null ? result : Collections.<ModData>emptyList());
                });
            });
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
            return Vars.content.blocks().select(new Boolf<Block>() {
                @Override
                public boolean get(Block block) {
                    return block != null && block.isVisible();
                }
            });
        } catch (Exception ignored) {
            return new Seq<Block>();
        }
    }
}
