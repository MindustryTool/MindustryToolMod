package mindustrytool.plugins.browser;

import mindustry.ui.dialogs.BaseDialog;

import mindustrytool.plugins.browser.ui.DetailDialog;

public class ModDialogs {
    public static final BaseDialog mapDialog;
    public static final BaseDialog schematicDialog;

    static {
        mapDialog = new BrowserDialog<>(ContentType.MAP, ContentData.class, new DetailDialog());
        schematicDialog = new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class, new DetailDialog());
    }

    private ModDialogs() {
    }
}
