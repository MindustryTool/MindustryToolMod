package mindustrytool.features.savesync;

import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class SaveSyncFeature extends Feature {
    public SaveSyncFeature() {
        super(FeatureMetadata.builder()
                .id("save-sync")
                .icon(FileIcon.of("cloud-upload.png"))
                .order(10)
                .enabledByDefault(false)
                .development(true)
                .build());
    }
}
