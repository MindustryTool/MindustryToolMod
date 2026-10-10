package mindustrytool.features.browser.patch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HjsonHighlighterTest {

    @Test
    void testFormatValidJson() {
        String json = "{\"name\":\"test\",\"count\":42,\"enabled\":true}";
        String formatted = HjsonHighlighter.format(json);
        assertNotNull(formatted);
        assertTrue(formatted.contains("name: test") || formatted.contains("name: \"test\""));
        assertTrue(formatted.contains("count: 42"));
        assertTrue(formatted.contains("enabled: true"));
    }

    @Test
    void testFormatFallbackOnInvalidJson() {
        String broken = "{\n  unclosed: [ 1, 2,";
        String formatted = HjsonHighlighter.format(broken);
        assertEquals(broken, formatted);
    }

    @Test
    void testHighlightTokens() {
        String hjson = "name: \"my-patch\"\ncount: 10\nenabled: true\n# comment";
        String highlighted = HjsonHighlighter.highlight(hjson);

        assertTrue(highlighted.contains("[accent]name[]"));
        assertTrue(highlighted.contains("[accent]count[]"));
        assertTrue(highlighted.contains("[accent]enabled[]"));

        assertTrue(highlighted.contains("[green]\"my-patch\"[]"));

        assertTrue(highlighted.contains("[stat]10[]"));

        assertTrue(highlighted.contains("[coral]true[]"));

        assertTrue(highlighted.contains("[gray]# comment[]"));
    }

    @Test
    void testEscapesSquareBrackets() {
        String text = "tags: [\"foo\", \"bar\"]";
        String highlighted = HjsonHighlighter.highlight(text);
        assertTrue(highlighted.contains("[lightgray][[[]"));
        assertTrue(highlighted.contains("[lightgray]][]"));
    }

    @Test
    void testBuildLineNumbers() {
        String multiLine = "line 1\nline 2\nline 3";
        String lineNumbers = HjsonHighlighter.buildLineNumbers(multiLine);
        assertEquals("1\n2\n3", lineNumbers);

        assertEquals("1", HjsonHighlighter.buildLineNumbers(""));
        assertEquals("1", HjsonHighlighter.buildLineNumbers("single line"));
    }
}
