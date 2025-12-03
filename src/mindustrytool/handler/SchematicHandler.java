package mindustrytool.handler;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustrytool.config.Utils;
import mindustrytool.data.SchematicData;
import mindustrytool.net.Api;

import static mindustry.Vars.ui;

public class SchematicHandler {
    public static void Copy(SchematicData schematic) {
        DownloadData(schematic, data -> {
            Schematic s = Utils.readSchematic(data);
            Core.app.setClipboardText(Vars.schematics.writeBase64(s));
            ui.showInfoFade("@copied");
        });
    }

    public static void Download(SchematicData schematic) {
        DownloadData(schematic, data -> {
            Schematic s = Utils.readSchematic(data);
            Api.findSchematicById(schematic.id(), detail -> {
                s.labels.add(detail.tags().map(i -> i.name()));
                s.removeSteamID();
                Vars.schematics.add(s);
                ui.showInfoFade("@schematic.saved");
            });
        });
    }

    public static void DownloadData(SchematicData data, Cons<String> cons) {
        Api.downloadSchematic(data.id(), result -> {
            cons.get(new String(Base64Coder.encode(result)));
        });
    }

}
