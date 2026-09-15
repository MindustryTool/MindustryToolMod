package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.util.serialization.Base64Coder;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustrytool.models.response.TagData;
import mindustrytool.services.MindustryTool;

/**
 * Utility actions for schematic operations: clipboard copy, local library
 * save, and in-game placement. Downloaded bytes are canonicalized through
 * parsing, mirroring the legacy browser behavior.
 */
public final class SchematicActions {

    private SchematicActions() {
    }

    /**
     * Downloads the schematic, parses it, and copies its canonical Base64
     * representation to the clipboard.
     */
    public static CompletableFuture<Void> copyToClipboard(String itemId) {
        return MindustryTool.downloadSchematic(itemId).thenAccept(bytes -> {
            Core.app.post(() -> {
                try {
                    Schematic schematic = parse(bytes);
                    Core.app.setClipboardText(Vars.schematics.writeBase64(schematic));
                    Vars.ui.showInfoFade(Core.bundle.get("browser.schematic.copied"));
                } catch (Exception e) {
                    Vars.ui.showException(e);
                }
            });
        }).exceptionally(SchematicActions::notifyDownloadError);
    }

    /**
     * Downloads the schematic, attaches its server tags as labels, and adds it
     * to the local schematic library.
     */
    public static CompletableFuture<Void> saveToLocal(String itemId) {
        return MindustryTool.downloadSchematic(itemId).thenAccept(bytes -> {
            MindustryTool.findSchematic(itemId).whenComplete((detail, throwable) -> {
                Core.app.post(() -> {
                    try {
                        Schematic schematic = parse(bytes);
                        if (detail != null && detail.getTags() != null) {
                            for (TagData tag : detail.getTags()) {
                                if (tag != null && tag.getName() != null) {
                                    schematic.labels.add(tag.getName());
                                }
                            }
                        }
                        schematic.removeSteamID();
                        Vars.schematics.add(schematic);
                        Vars.ui.showInfoFade(Core.bundle.get("browser.schematic.saved"));
                    } catch (Exception e) {
                        Vars.ui.showErrorMessage(
                                Core.bundle.format("browser.schematic.save-error", e.getMessage()));
                    }
                });
            });
        }).exceptionally(SchematicActions::notifyDownloadError);
    }

    /**
     * Downloads the schematic and attaches it to the player's placement cursor
     * for direct in-game placement.
     */
    public static CompletableFuture<Void> placeInGame(String itemId) {
        return MindustryTool.downloadSchematic(itemId).thenAccept(bytes -> {
            Core.app.post(() -> {
                try {
                    Vars.control.input.useSchematic(parse(bytes));
                } catch (Exception e) {
                    Vars.ui.showErrorMessage(
                            Core.bundle.format("browser.schematic.place-error", e.getMessage()));
                }
            });
        }).exceptionally(SchematicActions::notifyDownloadError);
    }

    /**
     * Returns true when the player is in an active match where placing
     * schematics is allowed.
     */
    public static boolean canPlaceInGame() {
        return Vars.state.isGame() && Vars.state.rules.schematicsAllowed;
    }

    private static Schematic parse(byte[] bytes) {
        return Schematics.readBase64(new String(Base64Coder.encode(bytes)));
    }

    private static Void notifyDownloadError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        Core.app.post(() -> Vars.ui.showErrorMessage(
                Core.bundle.format("browser.error.download", message)));
        return null;
    }
}
