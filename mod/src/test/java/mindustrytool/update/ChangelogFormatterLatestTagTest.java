package mindustrytool.update;

import static org.junit.jupiter.api.Assertions.*;

import mindustrytool.services.update.ChangelogFormatter;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

/** Pure tests for {@link ChangelogFormatter#findLatestTag(String)}. No network. */
class ChangelogFormatterLatestTagTest extends SolimEnv {

    @Test
    void findLatestTag_nullAndBlank_returnsNull() {
        assertNull(ChangelogFormatter.findLatestTag(null));
        assertNull(ChangelogFormatter.findLatestTag(""));
        assertNull(ChangelogFormatter.findLatestTag("   "));
    }

    @Test
    void findLatestTag_malformedAndNonArray_returnsNull() {
        assertNull(ChangelogFormatter.findLatestTag("not json"));
        assertNull(ChangelogFormatter.findLatestTag("{\"tag_name\":\"v1.0\"}"));
        assertNull(ChangelogFormatter.findLatestTag("[]"));
    }

    @Test
    void findLatestTag_realTagShapes_picksMaxOverBothChannels() {
        String json = "["
                + "{\"tag_name\":\"v4.59.1-v8\",\"prerelease\":false},"
                + "{\"tag_name\":\"v5.0.1-v8-beta\",\"prerelease\":true},"
                + "{\"tag_name\":\"v5.0.3-v8-beta\",\"prerelease\":true},"
                + "{\"tag_name\":\"v5.0.2-v8-beta\",\"prerelease\":true}"
                + "]";

        assertEquals("v5.0.3-v8-beta", ChangelogFormatter.findLatestTag(json));
    }

    @Test
    void findLatestTag_stableOnly_picksStableMax() {
        String json = "["
                + "{\"tag_name\":\"v4.59.0-v8\",\"prerelease\":false},"
                + "{\"tag_name\":\"v4.59.1-v8\",\"prerelease\":false}"
                + "]";

        assertEquals("v4.59.1-v8", ChangelogFormatter.findLatestTag(json));
    }

    @Test
    void findLatestTag_tie_returnsFirstWinner() {
        String json = "["
                + "{\"tag_name\":\"v5.0.3-v8\",\"prerelease\":false},"
                + "{\"tag_name\":\"v5.0.3-v8-beta\",\"prerelease\":true}"
                + "]";

        assertEquals("v5.0.3-v8", ChangelogFormatter.findLatestTag(json));
    }

    @Test
    void findLatestTag_skipsMissingBlankAndUnparseableTags() {
        String json = "["
                + "{\"prerelease\":false},"
                + "{\"tag_name\":\"\",\"prerelease\":false},"
                + "{\"tag_name\":\"   \",\"prerelease\":true},"
                + "{\"tag_name\":\"not-a-version\",\"prerelease\":true},"
                + "{\"tag_name\":null,\"prerelease\":false},"
                + "{\"tag_name\":\"v4.59.1-v8\",\"prerelease\":false}"
                + "]";

        assertEquals("v4.59.1-v8", ChangelogFormatter.findLatestTag(json));
    }

    @Test
    void findLatestTag_allUnusable_returnsNull() {
        String json = "["
                + "{\"prerelease\":false},"
                + "{\"tag_name\":\"???\",\"prerelease\":true},"
                + "null"
                + "]";

        assertNull(ChangelogFormatter.findLatestTag(json));
    }

    @Test
    void findLatestTag_doesNotThrowOnHostileInput() {
        assertDoesNotThrow(() -> ChangelogFormatter.findLatestTag("[123, true, []]]"));
        assertDoesNotThrow(() -> ChangelogFormatter.findLatestTag("[{\"tag_name\":123}]"));
    }
}
