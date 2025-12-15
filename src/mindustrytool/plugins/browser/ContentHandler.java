package mindustrytool.plugins.browser;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematic;
import static mindustry.Vars.ui;

/** Unified content download handlers for maps and schematics. */
public final class ContentHandler {
    private ContentHandler() {}

    // Map operations
    public static void downloadMap(ContentData map) {
        Api.downloadMap(map.id(), result -> {
            Fi file = Vars.customMapDirectory.child(map.id());
            file.writeBytes(result);
            Vars.maps.importMap(file);
            ui.showInfoFade("@map.saved");
        });
    }

    // Schematic operations  
    public static void copySchematic(ContentData s) {
        downloadSchematicData(s, d -> {
            Schematic sc = SchematicUtils.readSchematic(d);
            if (sc == null) { ui.showErrorMessage("@schematic.invalid"); return; }
            Core.app.setClipboardText(Vars.schematics.writeBase64(sc));
            ui.showInfoFade("@copied");
        });
    }

    public static void downloadSchematic(ContentData s) {
        downloadSchematicData(s, d -> {
            Schematic sc = SchematicUtils.readSchematic(d);
            if (sc == null) { ui.showErrorMessage("@schematic.invalid"); return; }
            Api.findSchematicById(s.id(), detail -> {
                sc.labels.add(detail.tags().map(i -> i.name()));
                sc.removeSteamID();
                Vars.schematics.add(sc);
                ui.showInfoFade("@schematic.saved");
            });
        });
    }

    public static void downloadSchematicData(ContentData data, Cons<String> cons) {
        Api.downloadSchematic(data.id(), r -> cons.get(new String(Base64Coder.encode(r))));
    }
}
