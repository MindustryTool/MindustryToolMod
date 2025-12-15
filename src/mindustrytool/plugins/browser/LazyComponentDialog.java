package mindustrytool.plugins.browser;

import arc.Core;

import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Dialog for managing lazy-loaded components.
 * Provides search, filter, and card-based UI for each component.
 */
public class LazyComponentDialog extends BaseDialog {
    private final Seq<LazyComponent<?>> components;
    private String searchText = "";
    private FilterMode filterMode = FilterMode.ALL;

    private enum FilterMode {
        ALL, LOADED, UNLOADED
    }

    public LazyComponentDialog(Seq<LazyComponent<?>> components) {
        super("@message.lazy-components.title");
        this.components = components;
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().pad(4);

        // Search bar and filter
        cont.table(header -> {
            header.field(searchText, text -> {
                searchText = text;
                rebuildList();
            }).growX().height(40).get().setMessageText(Core.bundle.get("message.lazy-components.search", "Search..."));

            header.button(getFilterLabel(), Styles.cleart, () -> {
                filterMode = FilterMode.values()[(filterMode.ordinal() + 1) % FilterMode.values().length];
                rebuildList();
            }).width(120).height(40);
        }).fillX().row();

        // Components list
        cont.pane(list -> rebuildListInto(list)).grow();
    }

    private void rebuildList() {
        cont.clear();
        rebuild();
    }

    private void rebuildListInto(Table list) {
        list.defaults().pad(6).growX();

        Seq<LazyComponent<?>> filtered = components.select(c -> {
            // Search filter
            if (!searchText.isEmpty() && !c.getName().toLowerCase().contains(searchText.toLowerCase())) {
                return false;
            }
            // Status filter
            return switch (filterMode) {
                case ALL -> true;
                case LOADED -> c.isLoaded();
                case UNLOADED -> !c.isLoaded();
            };
        });

        int columns = Core.graphics.getWidth() > 600 ? 2 : 1;
        int col = 0;

        for (LazyComponent<?> component : filtered) {
            buildCard(list, component);
            col++;
            if (col >= columns) {
                list.row();
                col = 0;
            }
        }

        if (filtered.isEmpty()) {
            list.add(Core.bundle.get("message.lazy-components.empty", "No components found")).center().pad(20);
        }
    }

    private void buildCard(Table parent, LazyComponent<?> component) {
        Table card = new Table(Styles.black5);
        card.margin(10);
        card.defaults().pad(4);

        // Header: Name + buttons
        card.table(header -> {
            header.add(component.getName()).style(Styles.defaultLabel).left().growX();

            if (component.hasSettings()) {
                header.button(Icon.settings, Styles.emptyi, 28, component::openSettings).right();
            }

            // Load/Unload button
            if (component.isLoaded()) {
                header.button(Icon.cancel, Styles.emptyi, 28, () -> {
                    component.unload();
                    rebuildList();
                }).right().tooltip(Core.bundle.get("message.lazy-components.unload", "Unload"));
            } else {
                header.button(Icon.download, Styles.emptyi, 28, () -> {
                    component.get();
                    rebuildList();
                }).right().tooltip(Core.bundle.get("message.lazy-components.load", "Load"));
            }
        }).fillX().row();

        // Description
        card.add(component.getDescription()).color(arc.graphics.Color.gray).wrap().width(200).left();

        // Status indicator
        card.row();
        String status = component.isLoaded()
                ? "[green]" + Core.bundle.get("message.lazy-components.loaded", "Loaded") + "[]"
                : "[gray]" + Core.bundle.get("message.lazy-components.not-loaded", "Not Loaded") + "[]";
        card.add(status).left().padTop(4);

        parent.add(card).width(280).pad(4);
    }

    private String getFilterLabel() {
        return switch (filterMode) {
            case ALL -> Core.bundle.get("message.lazy-components.filter.all", "Show All");
            case LOADED -> Core.bundle.get("message.lazy-components.filter.loaded", "Loaded");
            case UNLOADED -> Core.bundle.get("message.lazy-components.filter.unloaded", "Unloaded");
        };
    }
}
