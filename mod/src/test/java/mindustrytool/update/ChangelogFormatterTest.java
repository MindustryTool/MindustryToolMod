package mindustrytool.update;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import mindustrytool.services.update.ChangelogFormatter;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class ChangelogFormatterTest extends ArcTestEnv {

    @Test
    void renderMarkdown_link() {
        String out = ChangelogFormatter.renderMarkdown("[text](http://example.com)");
        assertEquals("[sky]text[white]", out);
    }

    @Test
    void renderMarkdown_header() {
        assertEquals("[accent]Header[white]", ChangelogFormatter.renderMarkdown("## Header"));
        assertEquals("[accent]Title[white]", ChangelogFormatter.renderMarkdown("# Title"));
    }

    @Test
    void renderMarkdown_list() {
        assertEquals("• item", ChangelogFormatter.renderMarkdown("- item"));
        assertEquals("• item", ChangelogFormatter.renderMarkdown("* item"));
        assertEquals("• item", ChangelogFormatter.renderMarkdown("  - item"));
    }

    @Test
    void renderMarkdown_bold() {
        assertEquals("[white]bold[white]", ChangelogFormatter.renderMarkdown("**bold**"));
    }

    @Test
    void renderMarkdown_italic() {
        assertEquals("[lightgray]italic[white]", ChangelogFormatter.renderMarkdown("*italic*"));
    }

    @Test
    void renderMarkdown_code() {
        assertEquals("[cyan]code[white]", ChangelogFormatter.renderMarkdown("`code`"));
    }

    @Test
    void renderMarkdown_combined() {
        String input = "**bold** and *italic* and `code` and [link](http://a)";
        String out = ChangelogFormatter.renderMarkdown(input);
        assertTrue(out.contains("[white]bold[white]"));
        assertTrue(out.contains("[lightgray]italic[white]"));
        assertTrue(out.contains("[cyan]code[white]"));
        assertTrue(out.contains("[sky]link[white]"));
    }

    @Test
    void renderMarkdown_null_returnsEmpty() {
        assertEquals("", ChangelogFormatter.renderMarkdown(null));
    }

    @Test
    void formatReleases_capsAt20() {
        List<ChangelogFormatter.ReleaseInfo> list = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            list.add(new ChangelogFormatter.ReleaseInfo("v" + i, "body " + i, "", i));
        }
        String out = ChangelogFormatter.formatReleases(list);
        // Should contain v0..v19 but not v20+
        assertTrue(out.contains("[accent]v0[white]"));
        assertTrue(out.contains("[accent]v19[white]"));
        assertFalse(out.contains("[accent]v20[white]"));
        assertFalse(out.contains("[accent]v24[white]"));
    }

    @Test
    void format_jsonParsing() {
        String json = "[{\"tag_name\":\"v1.0\",\"body\":\"hello\",\"published_at\":\"2024-01-01T12:00:00Z\",\"assets\":[{\"download_count\":3},{\"download_count\":2}]}]";
        String out = ChangelogFormatter.format(json);
        assertTrue(out.contains("[accent]v1.0[white]"));
        assertTrue(out.contains("5")); // 3+2
    }

    @Test
    void format_nullJson_returnsError() {
        String out = ChangelogFormatter.format((String) null);
        assertTrue(out.contains("Could not parse"));
    }

    @Test
    void format_withPublishedAt_formatsDateWithoutError() {
        String json = "[{\"tag_name\":\"v1.0\",\"body\":\"hello\",\"published_at\":\"2024-01-01T12:00:00Z\",\"assets\":[]}]";
        String out = ChangelogFormatter.format(json);
        assertTrue(out.contains("[accent]v1.0[white]"));
        assertTrue(out.contains("[lightgray]"));
    }

    @Test
    void partition_isolatesTargetReleaseFromRecentBeta() {
        String json = "["
                + "{\"tag_name\":\"v5.3.0-v8-beta\",\"body\":\"beta preview\",\"published_at\":\"\",\"assets\":[],\"prerelease\":true},"
                + "{\"tag_name\":\"v5.2.9-v8\",\"body\":\"stable release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false},"
                + "{\"tag_name\":\"v5.2.8-v8\",\"body\":\"older release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false}"
                + "]";

        ChangelogFormatter.PartitionedReleases partitioned = ChangelogFormatter.partition(json, "5.2.9", true);

        assertNotNull(partitioned.targetRelease);
        assertEquals("v5.2.9-v8", partitioned.targetRelease.tagName);
        assertEquals(2, partitioned.otherReleases.size());
        assertEquals("v5.3.0-v8-beta", partitioned.otherReleases.get(0).tagName);
        assertEquals("v5.2.8-v8", partitioned.otherReleases.get(1).tagName);
    }

    @Test
    void partition_matchesExactTag() {
        String json = "["
                + "{\"tag_name\":\"v5.2.9-v8-beta\",\"body\":\"beta release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":true},"
                + "{\"tag_name\":\"v5.2.8-v8\",\"body\":\"stable release\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false}"
                + "]";

        ChangelogFormatter.PartitionedReleases partitioned = ChangelogFormatter.partition(json, "v5.2.9-v8-beta", true);

        assertNotNull(partitioned.targetRelease);
        assertEquals("v5.2.9-v8-beta", partitioned.targetRelease.tagName);
        assertEquals(1, partitioned.otherReleases.size());
        assertEquals("v5.2.8-v8", partitioned.otherReleases.get(0).tagName);
    }

    @Test
    void partition_unmatchedTarget_leavesAllInOthers() {
        String json = "["
                + "{\"tag_name\":\"v1.0\",\"body\":\"first\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false}"
                + "]";

        ChangelogFormatter.PartitionedReleases partitioned = ChangelogFormatter.partition(json, "99.99", true);

        assertNull(partitioned.targetRelease);
        assertEquals(1, partitioned.otherReleases.size());
        assertEquals("v1.0", partitioned.otherReleases.get(0).tagName);
    }

    @Test
    void partition_singleReleaseTarget() {
        String json = "["
                + "{\"tag_name\":\"v1.0\",\"body\":\"first\",\"published_at\":\"\",\"assets\":[],\"prerelease\":false}"
                + "]";

        ChangelogFormatter.PartitionedReleases partitioned = ChangelogFormatter.partition(json, "v1.0", true);

        assertNotNull(partitioned.targetRelease);
        assertEquals("v1.0", partitioned.targetRelease.tagName);
        assertTrue(partitioned.otherReleases.isEmpty());
    }

    @Test
    void partition_nullOrEmptyInput_returnsEmpty() {
        ChangelogFormatter.PartitionedReleases p1 = ChangelogFormatter.partition(null, "v1.0", true);
        assertNull(p1.targetRelease);
        assertTrue(p1.otherReleases.isEmpty());

        ChangelogFormatter.PartitionedReleases p2 = ChangelogFormatter.partition("   ", "v1.0", true);
        assertNull(p2.targetRelease);
        assertTrue(p2.otherReleases.isEmpty());
    }

    @Test
    void formatTargetRelease_includesGreenDot() {
        ChangelogFormatter.ReleaseInfo info = new ChangelogFormatter.ReleaseInfo("v5.2.9", "fixes", "", 10);
        String out = ChangelogFormatter.formatTargetRelease(info);

        assertTrue(out.contains("[green]● [accent]v5.2.9[white]"));
        assertTrue(out.contains("Download count: 10"));
        assertTrue(out.contains("fixes"));
    }

    @Test
    void renderMarkdown_commitKeywords_basic() {
        assertEquals("[#82C341]feat[white]: new feature", ChangelogFormatter.renderMarkdown("feat: new feature"));
        assertEquals("[#EF4444]fix[white]: bug fix", ChangelogFormatter.renderMarkdown("fix: bug fix"));
        assertEquals("[#FAA31B]perf[white]: speedup", ChangelogFormatter.renderMarkdown("perf: speedup"));
        assertEquals("[#88C6ED]refactor[white]: cleanup", ChangelogFormatter.renderMarkdown("refactor: cleanup"));
        assertEquals("[#38BDF8]docs[white]: update readme", ChangelogFormatter.renderMarkdown("docs: update readme"));
        assertEquals("[#F472B6]test[white]: add unit test", ChangelogFormatter.renderMarkdown("test: add unit test"));
        assertEquals("[#C084FC]style[white]: format code", ChangelogFormatter.renderMarkdown("style: format code"));
        assertEquals("[#A1A1AA]chore[white]: bump version", ChangelogFormatter.renderMarkdown("chore: bump version"));
    }

    @Test
    void renderMarkdown_commitKeywords_withScope() {
        assertEquals("[#EF4444]fix[lightgray](chat)[white]: resolve overflow",
                ChangelogFormatter.renderMarkdown("fix(chat): resolve overflow"));
        assertEquals("[#82C341]feat[lightgray](dialog/update)[white]: add target header",
                ChangelogFormatter.renderMarkdown("feat(dialog/update): add target header"));
    }

    @Test
    void renderMarkdown_commitKeywords_withBreakingIndicator() {
        assertEquals("[#82C341]feat[crimson]![white]: breaking api change",
                ChangelogFormatter.renderMarkdown("feat!: breaking api change"));
        assertEquals("[#A1A1AA]chore[lightgray](deps)[crimson]![white]: drop java 7",
                ChangelogFormatter.renderMarkdown("chore(deps)!: drop java 7"));
    }

    @Test
    void renderMarkdown_commitKeywords_afterBulletList() {
        assertEquals("• [#82C341]feat[white]: new item",
                ChangelogFormatter.renderMarkdown("- feat: new item"));
        assertEquals("• [#EF4444]fix[lightgray](core)[white]: fixed error",
                ChangelogFormatter.renderMarkdown("* fix(core): fixed error"));
    }

    @Test
    void renderMarkdown_commitKeywords_avoidsFalsePositivesInProse() {
        String prose = "We need a fix: it is urgent.";
        assertEquals("We need a fix: it is urgent.", ChangelogFormatter.renderMarkdown(prose));
    }
}
