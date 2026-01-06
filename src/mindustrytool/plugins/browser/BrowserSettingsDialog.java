package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.layout.*;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Settings dialog for Browser plugins (Map/Schematic Browser).
 * Allows customization of display options.
 */
public class BrowserSettingsDialog extends BaseDialog {
    private final PluginSettings settings;
    private final Runnable onSettingsChanged;

    // Setting keys
    private static final String ITEMS_PER_PAGE = "itemsPerPage";
    private static final String CARD_WIDTH = "cardWidth";
    private static final String CARD_HEIGHT = "cardHeight";
    private static final String SHOW_PREVIEW = "showPreview";
    private static final String CACHE_SIZE_MB = "cacheSizeMB";

    public BrowserSettingsDialog(ContentType type, Runnable onSettingsChanged) {
        super(Core.bundle.get("message.lazy-components.settings", "Settings") + " - " + type.name());
        this.settings = new PluginSettings("browser." + type.name().toLowerCase());
        this.onSettingsChanged = onSettingsChanged;

        addCloseButton();
        buttons.button("@settings.reset", Icon.refresh, this::resetToDefaults).size(250, 64);

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        
        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        
        cont.pane(t -> {
            t.defaults().pad(6).growX();

            // === Display Section ===
            addSectionHeader(t, Core.bundle.get("settings.section.display", "Display"));

            // Items per page - slider
            addSliderRow(t,
                    Core.bundle.get("settings.items-per-page", "Items per page"),
                    getItemsPerPage(), 5, 100, 5,
                    val -> {
                        settings.setInt(ITEMS_PER_PAGE, val);
                        notifyChange();
                    });

            // Card Size (Scale) - slider
            // Calculate current scale based on width (Base 300)
            int currentScale = (int) ((getCardWidth() / 300f) * 100);

            addSliderRow(t,
                    Core.bundle.get("settings.card-scale", "Card Size"),
                    currentScale, 50, 200, 10,
                    val -> val + "%",
                    val -> {
                        // Update both width and height to maintain aspect ratio (300x200 base)
                        settings.setInt(CARD_WIDTH, (int) (300 * (val / 100f)));
                        settings.setInt(CARD_HEIGHT, (int) (200 * (val / 100f)));
                        notifyChange();
                    });
        }).width(width).maxHeight(Core.graphics.getHeight() * 0.8f).row();
    }

    /** Add centered section header like Mindustry style */
    private void addSectionHeader(Table table, String title) {
        table.add(title).color(mindustry.graphics.Pal.accent).center().padTop(12).padBottom(4).row();
    }

    // Overload for backward compatibility / simple integers
    private void addSliderRow(Table table, String label, int current, int min, int max, int step, arc.func.Intc onChange) {
        addSliderRow(table, label, current, min, max, step, String::valueOf, onChange);
    }

    private void addSliderRow(Table table, String label, int current, int min, int max, int step,
            arc.func.Func<Integer, String> format, arc.func.Intc onChange) {
        // Mindustry style: slider with overlaid label content
        arc.scene.ui.Slider slider = new arc.scene.ui.Slider(min, max, step, false);
        slider.setValue(current);

        arc.scene.ui.Label valueLabel = new arc.scene.ui.Label(format.get(current),
                mindustry.ui.Styles.outlineLabel);

        Table content = new Table();
        content.add(label, mindustry.ui.Styles.outlineLabel).left().growX().wrap();
        content.add(valueLabel).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = arc.scene.event.Touchable.disabled;

        slider.changed(() -> {
            int intVal = (int) slider.getValue();
            valueLabel.setText(format.get(intVal));
            onChange.get(intVal);
        });

        // Calculate width like Mindustry (passed from parent or recalculated? Recalculated is fine for stack)
        // But we are inside a table that already has width set.
        // So we should use growX() on the stack.
        
        table.stack(slider, content).height(40f).growX().padTop(4f).row();
    }

    private void resetToDefaults() {
        settings.remove(ITEMS_PER_PAGE);
        settings.remove(CARD_WIDTH);
        settings.remove(CARD_HEIGHT);
        settings.remove(SHOW_PREVIEW);
        settings.remove(CACHE_SIZE_MB);
        rebuild();
        notifyChange();
    }

    private void notifyChange() {
        if (onSettingsChanged != null)
            onSettingsChanged.run();
    }

    // Public getters for Browser dialogs to use
    public int getItemsPerPage() {
        return settings.getInt(ITEMS_PER_PAGE, 20);
    }

    public int getCardWidth() {
        return settings.getInt(CARD_WIDTH, 300);
    }

    public int getCardHeight() {
        return settings.getInt(CARD_HEIGHT, 200);
    }

    public boolean getShowPreview() {
        return settings.getBool(SHOW_PREVIEW, true);
    }

    public int getCacheSizeMB() {
        return settings.getInt(CACHE_SIZE_MB, 50);
    }

    // Static getters for external access
    public static int getItemsPerPage(ContentType type) {
        return new PluginSettings("browser." + type.name().toLowerCase()).getInt(ITEMS_PER_PAGE, 20);
    }

    public static int getCardWidth(ContentType type) {
        return new PluginSettings("browser." + type.name().toLowerCase()).getInt(CARD_WIDTH, 300);
    }

    public static int getCardHeight(ContentType type) {
        return new PluginSettings("browser." + type.name().toLowerCase()).getInt(CARD_HEIGHT, 200);
    }

    public static boolean getShowPreview(ContentType type) {
        return new PluginSettings("browser." + type.name().toLowerCase()).getBool(SHOW_PREVIEW, true);
    }
}
