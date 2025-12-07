package mindustrytool.presentation.dialog;

import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.domain.service.ContentType;
import mindustrytool.core.model.ContentData;

public class ModDialogs {
    public static final BaseDialog mapDialog;
    public static final BaseDialog schematicDialog;

    static {
        mapDialog = new BrowserDialog<>(ContentType.MAP, ContentData.class, new MapInfoDialog());
        schematicDialog = new BrowserDialog<>(ContentType.SCHEMATIC, ContentData.class, new SchematicInfoDialog());
    }

    private ModDialogs() {}
}
