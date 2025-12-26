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
import mindustrytool.plugins.playerconnect.PlayerConnectPlugin;

/**
 * Self-contained Browser plugin for browsing maps and schematics
 * from the MindustryTool website.
 * Uses lazy loading for dialogs to improve startup performance.
 */
public class BrowserPlugin implements Plugin {
    private static TextureRegionDrawable toolIcon;

    /** Registry of all lazy-loaded components in this plugin. */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    private static LazyComponentDialog componentDialog;

    // Lazy-loaded dialogs with settings
    // Lazy-loaded dialogs with settings
    private static final LazyComponent<BaseDialog> mapDialog = new LazyComponent<>(
            "MapBrowser",
            Core.bundle.get("message.lazy.map-browser.desc", "Browse maps from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.MAP, ContentData.class,
                    new mindustrytool.plugins.browser.ui.DetailDialog()));

    private static final LazyComponent<BaseDialog> schematicDialog = new LazyComponent<>(
            "SchematicBrowser",
            Core.bundle.get("message.lazy.schematic-browser.desc", "Browse schematics from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class,
                    new mindustrytool.plugins.browser.ui.DetailDialog()));

    static {
        mapDialog.onSettings(() -> new BrowserSettingsDialog(ContentType.MAP, () -> {
            if (mapDialog.isLoaded()) {
                ((BrowserDialog<?>) mapDialog.get()).reloadSettings();
            }
        }).show());

        schematicDialog.onSettings(() -> new BrowserSettingsDialog(ContentType.SCHEMATIC, () -> {
            if (schematicDialog.isLoaded()) {
                ((BrowserDialog<?>) schematicDialog.get()).reloadSettings();
            }
        }).show());

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

        // Register keybinds
        ModKeybinds.init();

        Events.on(ClientLoadEvent.class, e -> {
            addButtons();
            setupKeyboardShortcuts();
        });
    }

    private static void setupKeyboardShortcuts() {
        // Check for keybind presses every frame
        Events.run(mindustry.game.EventType.Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();

            if (noInputFocused && Core.input.keyTap(ModKeybinds.mapBrowser)) {
                BaseDialog dialog = mapDialog.getIfEnabled();
                if (dialog != null) {
                    if (dialog.isShown()) {
                        dialog.hide();
                    } else {
                        dialog.show();
                    }
                }
            }

            if (noInputFocused && Core.input.keyTap(ModKeybinds.schematicBrowser)) {
                BaseDialog dialog = schematicDialog.getIfEnabled();
                if (dialog != null) {
                    if (dialog.isShown()) {
                        dialog.hide();
                    } else {
                        dialog.show();
                    }
                }
            }

            if (noInputFocused && Core.input.keyTap(ModKeybinds.manageComponents)) {
                LazyComponentDialog dialog = getComponentDialog();
                if (dialog.isShown()) {
                    dialog.hide();
                } else {
                    dialog.show();
                }
            }
        });
    }

    private static void addButtons() {
        loadIcon();

        // Add browse button to schematics dialog (visibility synced with enabled state)
        arc.scene.ui.TextButton browseButton = Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            mindustry.ui.dialogs.BaseDialog dialog = schematicDialog.getIfEnabled();
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

    public static LazyComponentDialog getComponentDialog() {
        if (componentDialog == null) {
            Seq<LazyComponent<?>> allComponents = new Seq<>();
            allComponents.addAll(getLazyComponents());
            allComponents.addAll(PlayerConnectPlugin.getLazyComponents());
            componentDialog = new LazyComponentDialog(allComponents);
        }
        return componentDialog;
    }
}
