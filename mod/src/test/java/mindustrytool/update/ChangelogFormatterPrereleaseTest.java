package mindustrytool.update;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import mindustrytool.services.update.ChangelogFormatter;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class ChangelogFormatterPrereleaseTest extends SolimEnv {

    private static String releasesJson() {
        return "["
                + "{\"tag_name\":\"v2.0\",\"body\":\"stable release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false},"
                + "{\"tag_name\":\"v3.0-beta\",\"body\":\"beta release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":true}"
                + "]";
    }

    @Test
    void format_excludesPrereleaseWhenDisabled() {
        String out = ChangelogFormatter.format(releasesJson(), false);

        assertTrue(out.contains("[accent]v2.0[white]"));
        assertFalse(out.contains("v3.0-beta"));
    }

    @Test
    void format_includesPrereleaseWhenEnabled() {
        String out = ChangelogFormatter.format(releasesJson(), true);

        assertTrue(out.contains("[accent]v2.0[white]"));
        assertTrue(out.contains("[accent]v3.0-beta[white]"));
    }

    @Test
    void format_withoutFlag_includesAllForBackwardCompat() {
        String out = ChangelogFormatter.format(releasesJson());

        assertTrue(out.contains("[accent]v2.0[white]"));
        assertTrue(out.contains("[accent]v3.0-beta[white]"));
    }

    @Test
    void format_onlyPrereleasesExcluded_returnsEmpty() {
        String json =
                "[{\"tag_name\":\"v3.0-beta\",\"body\":\"beta\",\"published_at\":\"\",\"assets\":[],\"prerelease\":true}]";

        assertEquals("", ChangelogFormatter.format(json, false));
        assertTrue(ChangelogFormatter.format(json, true).contains("v3.0-beta"));
    }

    @Test
    void formatReleases_filtersStructuredList() {
        ChangelogFormatter.ReleaseInfo stable =
                new ChangelogFormatter.ReleaseInfo("v2.0", "stable", "", 0, false);
        ChangelogFormatter.ReleaseInfo beta =
                new ChangelogFormatter.ReleaseInfo("v3.0-beta", "beta", "", 0, true);

        String excluded = ChangelogFormatter.formatReleases(Arrays.asList(stable, beta), false);
        assertTrue(excluded.contains("v2.0"));
        assertFalse(excluded.contains("v3.0-beta"));

        String included = ChangelogFormatter.formatReleases(Arrays.asList(stable, beta), true);
        assertTrue(included.contains("v2.0"));
        assertTrue(included.contains("v3.0-beta"));
    }

    @Test
    void releaseInfo_fourArgConstructor_defaultsStable() {
        ChangelogFormatter.ReleaseInfo info =
                new ChangelogFormatter.ReleaseInfo("v1.0", "body", "", 0);

        assertFalse(info.prerelease);

        String out = ChangelogFormatter.formatReleases(Collections.singletonList(info), false);
        assertTrue(out.contains("v1.0"));
    }
}
