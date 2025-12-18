package mindustrytool.plugins.browser;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

public class ContentPreviewFactory {
    public static Button create(Table container, ContentData data, ContentType type, BaseDialog infoDialog,
            Runnable hide) {
        return create(container, data, type, infoDialog, hide, 200);
    }

    public static Button create(Table container, ContentData data, ContentType type, BaseDialog infoDialog,
            Runnable hide, int cardWidth) {
        ContentPreview.Type pType = type == ContentType.MAP ? ContentPreview.Type.MAP : ContentPreview.Type.SCHEMATIC;
        Runnable click = () -> handleClick(data, type, infoDialog, hide);
        return new ContentPreview(pType, data, click, cardWidth).create(container, infoDialog);
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
        ContentHandler.downloadSchematicBytes(data,
                bytes -> {
                    if (bytes == null) {
                        mindustry.Vars.ui.showErrorMessage("@error.network");
                        return;
                    }
                    try {
                        mindustry.game.Schematic sc = mindustry.game.Schematics
                                .read(new java.io.ByteArrayInputStream(bytes));
                        Vars.control.input.useSchematic(sc);
                    } catch (Throwable e) {
                        Vars.ui.showException("Schematic Invalid", e);
                    }
                });
        hide.run();
    }
}
