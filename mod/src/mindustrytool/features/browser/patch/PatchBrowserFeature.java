package mindustrytool.features.browser.patch;

import arc.func.Prov;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;

/**
 * Feature for browsing, searching, previewing, and saving data patches from
 * the online library.
 */
public class PatchBrowserFeature extends Feature {

    private @Nullable PatchBrowserDialog dialog;

    public PatchBrowserFeature() {
        super(FeatureMetadata.builder()
                .id("patch-browser")
                .icon(Icon.wrench)
                .order(32)
                .enabledByDefault(true)
                .quickAccess(false)
                .build());

        bindDialog("patchBrowser", KeyCode.unset, this::showDialog);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return null;
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return () -> {
            if (dialog == null) {
                dialog = new PatchBrowserDialog();
            }
            return dialog;
        };
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new PatchBrowserDialog();
        }
        dialog.show();
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        showDialog();
    }

    @Override
    public void onQuickAccessClick() {
        showDialog();
    }

    @Override
    public Readable<Boolean> quickAccessHighlight() {
        return Readable.of(true);
    }
}
