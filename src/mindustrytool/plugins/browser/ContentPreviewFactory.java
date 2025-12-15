package mindustrytool.plugins.browser;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

public class ContentPreviewFactory {
    public static Button create(Table container, ContentData data, ContentType type, BaseDialog infoDialog, Runnable hide) {
        ContentPreview.Type pType = type == ContentType.MAP ? ContentPreview.Type.MAP : ContentPreview.Type.SCHEMATIC;
        Runnable click = () -> handleClick(data, type, infoDialog, hide);
        return new ContentPreview(pType, data, click).create(container, infoDialog);
    }

    private static void handleClick(ContentData data, ContentType type, BaseDialog infoDialog, Runnable hide) {
        if (type == ContentType.MAP) { 
            InfoOpener.open(data, type, infoDialog); 
            return; 
        }
        if (Vars.state.isMenu()) { 
            InfoOpener.open(data, type, infoDialog); 
            return; 
        }
        if (!Vars.state.rules.schematicsAllowed) { 
            Vars.ui.showInfo("@schematic.disabled"); 
            return; 
        }
        ContentHandler.downloadSchematicData(data, d -> Vars.control.input.useSchematic(SchematicUtils.readSchematic(d)));
        hide.run();
    }
}
