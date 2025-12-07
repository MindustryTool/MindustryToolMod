package mindustrytool.domain.handler;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustrytool.core.model.ContentData;
import mindustrytool.data.api.Api;
import static mindustry.Vars.ui;
import mindustrytool.domain.service.Utils;

public final class SchematicHandler {
    private SchematicHandler() {}

    public static void Copy(ContentData s) {
        DownloadData(s, d -> {
            Schematic sc = Utils.readSchematic(d);
            if (sc == null) { ui.showErrorMessage("@schematic.invalid"); return; }
            Core.app.setClipboardText(Vars.schematics.writeBase64(sc));
            ui.showInfoFade("@copied");
        });
    }

    public static void Download(ContentData s) {
        DownloadData(s, d -> {
            Schematic sc = Utils.readSchematic(d);
            if (sc == null) { ui.showErrorMessage("@schematic.invalid"); return; }
            Api.findSchematicById(s.id(), detail -> {
                sc.labels.add(detail.tags().map(i -> i.name()));
                sc.removeSteamID();
                Vars.schematics.add(sc);
                ui.showInfoFade("@schematic.saved");
            });
        });
    }

    public static void DownloadData(ContentData data, Cons<String> cons) {
        Api.downloadSchematic(data.id(), r -> cons.get(new String(Base64Coder.encode(r))));
    }
}
