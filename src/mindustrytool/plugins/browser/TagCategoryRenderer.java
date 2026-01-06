package mindustrytool.plugins.browser;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.ui.Styles;
import mindustry.gen.Icon;
import arc.scene.ui.layout.Collapser;
import arc.util.Align;

public class TagCategoryRenderer {
    public static void render(Table table, SearchConfig searchConfig, Seq<TagCategory> categories, FilterConfig config,
            Seq<String> modIds, String searchQuery, arc.struct.ObjectMap<String, Boolean> collapseState) {
        if (categories == null)
            return;

        Seq<String> selectedPlanets = searchConfig.getSelectedPlanetTags();
        boolean hasPlanetSelection = !selectedPlanets.isEmpty();

        for (TagCategory category : categories.sort((a, b) -> a.position() - b.position())) {
            if (category.tags() == null || category.tags().isEmpty())
                continue;

            // Filter tags based on search query, planet selection, and MOD selection
            Seq<TagData> filteredTags = category.tags().select(tag -> {
                // 1. Search filter
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    if (!tag.name().toLowerCase().contains(searchQuery.toLowerCase()))
                        return false;
                }

                // 2. Mod Filter
                // Identify if this is a mod tag
                String tagModId = null;

                // Check fullTag for "modId:tagName" format
                if (tag.fullTag() != null && tag.fullTag().contains(":")) {
                    tagModId = tag.fullTag().split(":")[0];
                }
                // Fallback: Check planetIds if fullTag didn't yield a result (or maybe
                // planetIds IS the modId)
                else if (tag.planetIds() != null && !tag.planetIds().isEmpty()) {
                    // This is risky if planetIds contains "serpulo" and "serpulo" is not a mod.
                    // But usually mod tags have the mod ID here.
                }

                // If we identified a mod ID, check if it's selected
                if (tagModId != null) {
                    // If it's "global" or "vanilla", treat as generic?
                    if (!tagModId.equals("global") && !tagModId.equals("vanilla")) {
                        if (!modIds.contains(tagModId))
                            return false; // Hide if mod not selected
                    }
                } else {
                    // If no mod ID found from fullTag, check planetIds for strict non-generic
                    // filtering
                }

                // 3. Planet Filter (Legacy/Existing Logic)
                boolean isGeneric = tag.planetIds() == null || tag.planetIds().isEmpty();

                if (hasPlanetSelection) {
                    if (isGeneric)
                        return true;
                    return tag.planetIds().contains(p -> selectedPlanets.contains(p));
                } else {
                    // Default behavior: show generic tags.
                    // If the category IS "Planet", we should show all planets.
                    if (category.name().equalsIgnoreCase("Planet"))
                        return true;

                    // If we have Mod Selection, we might want to show mod tags even if they have
                    // planetIds?
                    // If modIds is NOT empty, and this tag matches a selected mod (via planetIds),
                    // show it.
                    if (!modIds.isEmpty() && !isGeneric) {
                        if (tag.planetIds().contains(id -> modIds.contains(id)))
                            return true;
                    }

                    return isGeneric;
                }
            });

            if (filteredTags.isEmpty())
                continue;

            table.row();
            renderCategory(table, searchConfig, category, config, modIds, filteredTags, collapseState);
        }
    }

    private static void renderCategory(Table table, SearchConfig searchConfig, TagCategory category,
            FilterConfig config, Seq<String> modIds, Seq<TagData> tags,
            arc.struct.ObjectMap<String, Boolean> collapseState) {
        Table content = new Table();
        // Default to expanded (false means NOT collapsed)
        boolean isCollapsed;
        if (collapseState.containsKey(category.id())) {
            isCollapsed = collapseState.get(category.id());
        } else {
            isCollapsed = arc.Core.settings.getBool("filter.collapse." + category.id(), false);
            collapseState.put(category.id(), isCollapsed);
        }

        Collapser collapser = new Collapser(content, true);
        collapser.setDuration(0); // Disable animation permanently
        collapser.setCollapsed(isCollapsed);

        table.button(b -> {
            b.label(() -> category.name()).fontScale(config.scale).left().growX();
            b.image(Icon.downOpen).update(i -> i.setRotation(collapser.isCollapsed() ? 90 : 270)).right();
        }, () -> {
            boolean newState = !collapser.isCollapsed();
            collapser.setCollapsed(newState);
            collapseState.put(category.id(), newState);
        }).get().setStyle(Styles.cleart);

        // Re-apply growX and padBottom after getting the button
        table.getCell(table.getChildren().peek()).growX().padBottom(4);

        table.row();
        table.add(collapser).growX();

        // Animation removed as requested

        content.left().top(); // Align content top-left

        // Use exact width calculation based on container padding (10px left + 10px
        // right + scrollbar)
        // Core.graphics.getWidth() - 60 should be safe and fill the row.
        float availableWidth = arc.Core.graphics.getWidth() - 60f;
        float currentWidth = 0;

        // Current row table
        Table[] currentRow = { new Table() };
        currentRow[0].left().defaults().pad(1);
        content.add(currentRow[0]).left().top().row();

        for (TagData value : tags.sort((a, b) -> a.position() - b.position())) {
            if (value == null)
                continue;

            // Mod filter check
            Seq<String> planetIds = value.planetIds();
            boolean isGeneric = planetIds == null || planetIds.size == 0;

            if (!modIds.isEmpty() && !isGeneric) {
                boolean matchesMod = planetIds.contains(id -> modIds.contains(id));
                if (!matchesMod)
                    continue;
            }

            // Estimate button width
            String name = value.name();
            if (value.count() != null)
                name += " (" + value.count() + ")";

            // Conservative estimation
            float iconWidth = (value.icon() != null && !value.icon().isEmpty()) ? 26 * config.scale : 0;
            float textWidth = name.length() * 10 * config.scale;
            float estimatedWidth = textWidth + iconWidth + 40 * config.scale; // Increased buffer for wider padding

            if (currentWidth + estimatedWidth > availableWidth) {
                // New row
                currentRow[0] = new Table();
                currentRow[0].left().defaults().pad(1);
                content.add(currentRow[0]).left().top().row();
                currentWidth = 0;
            }

            currentRow[0].button(btn -> {
                btn.left();
                if (value.icon() != null && !value.icon().isEmpty()) {
                    btn.add(new NetworkImage(value.icon())).size(24 * config.scale).padRight(4).align(Align.center);
                }
                String btnName = value.name();
                if (value.count() != null) {
                    btnName += " (" + value.count() + ")";
                }
                btn.add(btnName).fontScale(config.scale).align(Align.center);
                btn.margin(4f).marginLeft(8f).marginRight(8f); // Wider horizontal padding

                // Reactive state binding: Button state always reflects SearchConfig
                btn.update(() -> {
                    boolean isSelected = searchConfig.containTag(category, value);
                    btn.setChecked(isSelected);
                    btn.setColor(isSelected ? arc.graphics.Color.valueOf("9d57ff") : arc.graphics.Color.white);
                });
            }, () -> searchConfig.setTag(category, value))
                    .get().setStyle(Styles.flatBordert);

            // Re-apply checked state (initial) - though update() covers it, this helps
            // initial layout?
            // Actually update() runs before draw, so it's fine.
            arc.scene.ui.Button btn = (arc.scene.ui.Button) currentRow[0].getChildren().peek();

            // Flexible height, increased padding
            currentRow[0].getCell(btn).height(36 * config.scale).pad(4);

            currentWidth += estimatedWidth;
        }
    }
}
