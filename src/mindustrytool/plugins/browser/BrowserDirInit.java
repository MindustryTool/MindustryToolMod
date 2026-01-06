package mindustrytool.plugins.browser;

import arc.files.Fi;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;

public class BrowserDirInit {
    public static final Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static final Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static final Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

    public static void init() {
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();
    }
}
