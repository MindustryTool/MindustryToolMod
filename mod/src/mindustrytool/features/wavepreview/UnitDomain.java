package mindustrytool.features.wavepreview;

import mindustry.type.UnitType;

/**
 * Domain categorization for units in wave previews.
 * Unmatched units fall back to GROUND.
 */
public enum UnitDomain {
    GROUND,
    AIR,
    NAVAL;

    public static UnitDomain of(UnitType type) {
        if (type.flying) {
            return AIR;
        }
        if (type.naval) {
            return NAVAL;
        }
        return GROUND;
    }
}
