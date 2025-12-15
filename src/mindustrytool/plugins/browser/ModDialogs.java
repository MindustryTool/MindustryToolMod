package mindustrytool.plugins.browser;

import mindustry.ui.dialogs.BaseDialog;

public class ModDialogs {
    public static final BaseDialog mapDialog;
    public static final BaseDialog schematicDialog;

    static {
        mapDialog = new BrowserDialog<>(ContentType.MAP, ContentData.class, new MapInfoDialog());
        schematicDialog = new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class, new SchematicInfoDialog());
    }

    private ModDialogs() {}
}
