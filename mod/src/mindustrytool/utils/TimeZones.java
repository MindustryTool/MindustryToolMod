package mindustrytool.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import arc.func.Prov;

public final class TimeZones {

    private TimeZones() {
    }

    public static ZoneId systemDefaultOrUtc() {
        return resolveOrFallback(ZoneId::systemDefault, ZoneOffset.UTC);
    }

    static ZoneId resolveOrFallback(Prov<ZoneId> zoneSupplier, ZoneId fallback) {
        try {
            ZoneId resolved = zoneSupplier.get();
            return resolved != null ? resolved : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
