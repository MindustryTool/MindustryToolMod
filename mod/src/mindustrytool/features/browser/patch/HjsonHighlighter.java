package mindustrytool.features.browser.patch;

import arc.util.Log;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for formatting and syntax highlighting HJSON / JSON patch text
 * using Arc color markup tags.
 */
public final class HjsonHighlighter {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?<COMMENT>(?:#|//).*)|"
                    + "(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|(?:'''[\\s\\S]*?'''))|"
                    + "(?<KEY>[a-zA-Z0-9_$-]+(?=\\s*:))|"
                    + "(?<NUMBER>-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)|"
                    + "(?<BOOLNULL>\\b(?:true|false|null)\\b)|"
                    + "(?<PUNCT>[:{}\\[\\]\\,])"
    );

    private HjsonHighlighter() {
    }

    /**
     * Formats raw HJSON/JSON string with consistent 2-space indentation and clean HJSON syntax.
     * Falls back to raw string if parsing fails.
     */
    public static String format(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            Jval parsed = Jval.read(raw);
            return parsed.toString(Jformat.hjson);
        } catch (Throwable t) {
            Log.debug("Failed to format HJSON patch text: @", t.getMessage());
            return raw;
        }
    }

    /**
     * Highlights HJSON text by wrapping syntax tokens with Arc color markup:
     * - Keys: {@code [accent]}
     * - Strings: {@code [green]}
     * - Numbers: {@code [stat]}
     * - Booleans/Null: {@code [coral]}
     * - Comments: {@code [gray]}
     * - Punctuation: {@code [lightgray]}
     *
     * <p>Any literal {@code '['} characters in the source text are escaped to {@code '[['}
     * to prevent interference with Arc's markup parser.
     */
    public static String highlight(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 128);
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastEnd) {
                appendEscaped(sb, text.substring(lastEnd, start));
            }

            if (matcher.group("COMMENT") != null) {
                sb.append("[gray]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else if (matcher.group("KEY") != null) {
                sb.append("[accent]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else if (matcher.group("STRING") != null) {
                sb.append("[green]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else if (matcher.group("NUMBER") != null) {
                sb.append("[stat]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else if (matcher.group("BOOLNULL") != null) {
                sb.append("[coral]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else if (matcher.group("PUNCT") != null) {
                sb.append("[lightgray]");
                appendEscaped(sb, matcher.group());
                sb.append("[]");
            } else {
                appendEscaped(sb, matcher.group());
            }

            lastEnd = end;
        }

        if (lastEnd < text.length()) {
            appendEscaped(sb, text.substring(lastEnd));
        }

        return sb.toString();
    }

    /**
     * Formats and highlights the given raw HJSON/JSON patch text.
     */
    public static String formatAndHighlight(@Nullable String raw) {
        return highlight(format(raw));
    }

    /**
     * Generates a newline-separated string of line numbers ("1\n2\n3...") matching
     * the number of lines in {@code text}.
     */
    public static String buildLineNumbers(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "1";
        }
        int lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        StringBuilder sb = new StringBuilder(lineCount * 4);
        for (int i = 1; i <= lineCount; i++) {
            sb.append(i);
            if (i < lineCount) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendEscaped(StringBuilder sb, String fragment) {
        for (int i = 0; i < fragment.length(); i++) {
            char c = fragment.charAt(i);
            if (c == '[') {
                sb.append("[[");
            } else {
                sb.append(c);
            }
        }
    }
}
