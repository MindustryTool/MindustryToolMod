package mindustrytool.features.screenshot;

import arc.Core;
import arc.files.Fi;
import arc.func.Prov;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Simple in-game screen capture with toggleable UI presence. Screenshots always
 * land in the standard screenshots directory with fixed timestamped names.
 * Quick Access click captures immediately, long-click opens settings.
 */
public class ScreenshotFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> includeUiConfig;

    private final ScreenshotService service;
    private @Nullable ScreenshotSettingsDialog settingsDialog;

    public ScreenshotFeature() {
        this(new ScreenshotService());
    }

    ScreenshotFeature(ScreenshotService service) {
        super(FeatureMetadata.builder()
                .id("screenshot")
                .icon(FileIcon.of("camera.png"))
                .order(7)
                .enabledByDefault(false)
                .quickAccess(false)
                .build());

        this.service = service;
        config = configGroup();
        includeUiConfig = config.boolValue("include-ui", false);
    }

    public void resetToDefaults() {
        includeUiConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new ScreenshotSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        capture();
    }

    @Override
    public void onQuickAccessLongClick(@Nullable Element anchor) {
        Prov<SolimDialog> dialog = getSettingDialog();
        if (dialog != null) {
            dialog.get().show();
        }
    }

    /**
     * Entry point for captures. Hides the settings dialog so it never appears in
     * the image.
     */
    public void capture() {
        if (settingsDialog != null && settingsDialog.isShown()) {
            settingsDialog.hide();
        }
        doCapture();
    }

    void doCapture() {
        Fi directory = Vars.screenshotDirectory;
        String filename = ScreenshotNaming.resolveFilename(System.currentTimeMillis());
        Boolean includeUi = includeUiConfig.signal().peek();

        boolean queued = service.capture(directory, filename, Boolean.TRUE.equals(includeUi),
                this::onCaptureSuccess, this::onCaptureError);
        if (!queued) {
            Log.info("Screenshot debounced: capture requested too soon after previous capture");
        }
    }

    private void onCaptureSuccess(Fi file) {
        Core.app.post(() -> Vars.ui.showInfoFade(
                Core.bundle.format("feature.screenshot.success.text", file.name())));
    }

    private void onCaptureError(Exception e) {
        Log.err("Screenshot failed", e);
        Core.app.post(() -> Vars.ui.showErrorMessage(
                Core.bundle.format("feature.screenshot.error.write", String.valueOf(e.getMessage()))));
    }
}
