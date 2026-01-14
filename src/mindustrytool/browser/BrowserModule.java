package mindustrytool.browser;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.ModuleLoader.Module;
import mindustrytool.browser.data.DataMap;
import mindustrytool.browser.data.DataSchematic;
import mindustrytool.browser.gui.BrowserDialog;
import mindustrytool.browser.gui.browser.BrowserType;

/**
 * Browser module: initializes cache and dialogs, and registers UI when client loads.
 */
public class BrowserModule implements Module {
    public static final String NAME = "Browser";

    public static BrowserDialog<DataSchematic> schematicDialog;
    public static BrowserDialog<DataMap> mapDialog;

    public static Fi imageDir;
    public static Fi mapsDir;
    public static Fi schematicDir;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        Log.info("[BrowserModule] Initializing...");

        imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
        mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
        schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();

        schematicDialog = new BrowserDialog<>(BrowserType.SCHEMATIC);
        mapDialog = new BrowserDialog<>(BrowserType.MAP);

        Events.on(ClientLoadEvent.class, e -> registerUi());

        Log.info("[BrowserModule] Initialized");
    }

    private void registerUi() {
        Log.info("[BrowserModule] Registering UI");

        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            schematicDialog.show();
        });

        MenuButton mapBrowserButton = new MenuButton(Core.bundle.get("message.map-browser.title"), Icon.map, () -> mapDialog.show());

        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(mapBrowserButton.text, mapBrowserButton.icon, mapBrowserButton.runnable);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Browser", Icon.file, () -> {}, mapBrowserButton));
        }

        Log.info("[BrowserModule] UI registered");
    }
}
