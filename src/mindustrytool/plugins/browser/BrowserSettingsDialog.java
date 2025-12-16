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
        buttons.button("@settings.reset", Icon.refresh, this::resetToDefaults).size(210, 64);

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().pad(6).fillX();

        // === Display Section ===
        addSectionHeader(Core.bundle.get("settings.section.display", "Display"));

        // Items per page - slider
        addSliderRow(
                Core.bundle.get("settings.items-per-page", "Items per page"),
                getItemsPerPage(), 5, 100, 5,
                val -> {
                    settings.setInt(ITEMS_PER_PAGE, val);
                    notifyChange();
                });

        // Card width - slider
        addSliderRow(
                Core.bundle.get("settings.card-width", "Card width"),
                getCardWidth(), 200, 600, 20,
                val -> {
                    settings.setInt(CARD_WIDTH, val);
                    notifyChange();
                });

        // Card height - slider
        addSliderRow(
                Core.bundle.get("settings.card-height", "Card height"),
                getCardHeight(), 100, 400, 20,
                val -> {
                    settings.setInt(CARD_HEIGHT, val);
                    notifyChange();
                });
    }

    /** Add centered section header like Mindustry style */
    private void addSectionHeader(String title) {
        cont.add(title).color(arc.graphics.Color.gold).center().padTop(12).padBottom(4).row();
    }

    private void addSliderRow(String label, int current, int min, int max, int step, arc.func.Intc onChange) {
        // Mindustry style: slider with overlaid label content
        arc.scene.ui.Slider slider = new arc.scene.ui.Slider(min, max, step, false);
        slider.setValue(current);

        arc.scene.ui.Label valueLabel = new arc.scene.ui.Label(String.valueOf(current),
                mindustry.ui.Styles.outlineLabel);

        Table content = new Table();
        content.add(label, mindustry.ui.Styles.outlineLabel).left().growX().wrap();
        content.add(valueLabel).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = arc.scene.event.Touchable.disabled;

        slider.changed(() -> {
            int intVal = (int) slider.getValue();
            valueLabel.setText(String.valueOf(intVal));
            onChange.get(intVal);
        });

        // Calculate width like Mindustry
        float width = Math.min(arc.Core.graphics.getWidth() / 1.2f, 460f);
        cont.stack(slider, content).width(width).left().padTop(4f).row();
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
