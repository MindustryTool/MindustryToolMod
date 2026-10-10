package mindustrytool.features.browser.patch;

import arc.Core;
import arc.files.Fi;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustrytool.services.MindustryTool;

/**
 * Utility actions for content patch operations: clipboard copy and local file export.
 */
public final class PatchActions {

    private PatchActions() {
    }

    /**
     * Fetches the content patch detail (which includes the HJSON data) and copies
     * the raw text to the clipboard.
     */
    public static CompletableFuture<Void> copyToClipboard(String itemId) {
        return MindustryTool.findContentPatch(itemId).thenAccept(detail -> {
            Core.app.post(() -> {
                try {
                    String data = detail != null && detail.getData() != null ? detail.getData() : "";
                    Core.app.setClipboardText(data);
                    Vars.ui.showInfoFade(Core.bundle.get("browser.patch.copied"));
                } catch (Exception e) {
                    Vars.ui.showException(e);
                }
            });
        }).exceptionally(PatchActions::notifyDownloadError);
    }

    /**
     * Directly copies provided HJSON patch text to clipboard.
     */
    public static void copyTextToClipboard(String data) {
        Core.app.setClipboardText(data != null ? data : "");
        Vars.ui.showInfoFade(Core.bundle.get("browser.patch.copied"));
    }

    /**
     * Downloads the patch and saves it as a .hjson file in the patches folder.
     */
    public static CompletableFuture<Void> saveToFile(String itemId, String patchName) {
        return MindustryTool.findContentPatch(itemId).thenAccept(detail -> {
            Core.app.post(() -> {
                try {
                    String data = detail != null && detail.getData() != null ? detail.getData() : "";
                    saveTextToFile(data, patchName);
                } catch (Exception e) {
                    Vars.ui.showErrorMessage(
                            Core.bundle.format("browser.patch.save-error", e.getMessage()));
                }
            });
        }).exceptionally(PatchActions::notifyDownloadError);
    }

    /**
     * Saves raw HJSON string directly to the local game patches directory.
     */
    public static void saveTextToFile(String data, String patchName) {
        try {
            String safeName = (patchName != null && !patchName.trim().isEmpty())
                    ? patchName.trim().replaceAll("[\\\\/:*?\"<>|]", "_")
                    : "patch_" + System.currentTimeMillis();

            Fi patchesDir = Vars.dataDirectory.child("patches");
            if (!patchesDir.exists()) {
                patchesDir.mkdirs();
            }

            Fi file = patchesDir.child(safeName + ".hjson");
            file.writeString(data != null ? data : "");
            Vars.ui.showInfoFade(Core.bundle.format("browser.patch.saved", "patches/" + file.name()));
        } catch (Exception e) {
            Vars.ui.showErrorMessage(
                    Core.bundle.format("browser.patch.save-error", e.getMessage()));
        }
    }

    private static Void notifyDownloadError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        Core.app.post(() -> Vars.ui.showErrorMessage(
                Core.bundle.format("browser.error.download", message)));
        return null;
    }
}
