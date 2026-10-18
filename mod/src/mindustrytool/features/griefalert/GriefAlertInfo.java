package mindustrytool.features.griefalert;

import arc.util.Time;

public class GriefAlertInfo {

    public final String builderName;

    public final String blockName;

    public final int tileX;

    public final int tileY;

    public final float worldX;

    public final float worldY;

    public final int coreTileX;

    public final int coreTileY;

    public final long timestamp;

    public GriefAlertInfo(String builderName, String blockName, int tileX, int tileY, float worldX, float worldY,
            int coreTileX, int coreTileY) {
        this.builderName = builderName;
        this.blockName = blockName;
        this.tileX = tileX;
        this.tileY = tileY;
        this.worldX = worldX;
        this.worldY = worldY;
        this.coreTileX = coreTileX;
        this.coreTileY = coreTileY;
        this.timestamp = Time.millis();
    }
}
