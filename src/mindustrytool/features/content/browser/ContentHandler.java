package mindustrytool.features.content.browser;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;

import mindustry.Vars;
import mindustry.game.Schematic;
import static mindustry.Vars.ui;

/** Unified content download handlers for maps and schematics. */
public final class ContentHandler {
    private ContentHandler() {
    }

    // Map operations
    public static void downloadMap(ContentData map) {
        String safeName = sanitizeFilename(map.id());
        if (!map.id().equals(safeName)) {
            // Log warning? Or just use safe name? For security, safe name is best.
            // But if ID was meant to be a path, this breaks it - which is intended.
        }

        Api.downloadMap(map.id(), result -> {
            Fi file = Vars.customMapDirectory.child(safeName + ".msav");
            file.writeBytes(result);
            Vars.maps.importMap(file);
            ui.showInfoFade("@map.saved");
        });
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    // Schematic operations
    // Schematic operations
    public static void copySchematic(ContentData s) {
        downloadSchematicBytes(s, bytes -> {
            if (bytes == null) {
                ui.showErrorMessage("@error.network");
                return;
            }
            try {
                checkHeader(bytes);
                Schematic sc = mindustry.game.Schematics
                        .read(new java.io.ByteArrayInputStream(bytes));
                Core.app.setClipboardText(Vars.schematics.writeBase64(sc));
                ui.showInfoFade("@copied");
            } catch (Throwable e) {
                ui.showException("Schematic Invalid", e);
            }
        });
    }

    public static void downloadSchematic(ContentData s) {
        downloadSchematicBytes(s, bytes -> {
            if (bytes == null) {
                ui.showErrorMessage("@error.network");
                return;
            }
            try {
                checkHeader(bytes);
                Schematic sc = mindustry.game.Schematics
                        .read(new java.io.ByteArrayInputStream(bytes));
                Api.findSchematicById(s.id(), detail -> {
                    sc.labels.add(detail.tags().map(i -> i.name()));
                    sc.removeSteamID();
                    Vars.schematics.add(sc);
                    ui.showInfoFade("@schematic.saved");
                });
            } catch (Throwable e) {
                ui.showException("Schematic Invalid", e);
            }
        });
    }

    public static void downloadSchematicBytes(ContentData data, Cons<byte[]> cons) {
        Api.downloadSchematic(data.id(), r -> cons.get(r));
    }

    private static void checkHeader(byte[] bytes) throws java.io.IOException {
        if (bytes.length < 4)
            throw new java.io.IOException("Data too short: " + bytes.length);
        if (bytes[0] != 0x6D || bytes[1] != 0x73 || bytes[2] != 0x63 || bytes[3] != 0x68) {
            String hex = "";
            for (int i = 0; i < Math.min(bytes.length, 8); i++)
                hex += String.format("%02X ", bytes[i]);
            throw new java.io.IOException("Invalid Header: " + hex);
        }
    }
}
