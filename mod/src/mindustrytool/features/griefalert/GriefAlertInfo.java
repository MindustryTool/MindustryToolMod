package mindustrytool.features.griefalert;

import arc.util.Time;

/**
 * Immutable snapshot of an explosive reactor placement detected near an allied core.
 * Drives the clickable HUD alert banner, the camera jump and the world highlight marker.
 */
public class GriefAlertInfo {

    /** Display name of the builder: the player name, or the team name for unowned builds. */
    public final String builderName;
    /** Localized name of the explosive block being placed. */
    public final String blockName;
    /** Tile X of the offending placement. */
    public final int tileX;
    /** Tile Y of the offending placement. */
    public final int tileY;
    /** World X of the offending placement, used for the camera jump and the world marker. */
    public final float worldX;
    /** World Y of the offending placement, used for the camera jump and the world marker. */
    public final float worldY;
    /** Tile X of the nearest threatened core. */
    public final int coreTileX;
    /** Tile Y of the nearest threatened core. */
    public final int coreTileY;
    /** {@link Time#millis()} at which this alert was raised. */
    public final long timestamp;

    public GriefAlertInfo(String builderName, String blockName, int tileX, int tileY, float worldX, float worldY,
                          int coreTileX, int coreTileY){
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
