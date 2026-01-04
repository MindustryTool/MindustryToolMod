package mindustrytool.features.content.browser;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Fullscreen dialog for managing lazy-loaded components.
 * Responsive grid layout adapts to screen size.
 */
public class LazyComponentDialog extends BaseDialog {
    private static final float CARD_MIN_WIDTH = 280f;
    private static final float CARD_HEIGHT = 160f;

    private final Seq<LazyComponent<?>> components;
    private String searchText = "";
    private FilterMode filterMode = FilterMode.ALL;

    private enum FilterMode {
        ALL, ENABLED, DISABLED
    }

    public LazyComponentDialog(Seq<LazyComponent<?>> components) {
        super("@mdt.message.lazy-components.title");
        this.components = components;

        // Fullscreen dialog
        setFillParent(true);

        addCloseButton();
        buttons.button("Reset to defaults", Icon.refresh, () -> {
            for (LazyComponent<?> component : components) {
                component.reset();
            }
            rebuild();
        }).size(250, 64);

        shown(this::rebuild);
        onResize(this::rebuild);
    }

    private float lastWidth = -1;

    @Override
    public void act(float delta) {
        super.act(delta);
        if (lastWidth < 0)
            lastWidth = Core.graphics.getWidth();
        if (Math.abs(lastWidth - Core.graphics.getWidth()) > 1) {
            lastWidth = Core.graphics.getWidth();
            rebuild();
        }
    }

    private void rebuild() {
        cont.clear();
        cont.top().left();
        cont.defaults().pad(8);

        // Calculate optimal card width and columns
        // Adjusted padding to 70 to safely account for vertical scrollbar (approx 20px)
        // + dialog margins
        float screenWidth = (Core.graphics.getWidth() / Scl.scl(1f)) - 70;
        int columns = Math.max(1, (int) (screenWidth / CARD_MIN_WIDTH));
        float cardWidth = (screenWidth - (columns + 1) * 8) / columns;

        // Header: Search and filter
        cont.table(header -> {
            header.defaults().pad(4);
            header.field(searchText, text -> {
                searchText = text;
                rebuildList();
            }).growX().height(45).get().setMessageText(Core.bundle.get("mdt.message.lazy-components.search"));

            header.button(getFilterLabel(), Styles.squareTogglet, () -> {
                filterMode = FilterMode.values()[(filterMode.ordinal() + 1) % FilterMode.values().length];
                rebuildList();
            }).width(140).height(45);
        }).fillX().colspan(columns).row();

        // Build cards grid
        buildGrid(columns, cardWidth);
    }

    private void rebuildList() {
        cont.clear();
        rebuild();
    }

    private void buildGrid(int columns, float cardWidth) {
        Seq<LazyComponent<?>> filtered = components.select(c -> {
            if (!searchText.isEmpty() && !c.getName().toLowerCase().contains(searchText.toLowerCase())) {
                return false;
            }
            return switch (filterMode) {
                case ALL -> true;
                case ENABLED -> c.isEnabled();
                case DISABLED -> !c.isEnabled();
            };
        });

        cont.pane(pane -> {
            pane.top().left();
            pane.defaults().pad(4);

            int col = 0;
            for (LazyComponent<?> component : filtered) {
                buildCard(pane, component, cardWidth);
                col++;
                if (col >= columns) {
                    pane.row();
                    col = 0;
                }
            }

            if (filtered.isEmpty()) {
                pane.add(Core.bundle.get("mdt.message.lazy-components.empty")).color(Color.gray).pad(40);
            }
        }).grow().colspan(columns).scrollX(false); // Force no horizontal scroll
    }

    private void buildCard(Table parent, LazyComponent<?> component, float width) {
        Table card = new Table();
        card.setBackground(Styles.black5);
        card.margin(0);

        // Status bar at top (colored indicator)
        Color statusColor = component.isEnabled() ? Color.valueOf("4CAF50") : Color.valueOf("F44336");
        card.image().color(statusColor).height(4).growX().row();

        // Content area
        card.table(content -> {
            content.setColor(Color.white); // Reset parent color
            content.margin(12);
            content.top().left();
            content.defaults().pad(2).left();

            // Header row: Name + icons
            content.table(header -> {
                header.add(component.getName()).style(Styles.defaultLabel).color(Color.white).left().growX();

                // Settings button
                if (component.hasSettings()) {
                    header.button(Icon.settings, Styles.emptyi, 24, component::openSettings)
                            .size(32).padLeft(8)
                            .tooltip(Core.bundle.get("mdt.message.lazy-components.settings"));
                }

                // Enable/Disable toggle
                if (component.isEnabled()) {
                    header.button(Icon.eyeSmall, Styles.emptyi, 24, () -> {
                        component.setEnabled(false);
                        rebuildList();
                    }).size(32).tooltip(Core.bundle.get("mdt.message.lazy-components.disable"));
                } else {
                    header.button(Icon.eyeOffSmall, Styles.emptyi, 24, () -> {
                        component.setEnabled(true);
                        rebuildList();
                    }).size(32).tooltip(Core.bundle.get("mdt.message.lazy-components.enable"));
                }
            }).fillX().row();

            // Description
            content.add(component.getDescription()).color(Color.lightGray).wrap().width(width - 40).left().padTop(4)
                    .row();

            // Spacer to push status to bottom
            content.add().growY().row();

            // Status text at the bottom
            String statusText = component.isEnabled()
                    ? Core.bundle.get("mdt.message.lazy-components.enabled")
                    : Core.bundle.get("mdt.message.lazy-components.disabled-status");

            arc.scene.ui.Label label = new arc.scene.ui.Label(statusText);
            label.setColor(statusColor);
            content.add(label).left().padBottom(4);
        }).grow();

        parent.add(card).size(width, CARD_HEIGHT).pad(4);
    }

    private String getFilterLabel() {
        return switch (filterMode) {
            case ALL -> Core.bundle.get("mdt.message.lazy-components.filter.all");
            case ENABLED -> Core.bundle.get("mdt.message.lazy-components.filter.enabled");
            case DISABLED -> Core.bundle.get("mdt.message.lazy-components.filter.disabled");
        };
    }
}
