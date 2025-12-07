package mindustrytool.presentation.builder;


import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.model.ContentData;
import mindustrytool.domain.service.ContentType;
import mindustrytool.presentation.component.ContentPreview;
import mindustrytool.domain.handler.SchematicHandler;
import mindustrytool.domain.service.*;

public class ContentPreviewFactory {
    public static Button create(Table container, ContentData data, ContentType type, BaseDialog infoDialog, Runnable hide) {
        ContentPreview.Type pType = type == ContentType.MAP ? ContentPreview.Type.MAP : ContentPreview.Type.SCHEMATIC;
        Runnable click = () -> handleClick(data, type, infoDialog, hide);
        return new ContentPreview(pType, data, click).create(container, infoDialog);
    }

    private static void handleClick(ContentData data, ContentType type, BaseDialog infoDialog, Runnable hide) {
        if (type == ContentType.MAP) { InfoOpener.open(data, type, infoDialog); return; }
        if (Vars.state.isMenu()) { InfoOpener.open(data, type, infoDialog); return; }
        if (!Vars.state.rules.schematicsAllowed) { Vars.ui.showInfo("@schematic.disabled"); return; }
        SchematicHandler.DownloadData(data, d -> Vars.control.input.useSchematic(Utils.readSchematic(d)));
        hide.run();
    }
}
