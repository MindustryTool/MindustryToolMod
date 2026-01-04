package mindustrytool.features.content.browser;

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
import mindustrytool.Feature;

/**
 * Self-contained Browser plugin for browsing maps and schematics
 * from the MindustryTool website.
 * Uses lazy loading for dialogs to improve startup performance.
 */
public class BrowserFeature implements Feature {
    private static TextureRegionDrawable toolIcon;

    /** Registry of all lazy-loaded components in this plugin. */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    // private static LazyComponentDialog componentDialog; // Removed to force
    // refresh

    // Lazy-loaded dialogs with settings
    // Lazy-loaded dialogs with settings
    private static final LazyComponent<BaseDialog> mapDialog = new LazyComponent<>(
            "MapBrowser",
            Core.bundle.get("mdt.message.lazy.map-browser.desc", "Browse maps from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.MAP, ContentData.class,
                    new mindustrytool.features.content.browser.ui.DetailDialog()));

    private static final LazyComponent<BaseDialog> schematicDialog = new LazyComponent<>(
            "SchematicBrowser",
            Core.bundle.get("mdt.message.lazy.schematic-browser.desc", "Browse schematics from MindustryTool"),
            (arc.func.Prov<BaseDialog>) () -> new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class,
                    new mindustrytool.features.content.browser.ui.DetailDialog()));

    // Background component - uses Object as it doesn't need a dialog instance
    private static final LazyComponent<Object> backgroundComponent = new LazyComponent<>(
            "CustomBackground",
            Core.bundle.get("mdt.message.lazy.custom-background.desc", "Customize the main menu background"),
            () -> null, // No actual object needed, BackgroundFeature handles rendering
            true);

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

        backgroundComponent.onSettings(() -> mindustrytool.features.tools.background.BackgroundSettingsDialog.open());
        backgroundComponent.onToggle(
                enabled -> mindustrytool.features.tools.background.BackgroundFeature.getInstance().setEnabled(enabled));

        lazyComponents.add(mapDialog);
        lazyComponents.add(schematicDialog);
        lazyComponents.add(backgroundComponent);
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

        // Add browse button to Custom Game dialog via Reflection
        // SECURITY: 'custom' dialog is a private field in UI. Using reflection to
        // inject button.
        try {
            Object custom = arc.util.Reflect.get(Vars.ui, "custom");
            if (custom instanceof BaseDialog) {
                ((BaseDialog) custom).shown(() -> {
                    // Check by name to avoid duplicates
                    if (((BaseDialog) custom).buttons.find("map-browse") == null) {
                        ((BaseDialog) custom).buttons.button("Browse", Icon.menu, () -> {
                            ((BaseDialog) custom).hide();
                            mindustry.ui.dialogs.BaseDialog dialog = mapDialog.getIfEnabled();
                            if (dialog != null)
                                dialog.show();
                        }).size(210f, 64f).name("map-browse");
                    }
                });
            }
        } catch (Throwable e) {
            // Ignore if custom dialog not found
        }

        // Add browse button to Maps dialog via Reflection
        // SECURITY: 'maps' dialog is a private field in UI. Using reflection to inject
        // button.
        try {
            Object maps = arc.util.Reflect.get(Vars.ui, "maps");
            if (maps instanceof BaseDialog) {
                ((BaseDialog) maps).shown(() -> {
                    if (((BaseDialog) maps).buttons.find("map-browse") == null) {
                        ((BaseDialog) maps).buttons.button("Browse", Icon.menu, () -> {
                            ((BaseDialog) maps).hide();
                            mindustry.ui.dialogs.BaseDialog dialog = mapDialog.getIfEnabled();
                            if (dialog != null)
                                dialog.show();
                        }).size(210f, 64f).name("map-browse");
                    }
                });
            }
        } catch (Throwable e) {
            // Ignore if maps dialog not found
        }

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
        return new LazyComponentDialog(mindustrytool.utils.ComponentRegistry.getAllComponents());
    }
}
