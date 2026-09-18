package mindustrytool.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import org.junit.jupiter.api.Test;

class TimeZonesTest {

    @Test
    void systemDefaultOrUtc_returnsNonNullZone() {
        ZoneId zone = TimeZones.systemDefaultOrUtc();
        assertNotNull(zone);
    }

    @Test
    void resolveOrFallback_returnsSupplierValueWhenAvailable() {
        ZoneId expected = ZoneId.of("UTC");
        ZoneId actual = TimeZones.resolveOrFallback(() -> expected, ZoneOffset.ofHours(5));
        assertEquals(expected, actual);
    }

    @Test
    void resolveOrFallback_fallsBackOnZoneRulesException() {
        ZoneId fallback = ZoneOffset.UTC;
        ZoneId actual = TimeZones.resolveOrFallback(() -> {
            throw new ZoneRulesException("Simulated missing tzdb.dat");
        }, fallback);

        assertEquals(fallback, actual);
    }

    @Test
    void resolveOrFallback_fallsBackOnExceptionInInitializerError() {
        ZoneId fallback = ZoneOffset.UTC;
        ZoneId actual = TimeZones.resolveOrFallback(() -> {
            throw new ExceptionInInitializerError("Simulated class init failure");
        }, fallback);

        assertEquals(fallback, actual);
    }

    @Test
    void resolveOrFallback_fallsBackOnNullSupplierResult() {
        ZoneId fallback = ZoneOffset.UTC;
        ZoneId actual = TimeZones.resolveOrFallback(() -> null, fallback);

        assertEquals(fallback, actual);
    }
}
