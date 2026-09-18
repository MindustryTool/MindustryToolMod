package mindustrytool.services.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.files.Fi;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CrashTimestampParserTest {

    @Test
    void parse_nullFile_returnsZero() {
        assertEquals(0L, CrashTimestampParser.parse(null));
    }

    @Test
    void parse_unrelatedFilename_returnsZero() {
        Fi file = new Fi("some_random_file.txt");
        assertEquals(0L, CrashTimestampParser.parse(file));
    }

    @Test
    void parse_invalidCrashReportDate_returnsZero() {
        Fi file = new Fi("crash-report-not_a_valid_date.txt");
        assertEquals(0L, CrashTimestampParser.parse(file));
    }

    @Test
    void parse_validCrashReport_returnsUtcEpochMilli() {
        Fi file = new Fi("crash-report-01_25_2026_10_30_00.txt");
        long expected = LocalDateTime.of(2026, 1, 25, 10, 30, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();

        long actual = CrashTimestampParser.parse(file);
        assertEquals(expected, actual);
    }

    @Test
    void parse_preservesChronologicalOrder() {
        Fi earlier = new Fi("crash-report-01_25_2026_10_30_00.txt");
        Fi later = new Fi("crash-report-01_25_2026_10_30_01.txt");

        long earlierEpoch = CrashTimestampParser.parse(earlier);
        long laterEpoch = CrashTimestampParser.parse(later);

        assertTrue(laterEpoch > earlierEpoch);
        assertEquals(1000L, laterEpoch - earlierEpoch);
    }

    @Test
    void parse_validEpochCrashFilename_returnsEpoch() {
        Fi file = new Fi("crash_1700000000000.txt");
        assertEquals(1700000000000L, CrashTimestampParser.parse(file));
    }

    @Test
    void parse_invalidEpochCrashFilename_returnsZero() {
        Fi file = new Fi("crash_invalid_epoch.txt");
        assertEquals(0L, CrashTimestampParser.parse(file));
    }
}
