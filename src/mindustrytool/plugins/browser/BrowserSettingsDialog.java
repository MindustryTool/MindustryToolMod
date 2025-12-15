package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.layout.*;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Settings dialog for Browser plugins (Map/Schematic Browser).
 * Allows customization of display options.
 */
public class BrowserSettingsDialog extends BaseDialog {
    private final PluginSettings settings;
    private final ContentType contentType;
    private final Runnable onSettingsChanged;

    // Setting keys
    private static final String ITEMS_PER_PAGE = "itemsPerPage";
    private static final String CARD_WIDTH = "cardWidth";
    private static final String CARD_HEIGHT = "cardHeight";
    private static final String SHOW_PREVIEW = "showPreview";
    private static final String CACHE_SIZE_MB = "cacheSizeMB";

    public BrowserSettingsDialog(ContentType type, Runnable onSettingsChanged) {
        super(Core.bundle.get("message.lazy-components.settings", "Settings") + " - " + type.name());
        this.contentType = type;
        this.settings = new PluginSettings("browser." + type.name().toLowerCase());
        this.onSettingsChanged = onSettingsChanged;

        addCloseButton();
        buttons.button("@settings.reset", Icon.refresh, this::resetToDefaults).size(210, 64);

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().pad(10).left();

        // Items per page
        cont.add(Core.bundle.get("settings.items-per-page", "Items per page:")).left();
        cont.table(t -> {
            int current = getItemsPerPage();
            t.button("-", () -> {
                settings.setInt(ITEMS_PER_PAGE, Math.max(5, current - 5));
                rebuild();
                notifyChange();
            }).size(40);
            t.add(String.valueOf(current)).width(60).center();
            t.button("+", () -> {
                settings.setInt(ITEMS_PER_PAGE, Math.min(100, current + 5));
                rebuild();
                notifyChange();
            }).size(40);
        }).row();

        // Card width
        cont.add(Core.bundle.get("settings.card-width", "Card width:")).left();
        cont.table(t -> {
            int current = getCardWidth();
            t.button("-", () -> {
                settings.setInt(CARD_WIDTH, Math.max(200, current - 20));
                rebuild();
                notifyChange();
            }).size(40);
            t.add(String.valueOf(current)).width(60).center();
            t.button("+", () -> {
                settings.setInt(CARD_WIDTH, Math.min(600, current + 20));
                rebuild();
                notifyChange();
            }).size(40);
        }).row();

        // Card height
        cont.add(Core.bundle.get("settings.card-height", "Card height:")).left();
        cont.table(t -> {
            int current = getCardHeight();
            t.button("-", () -> {
                settings.setInt(CARD_HEIGHT, Math.max(100, current - 20));
                rebuild();
                notifyChange();
            }).size(40);
            t.add(String.valueOf(current)).width(60).center();
            t.button("+", () -> {
                settings.setInt(CARD_HEIGHT, Math.min(400, current + 20));
                rebuild();
                notifyChange();
            }).size(40);
        }).row();

        // Show preview toggle
        cont.add(Core.bundle.get("settings.show-preview", "Show preview:")).left();
        cont.check("", getShowPreview(), val -> {
            settings.setBool(SHOW_PREVIEW, val);
            notifyChange();
        }).row();

        // Cache size
        cont.add(Core.bundle.get("settings.cache-size", "Cache size (MB):")).left();
        cont.table(t -> {
            int current = getCacheSizeMB();
            t.button("-", () -> {
                settings.setInt(CACHE_SIZE_MB, Math.max(10, current - 10));
                rebuild();
                notifyChange();
            }).size(40);
            t.add(String.valueOf(current)).width(60).center();
            t.button("+", () -> {
                settings.setInt(CACHE_SIZE_MB, Math.min(500, current + 10));
                rebuild();
                notifyChange();
            }).size(40);
        }).row();
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
