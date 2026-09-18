package mindustrytool.update;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import mindustrytool.services.update.ChangelogFormatter;
import org.junit.jupiter.api.Test;

class ChangelogFormatterTest {

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
}
