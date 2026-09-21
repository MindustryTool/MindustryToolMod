package mindustrytool.features.screenshot;

import static org.junit.jupiter.api.Assertions.*;

import arc.files.Fi;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solim.test.SolimEnv;

class ScreenshotNamingTest extends SolimEnv {

    @Test
    void resolveFilenameProducesFixedTimestampedPng() {
        String name = ScreenshotNaming.resolveFilename(0L);

        assertTrue(name.startsWith("screenshot-"), "Fixed prefix must be used: " + name);
        assertTrue(name.endsWith(".png"), "PNG extension must be present: " + name);
    }

    @Test
    void formatTimestampMatchesExpectedShape() {
        String timestamp = ScreenshotNaming.formatTimestamp(0L);

        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"),
                "Timestamp must look like yyyy-MM-dd_HH-mm-ss: " + timestamp);
    }

    @Test
    void resolveCollisionReturnsOriginalWhenFree(@TempDir File tempDir) {
        Fi dir = new Fi(tempDir);

        Fi target = ScreenshotNaming.resolveCollision(dir, "screenshot-2026-09-17_19-30-00.png");

        assertEquals("screenshot-2026-09-17_19-30-00.png", target.name());
    }

    @Test
    void resolveCollisionAppendsIncrementalSuffix(@TempDir File tempDir) throws IOException {
        Fi dir = new Fi(tempDir);
        assertTrue(new File(tempDir, "shot.png").createNewFile());
        assertTrue(new File(tempDir, "shot_1.png").createNewFile());

        Fi target = ScreenshotNaming.resolveCollision(dir, "shot.png");

        assertEquals("shot_2.png", target.name());
    }

    @Test
    void resolveCollisionFallsBackToTimestampOnBlank(@TempDir File tempDir) {
        Fi dir = new Fi(tempDir);

        Fi target = ScreenshotNaming.resolveCollision(dir, "   ");

        assertTrue(target.name().startsWith("screenshot-"), "Blank name must fall back: " + target.name());
        assertTrue(target.name().endsWith(".png"));
    }
}
