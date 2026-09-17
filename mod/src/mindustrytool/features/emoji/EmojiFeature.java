package mindustrytool.features.emoji;

import arc.func.Prov;
import arc.util.Nullable;
import mindustry.gen.Iconc;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.overlay.SolimDialog;

/**
 * Standalone Emoji feature exposing a searchable {@link Iconc} glyph browser
 * from its feature card and the QuickAccess bar.
 */
public class EmojiFeature extends Feature {

    private @Nullable EmojiDialog dialog;

    public EmojiFeature() {
        super(FeatureMetadata.builder()
                .id("emoji")
                .icon(FileIcon.of("smile.png"))
                .order(6)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return () -> {
            if (dialog == null) {
                dialog = new EmojiDialog();
            }
            return dialog;
        };
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new EmojiDialog();
        }
        dialog.show();
    }

    @Override
    public void onQuickAccessClick() {
        showDialog();
    }
}
