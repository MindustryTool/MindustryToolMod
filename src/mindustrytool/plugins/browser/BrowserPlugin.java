package mindustrytool.plugins.browser;

import arc.Core;
import arc.Events;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

import mindustrytool.Main;
import mindustrytool.Plugin;

/**
 * Self-contained Browser plugin for browsing maps and schematics
 * from the MindustryTool website.
 * Uses lazy loading for dialogs to improve startup performance.
 */
public class BrowserPlugin implements Plugin {
    private static TextureRegionDrawable toolIcon;
    private static KeybindHandler keybindHandler;

    /** Registry of all lazy-loaded components in this plugin. */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    // Lazy-loaded dialogs with settings
    private static final LazyComponent<BaseDialog> mapDialog = new LazyComponent<>(
            "MapBrowser",
            Core.bundle.get("message.lazy.map-browser.desc", "Browse maps from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.MAP, ContentData.class,
                    new MapInfoDialog()))
            .onSettings(() -> new BrowserSettingsDialog(ContentType.MAP, BrowserPlugin::reloadKeybinds).show());

    private static final LazyComponent<BaseDialog> schematicDialog = new LazyComponent<>(
            "SchematicBrowser",
            Core.bundle.get("message.lazy.schematic-browser.desc", "Browse schematics from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class,
                    new SchematicInfoDialog()))
            .onSettings(() -> new BrowserSettingsDialog(ContentType.SCHEMATIC, BrowserPlugin::reloadKeybinds).show());

    static {
        lazyComponents.add(mapDialog);
        lazyComponents.add(schematicDialog);
    }

    @Override
    public String getName() {
        return "Browser";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void init() {
        BrowserDirInit.init();
        Events.on(ClientLoadEvent.class, e -> {
            addButtons();
            setupKeyboardShortcuts();
            addToGameSettings();
        });
    }

    private static void setupKeyboardShortcuts() {
        // Ensure default shortcuts are set
        KeybindHandler.ensureDefaults();

        // Register global keyboard listener
        keybindHandler = new KeybindHandler();
        Core.scene.addListener(keybindHandler);
    }

    private static void addToGameSettings() {
        // Add shortcuts to existing Controls settings (not a new category)
        Core.app.post(() -> {
            // Find the game controls category and add our keybinds there
            try {
                // Access the settings dialog's game section
                var gameSettings = Vars.ui.settings.game;

                // Add Map Browser keybind row
                new SettingKeyBind(
                        "Map Browser",
                        ContentType.MAP,
                        BrowserPlugin::reloadKeybinds).addTo(gameSettings);

                // Add Schematic Browser keybind row
                new SettingKeyBind(
                        "Schematic Browser",
                        ContentType.SCHEMATIC,
                        BrowserPlugin::reloadKeybinds).addTo(gameSettings);
            } catch (Exception e) {
                arc.util.Log.err("Failed to add keybinds to settings", e);
            }
        });
    }

    /** Reload keybind handler when settings change */
    public static void reloadKeybinds() {
        if (keybindHandler != null) {
            keybindHandler.reload();
        }
    }

    private static void addButtons() {
        loadIcon();

        // Add browse button to schematics dialog (visibility synced with enabled state)
        var browseButton = Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            var dialog = schematicDialog.getIfEnabled();
            if (dialog != null)
                dialog.show();
        }).get();
        browseButton.update(() -> browseButton.visible = schematicDialog.isEnabled());

        if (Vars.mobile) {
            // Mobile: single button opens tools dialog
            Vars.ui.menufrag.addButton("Tools", Icon.settings, () -> new ToolsMenuDialog().show());
        } else {
            // Desktop: Tools button opens custom dialog with dynamic visibility
            Vars.ui.menufrag.addButton("Tools", toolIcon, () -> new ToolsMenuDialog().show());
        }
    }

    private static void loadIcon() {
        try {
            Pixmap original = new Pixmap(Vars.mods.getMod(Main.class).root.child("icon.png"));
            int targetSize = 36;
            Pixmap scaled = new Pixmap(targetSize, targetSize);
            scaled.draw(original, 0, 0, original.width, original.height, 0, 0, targetSize, targetSize, true);
            original.dispose();
            Texture tex = new Texture(scaled);
            tex.setFilter(Texture.TextureFilter.linear);
            scaled.dispose();
            toolIcon = new TextureRegionDrawable(new TextureRegion(tex));
        } catch (Exception e) {
            toolIcon = new TextureRegionDrawable(Icon.menu.getRegion());
        }
    }

    /** Show the map browser dialog (lazy loads on first call). */
    public static void showMapBrowser() {
        mapDialog.get().show();
    }

    /** Show the schematic browser dialog (lazy loads on first call). */
    public static void showSchematicBrowser() {
        schematicDialog.get().show();
    }

    /** Get all lazy components for management. */
    public static Seq<LazyComponent<?>> getLazyComponents() {
        return lazyComponents;
    }

    /** Get map dialog lazy component. */
    public static LazyComponent<BaseDialog> getMapDialog() {
        return mapDialog;
    }

    /** Get schematic dialog lazy component. */
    public static LazyComponent<BaseDialog> getSchematicDialog() {
        return schematicDialog;
    }
}
