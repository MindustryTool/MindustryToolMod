package mindustrytool.features.wavepreview;

import mindustry.type.UnitType;

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
