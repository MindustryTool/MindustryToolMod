package mindustrytool.features.wavepreview;

import mindustry.type.UnitType;

/**
 * A unit type and count entry in a wave preview domain section.
 */
public class WaveUnitEntry {
    public final UnitType type;
    public final int amount;

    public WaveUnitEntry(UnitType type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}
