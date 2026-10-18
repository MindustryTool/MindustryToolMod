package mindustrytool.features.maprules;

import static solim.UI.badge;
import static solim.UI.button;
import static solim.UI.card;
import static solim.UI.checkbox;
import static solim.UI.column;
import static solim.UI.divider;
import static solim.UI.dvh;
import static solim.UI.dynamic;
import static solim.UI.effect;
import static solim.UI.icon;
import static solim.UI.row;
import static solim.UI.scroll;
import static solim.UI.slider;
import static solim.UI.spacer;
import static solim.UI.text;
import static solim.UI.textField;
import static solim.UI.unit;
import static solim.UI.wrap;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.enabled;
import static solim.modifier.Values.style;
import static solim.modifier.Values.border;
import static solim.modifier.Values.center;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.left;
import static solim.modifier.Values.height;
import static solim.modifier.Values.minHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.func.Cons;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.game.Rules;
import mindustry.gen.Icon;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class MapRulesView extends Component {

    private static final String CAT_ALL = RuleDescriptor.CAT_ALL;
    private static final String CAT_MODIFIED = RuleDescriptor.CAT_MODIFIED;
    private static final String CAT_MULTIPLIERS = RuleDescriptor.CAT_MULTIPLIERS;
    private static final String CAT_WAVES = RuleDescriptor.CAT_WAVES;
    private static final String CAT_BANS = RuleDescriptor.CAT_BANS;
    private static final String CAT_ENVIRONMENT = RuleDescriptor.CAT_ENVIRONMENT;
    private static final String CAT_MECHANICS = RuleDescriptor.CAT_MECHANICS;

    private final MapRulesFeature feature;
    private final Signal<String> search = Signal.of("");
    private final Signal<String> category = Signal.of(CAT_ALL);
    private final Signal<RuleEditMode> mode = Signal.of(RuleEditMode.SINGLE_PLAYER);
    private final Signal<Integer> refreshTick = Signal.of(0);
    private final Signal<String> lastSavedPath = Signal.of("");
    private @Nullable Runnable saveHandler;

    public MapRulesView(MapRulesFeature feature) {
        this.feature = feature;
        mode.set(RuleEditMode.resolve());
        listen(WorldLoadEndEvent.class, e -> {
            mode.set(RuleEditMode.resolve());
            refreshTick.set(refreshTick.peek() != null ? refreshTick.peek() + 1 : 1);
        });
        listen(ResetEvent.class, e -> {
            mode.set(RuleEditMode.resolve());
            refreshTick.set(refreshTick.peek() != null ? refreshTick.peek() + 1 : 1);
        });
        effect(() -> {
            refreshTick.get();
            mode.set(RuleEditMode.resolve());
        });
    }

    public void refresh() {
        mode.set(RuleEditMode.resolve());
        Integer tick = refreshTick.peek();
        refreshTick.set(tick != null ? tick + 1 : 1);
    }

    public void setLastSaved(@Nullable String path) {
        lastSavedPath.set(path != null ? path : "");
        refresh();
    }

    private String t(String key) {
        return Core.bundle.has(key) ? Core.bundle.get(key) : key;
    }

    private String f(String key, Object arg) {
        if (Core.bundle.has(key)) {
            return Core.bundle.format(key, arg);
        }
        return key + ": " + arg;
    }

    private @Nullable String customMapsFolder() {
        try {
            if (Vars.customMapDirectory != null) {
                return Vars.customMapDirectory.absolutePath();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Readable<String> modeBadgeText() {
        return new Computed<>(() -> {
            RuleEditMode m = mode.get();
            refreshTick.get();
            if (m == null) {
                m = RuleEditMode.resolve();
            }
            if (m == RuleEditMode.HOST) {
                return t("feature.map-rules.mode.host");
            }
            if (m == RuleEditMode.CLIENT_ADMIN) {
                return t("feature.map-rules.mode.client-admin");
            }
            if (m == RuleEditMode.CLIENT_READONLY) {
                return t("feature.map-rules.mode.client-readonly");
            }
            return t("feature.map-rules.mode.single-player");
        });
    }

    private @Nullable Rules rules() {
        return Vars.state != null ? Vars.state.rules : null;
    }

    private boolean editableGameplay() {
        RuleEditMode m = mode.peek();
        if (m == null) {
            m = RuleEditMode.resolve();
        }
        return m != null && m.canEditGameplay();
    }

    private boolean visualEditable(String field) {
        if (editableGameplay()) {
            return true;
        }
        RuleEditMode m = mode.peek();
        if (m == null) {
            m = RuleEditMode.resolve();
        }
        if (m != RuleEditMode.CLIENT_READONLY) {
            return false;
        }
        return "fog".equals(field) || "lighting".equals(field);
    }

    @Override
    protected Element build() {
        Readable<String> badgeText = modeBadgeText();
        return column().props(grow(), center()).children(() -> {
            column().props(growX(), gap(unit(2))).children(() -> {
                header(badgeText);
                searchRow();
                categoryChips();
                divider();
                scroll().props(grow(), minHeight(dvh(45))).children(() -> {
                    dynamic(viewKey(), key -> {
                        column().props(growX(), gap(unit(3))).children(() -> renderSections());
                    }).props(grow());
                });
            });
        }).element();
    }

    private Readable<String> viewKey() {
        return new Computed<>(() -> {
            String q = search.get();
            String cat = category.get();
            mode.get();
            Integer tick = refreshTick.get();
            return (q != null ? q : "") + "|" + (cat != null ? cat : CAT_ALL) + "|" + (tick != null ? tick : 0);
        });
    }

    private void header(Readable<String> badgeText) {
        boolean showPermission = showPermission();
        row().props(growX(), gap(unit(2)), center()).children(() -> {
            if (showPermission) {
                badge(badgeText).props(color(Color.royal));
            }
            spacer();
            button(this::onResetClicked).props(style(WebStyles.outlineText()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(t("feature.map-rules.reset")))
                    .children(() -> icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
        });
        RuleEditMode m = mode.peek();
        if (m == RuleEditMode.CLIENT_ADMIN && showPermission) {
            text(t("feature.map-rules.hint.js-required")).props(color(Color.lightGray), left());
        }
    }

    private boolean showPermission() {
        try {
            return feature.showPermissionConfig.get();
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean showDescriptions() {
        try {
            return feature.showDescriptionsConfig.get();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void onSaveClicked() {
        if (saveHandler != null) {
            saveHandler.run();
        }
    }

    private void onResetClicked() {
        feature.resetToDefaults();
        refresh();
    }

    public void setSaveHandler(Runnable handler) {
        this.saveHandler = handler;
    }

    private void searchRow() {
        row().props(growX(), gap(unit(1)), padding(unit(1.5f)), rounded(unit(2), WebStyles.Colors.SECONDARY_BG), border(1.5f, WebStyles.Colors.BORDER_INPUT), center())
                .children(() -> {
                    icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)), color(WebStyles.Colors.GHOST_FG));
                    textField(search)
                            .props(style(WebStyles.clearInput()))
                            .props(growX(), height(unit(8)), prefHeight(unit(8)), solim.modifier.Values.placeholder(t("feature.map-rules.search.placeholder")));
                });
    }

    private void categoryChips() {
        wrap().props(left(), gap(unit(1))).children(() -> {
            chip(CAT_ALL, t("feature.map-rules.category.all"));
            chip(CAT_MODIFIED, t("feature.map-rules.category.modified"));
            chip(CAT_MULTIPLIERS, t("feature.map-rules.category.multipliers"));
            chip(CAT_WAVES, t("feature.map-rules.category.waves"));
            chip(CAT_BANS, t("feature.map-rules.category.bans"));
            chip(CAT_ENVIRONMENT, t("feature.map-rules.category.environment"));
            chip(CAT_MECHANICS, t("feature.map-rules.category.mechanics"));
        });
    }

    private void chip(String id, String label) {
        Readable<Boolean> checked = category.map(current -> id.equals(current));
        button(label, () -> category.set(id))
                .props(style(WebStyles.filterChipText()), checked(checked), height(unit(9)), prefHeight(unit(9)));
    }

    private void renderSections() {
        String cat = category.peek();
        String query = search.peek();
        Rules rules = rules();
        if (rules == null) {
            row().props(growX(), padding(unit(4)), center()).children(() -> {
                text(t("feature.map-rules.empty")).props(color(Color.gray));
            });
            return;
        }
        boolean onlyModified = CAT_MODIFIED.equals(cat);
        String q = query != null ? query.trim().toLowerCase() : "";
        boolean filtering = q != null && !q.isEmpty();
        boolean anyShown = false;

        saveSection();

        if (showCategory(cat, CAT_MULTIPLIERS)) {
            anyShown |= scalarSection("feature.map-rules.section.multipliers", CAT_MULTIPLIERS,
                    rules, onlyModified, q) || anyShown;
        }
        if (showCategory(cat, CAT_WAVES)) {
            anyShown |= scalarSection("feature.map-rules.section.waves", CAT_WAVES,
                    rules, onlyModified, q) || anyShown;
        }
        if (showCategory(cat, CAT_BANS)) {
            anyShown |= bansSection(rules, onlyModified, q, filtering) || anyShown;
        }
        if (showCategory(cat, CAT_ENVIRONMENT)) {
            anyShown |= scalarSection("feature.map-rules.section.environment", CAT_ENVIRONMENT,
                    rules, onlyModified, q) || anyShown;
        }
        if (showCategory(cat, CAT_MECHANICS)) {
            anyShown |= scalarSection("feature.map-rules.section.mechanics", CAT_MECHANICS,
                    rules, onlyModified, q) || anyShown;
        }
        if (!anyShown) {
            row().props(growX(), padding(unit(4)), center()).children(() -> {
                String msg = CAT_MODIFIED.equals(category.peek())
                        ? t("feature.map-rules.empty-defaults")
                        : t("feature.map-rules.empty");
                text(msg).props(color(Color.gray));
            });
        }
    }

    private void saveSection() {
        card().props(growX(), padding(unit(3))).children(() -> {
            column().props(growX(), gap(unit(2))).children(() -> {
                text(t("feature.map-rules.save_map")).props(color(Color.white), left());
                text(t("feature.map-rules.save_section.desc")).props(color(Color.lightGray), left(), solim.modifier.Values.wrap(true));
                button(t("feature.map-rules.save_map"), this::onSaveClicked)
                        .props(style(WebStyles.primaryText()), growX(), height(unit(11)), prefHeight(unit(11)));
                String folder = customMapsFolder();
                if (folder != null) {
                    text(f("feature.map-rules.save_folder", folder)).props(color(Color.gray), left(), solim.modifier.Values.wrap(true));
                }
                dynamic(lastSavedPath, path -> {
                    if (path != null && !path.isEmpty()) {
                        text(f("feature.map-rules.save_location", path)).props(color(Color.lightGray), left(), solim.modifier.Values.wrap(true));
                    }
                });
            });
        });
    }

    private boolean showCategory(@Nullable String active, String section) {
        return active == null || CAT_ALL.equals(active) || CAT_MODIFIED.equals(active) || active.equals(section);
    }

    private boolean matches(String nameKey, String descKey, String q) {
        if (q == null || q.isEmpty()) {
            return true;
        }
        String name = t(nameKey).toLowerCase();
        String desc = t(descKey).toLowerCase();
        return name.contains(q) || desc.contains(q) || nameKey.toLowerCase().contains(q);
    }

    private boolean scalarSection(String headerKey, String category, Rules rules, boolean onlyModified, String q) {
        Seq<RuleDescriptor> visible = RuleDescriptors.forCategory(category)
                .select(def -> def.isScalar() && shouldShow(def, rules, onlyModified, q));
        if (visible.isEmpty()) {
            return false;
        }
        column().props(growX(), gap(unit(2))).children(() -> {
            text(t(headerKey)).props(color(Color.white), left());
            for (RuleDescriptor def : visible) {
                switch (def.kind) {
                    case FLOAT:
                        sliderRow(def, rules);
                        break;
                    case INT:
                        intRow(def, rules);
                        break;
                    case BOOL:
                        boolRow(def, rules);
                        break;
                    default:
                        break;
                }
            }
        });
        return true;
    }

    private boolean shouldShow(RuleDescriptor def, Rules rules, boolean onlyModified, String q) {
        if (onlyModified && !feature.diff.isModified(def, rules)) {
            return false;
        }
        return matches(def.nameKey(), def.descKey(), q);
    }

    private void ruleCard(String descKey, Runnable content) {
        card().props(growX(), padding(unit(3))).children(() -> {
            column().props(growX(), gap(unit(2))).children(() -> {
                content.run();
                if (showDescriptions()) {
                    text(t(descKey)).props(color(Color.lightGray), left(), solim.modifier.Values.wrap(true));
                }
            });
        });
    }

    private void sliderRow(RuleDescriptor def, Rules rules) {
        boolean modified = feature.diff.isModified(def, rules);
        float current = def.floatGetter.get(rules);
        String label = t(def.nameKey()) + (modified ? " *" : "");
        boolean editable = editableGameplay();
        ruleCard(def.descKey(), () -> {
            if (editable) {
                Signal<Float> value = Signal.of(current);
                effect(() -> {
                    Float v = value.get();
                    Rules live = rules();
                    float base = live != null ? def.floatGetter.get(live) : current;
                    if (v != null && Math.abs(v - base) > 0.0001f) {
                        feature.mutator.setFloat(def, v);
                    }
                });
                row().props(growX()).children(() -> {
                    text(label).props(left());
                    spacer();
                    text(value.map(v -> v != null ? String.format("%.2f", v) : "")).props(color(Color.lightGray));
                });
                slider(value, def.min, def.max, def.step).props(growX(), minWidth(unit(50)));
            } else {
                row().props(growX()).children(() -> {
                    text(label).props(left());
                    spacer();
                    text(String.format("%.2f", current)).props(color(Color.lightGray));
                });
            }
        });
    }

    private void intRow(RuleDescriptor def, Rules rules) {
        boolean modified = feature.diff.isModified(def, rules);
        int current = def.intGetter.get(rules);
        String label = t(def.nameKey()) + (modified ? " *" : "");
        boolean editable = editableGameplay();
        ruleCard(def.descKey(), () -> {
            if (editable) {
                Signal<Integer> value = Signal.of(current);
                effect(() -> {
                    Integer v = value.get();
                    Rules live = rules();
                    int base = live != null ? def.intGetter.get(live) : current;
                    if (v != null && v != base) {
                        feature.mutator.setInt(def, v);
                    }
                });
                row().props(growX()).children(() -> {
                    text(label).props(left());
                    spacer();
                    text(value.map(v -> v != null ? String.valueOf(v) : "")).props(color(Color.lightGray));
                });
                slider(value, (int) def.min, (int) def.max, (int) def.step).props(growX(), minWidth(unit(50)));
            } else {
                row().props(growX()).children(() -> {
                    text(label).props(left());
                    spacer();
                    text(String.valueOf(current)).props(color(Color.lightGray));
                });
            }
        });
    }

    private boolean bansSection(Rules rules, boolean onlyModified, String q, boolean filtering) {
        boolean modified = false;
        for (RuleDescriptor def : RuleDescriptors.forCategory(RuleDescriptor.CAT_BANS)) {
            if (feature.diff.isModified(def, rules)) {
                modified = true;
                break;
            }
        }
        if (onlyModified && !modified) {
            return false;
        }
        if (filtering && !matches("feature.map-rules.section.bans", "feature.map-rules.bans.blocks", q)
                && !matches("feature.map-rules.bans.blocks", "feature.map-rules.bans.blocks", q)
                && !matches("feature.map-rules.bans.units", "feature.map-rules.bans.units", q)) {
            boolean anyContentMatch = anyBannedContentMatches(q);
            if (!anyContentMatch) {
                return false;
            }
        }
        column().props(growX(), gap(unit(2))).children(() -> {
            text(t("feature.map-rules.section.bans")).props(color(Color.white), left());
            text(t("feature.map-rules.bans.blocks")).props(color(Color.lightGray), left());
            banGrid("feature.map-rules.bans.none-blocks", Vars.content.blocks(), rules.bannedBlocks,
                    block -> toggleBan(block, r -> r.bannedBlocks, "bannedBlocks", "block"),
                    block -> block.uiIcon, q);
            text(t("feature.map-rules.bans.units")).props(color(Color.lightGray), left());
            banGrid("feature.map-rules.bans.none-units", Vars.content.units(), rules.bannedUnits,
                    type -> toggleBan(type, r -> r.bannedUnits, "bannedUnits", "unit"), null, q);
        });
        return true;
    }

    private boolean anyBannedContentMatches(String q) {
        if (q == null || q.isEmpty() || Vars.content == null) {
            return false;
        }
        try {
            for (Block block : Vars.content.blocks()) {
                if (block != null && block.localizedName != null
                        && block.localizedName.toLowerCase().contains(q)) {
                    return true;
                }
            }
            for (UnitType type : Vars.content.units()) {
                if (type != null && type.localizedName != null
                        && type.localizedName.toLowerCase().contains(q)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private <T extends UnlockableContent> void banGrid(String noneKey, Seq<T> all, ObjectSet<T> banned,
            Cons<T> onToggle, @Nullable Func<T, TextureRegion> iconOf, String q) {
        Seq<T> toShow = new Seq<>();
        try {
            if (q != null && !q.isEmpty()) {
                for (T item : all) {
                    if (item == null) {
                        continue;
                    }
                    if (toShow.size >= 60) {
                        break;
                    }
                    String name = item.localizedName != null ? item.localizedName.toLowerCase() : "";
                    if (name.contains(q) || item.name.contains(q)) {
                        toShow.add(item);
                    }
                }
            } else if (banned != null) {
                toShow.addAll(banned.toSeq());
            }
        } catch (Exception ignored) {
        }
        if (toShow.isEmpty()) {
            text(t(noneKey)).props(color(Color.gray), left());
            return;
        }
        wrap().props(left(), gap(unit(1))).children(() -> {
            for (T item : toShow) {
                banChip(item, banned != null && banned.contains(item), onToggle, iconOf);
            }
        });
    }

    private <T extends UnlockableContent> void banChip(T item, boolean banned, Cons<T> onToggle,
            @Nullable Func<T, TextureRegion> iconOf) {
        if (item == null) {
            return;
        }
        String label = item.localizedName != null ? item.localizedName : item.name;
        button(label, () -> onToggle.get(item))
                .props(style(WebStyles.filterChipText()), checked(Readable.of(banned)), enabled(Readable.of(editableGameplay())), height(unit(9)), prefHeight(unit(9)))
                .children(() -> {
                    try {
                        TextureRegion region = iconOf != null ? iconOf.get(item) : null;
                        if (region != null) {
                            icon(new TextureRegionDrawable(region)).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
                        }
                    } catch (Exception ignored) {
                    }
                });
    }

    private <T extends UnlockableContent> void toggleBan(T content, Func<Rules, ObjectSet<T>> setOf,
            String jsCollection, String jsLookup) {
        if (!editableGameplay() || content == null) {
            return;
        }
        Rules rules = rules();
        if (rules == null) {
            return;
        }
        ObjectSet<T> set = setOf.get(rules);
        if (set == null) {
            return;
        }
        RuleEditMode m = mode.peek() != null ? mode.peek() : RuleEditMode.resolve();
        if (m == RuleEditMode.CLIENT_ADMIN) {
            final boolean adding = !set.contains(content);
            feature.mutator.mutateVisual(r -> {
                ObjectSet<T> live = setOf.get(r);
                if (live == null) {
                    return;
                }
                if (adding) {
                    live.add(content);
                } else {
                    live.remove(content);
                }
            });
            feature.mutator.mutateRemote("Vars.state.rules." + jsCollection + "." + (adding ? "add" : "remove")
                    + "(Vars.content." + jsLookup + "(\"" + content.name + "\")); Call.setRules(Vars.state.rules);");
            feature.mutator.scheduleResync();
            refresh();
            return;
        }
        feature.mutator.mutate(r -> {
            ObjectSet<T> live = setOf.get(r);
            if (live == null) {
                return;
            }
            if (live.contains(content)) {
                live.remove(content);
            } else {
                live.add(content);
            }
        });
        refresh();
    }

    private void boolRow(RuleDescriptor def, Rules rules) {
        boolean modified = feature.diff.isModified(def, rules);
        boolean current = def.boolGetter.get(rules);
        String label = t(def.nameKey()) + (modified ? " *" : "");
        boolean editable = "fog".equals(def.fieldName) || "lighting".equals(def.fieldName)
                ? visualEditable(def.fieldName)
                : editableGameplay();
        ruleCard(def.descKey(), () -> {
            if (editable) {
                boolean isVisualOnly = !editableGameplay() && visualEditable(def.fieldName);
                if (isVisualOnly) {
                    checkbox(label, current, value -> feature.mutator.setVisualBool(def, value)).props(growX());
                } else {
                    checkbox(label, current, value -> {
                        feature.mutator.setBool(def, value);
                        refresh();
                    }).props(growX());
                }
            } else {
                row().props(growX()).children(() -> {
                    text(label).props(left());
                    spacer();
                    text(current ? "[green]ON" : "[scarlet]OFF").props(color(Color.lightGray));
                });
            }
        });
    }
}
