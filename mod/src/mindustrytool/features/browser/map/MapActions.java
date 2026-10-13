package mindustrytool.features.browser.map;

import arc.Core;
import arc.files.Fi;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.maps.Map;
import mindustrytool.services.MindustryTool;

public final class MapActions {

    private MapActions() {
    }

    public static CompletableFuture<Void> downloadAndImport(String itemId) {
        return MindustryTool.downloadMap(itemId).thenAccept(bytes -> {
            Core.app.post(() -> {
                try {
                    Fi mapFile = Vars.customMapDirectory.child(itemId);
                    mapFile.writeBytes(bytes);
                    Vars.maps.importMap(mapFile);
                    Vars.ui.showInfoFade(Core.bundle.get("browser.map.saved"));
                } catch (Exception e) {
                    Vars.ui.showErrorMessage(Core.bundle.format("browser.map.save-error", e.getMessage()));
                }
            });
        }).exceptionally(MapActions::notifyDownloadError);
    }

    public static CompletableFuture<Void> playMap(String itemId) {
        return MindustryTool.downloadMap(itemId).thenAccept(bytes -> {
            Core.app.post(() -> {
                try {
                    Fi mapFile = Vars.customMapDirectory.child(itemId);
                    mapFile.writeBytes(bytes);
                    Map map = Vars.maps.importMap(mapFile);
                    Vars.control.playMap(map, map.rules());
                } catch (Exception e) {
                    Vars.ui.showErrorMessage(Core.bundle.format("browser.map.host-error", e.getMessage()));
                }
            });
        }).exceptionally(MapActions::notifyDownloadError);
    }

    private static Void notifyDownloadError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        Core.app.post(() -> Vars.ui.showErrorMessage(
                Core.bundle.format("browser.error.download", message)));
        return null;
    }
}
