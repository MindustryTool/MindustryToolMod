package mindustrytool.core.markdown;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reusable Markdown renderer for Mindustry UI.
 * Supports:
 * - Headers (# ## ###)
 * - Bold (**text**)
 * - Italic (*text* or _text_)
 * - Code (`code`)
 * - Lists (- item or * item)
 * - Images (![alt](url))
 * - Links [text](url) -> clickable
 * - Blockquotes (> text)
 * - Horizontal rules (---)
 * - Emoji shortcuts
 * 
 * Usage:
 * MarkdownRenderer renderer = new MarkdownRenderer();
 * renderer.render(parentTable, markdownText);
 */
public class MarkdownRenderer {

    // Image cache to avoid re-downloading
    private static final ObjectMap<String, TextureRegion> imageCache = new ObjectMap<>();
    private static final ObjectMap<String, Seq<Runnable>> pendingCallbacks = new ObjectMap<>();

    // Patterns
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HR_PATTERN = Pattern.compile("^-{3,}$|^\\*{3,}$|^_{3,}$");

    // Configuration
    private float maxImageWidth = 400f;
    private float maxImageHeight = 300f;
    private float contentWidth = 500f;

    // State for tables
    private Seq<String> tableBuffer = new Seq<>();

    public MarkdownRenderer() {
    }

    public MarkdownRenderer setMaxImageSize(float width, float height) {
        this.maxImageWidth = width;
        this.maxImageHeight = height;
        return this;
    }

    public MarkdownRenderer setContentWidth(float width) {
        this.contentWidth = width;
        return this;
    }

    /**
     * Render markdown text into a Table.
     * 
     * @param parent   The table to add elements to
     * @param markdown The markdown text to render
     */
    public void render(Table parent, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            parent.add("[gray]No content.[]").left().row();
            return;
        }

        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        StringBuilder codeBlockContent = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            // Handle code blocks (```)
            if (trimmed.startsWith("```")) {
                if (!tableBuffer.isEmpty()) {
                    renderTable(parent);
                }

                if (inCodeBlock) {
                    // End code block
                    renderCodeBlock(parent, codeBlockContent.toString());
                    codeBlockContent = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    // Start code block
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n");
                continue;
            }

            // Check for alignment marker (custom protocol from ModInfoDialog)
            boolean centered = false;
            // Handle :::center prefix on same line (e.g. ":::center # Header")
            if (trimmed.startsWith(":::center")) {
                centered = true;
                trimmed = trimmed.substring(9).trim();
                if (trimmed.isEmpty())
                    continue; // Just a marker line
            }

            // Table detection
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                tableBuffer.add(trimmed);
                continue;
            } else if (!tableBuffer.isEmpty()) {
                // End of table block
                renderTable(parent);
            }

            // Empty line = spacing
            if (trimmed.isEmpty()) {
                parent.add("").height(10f).row();
                continue;
            }

            // Horizontal rule
            if (HR_PATTERN.matcher(trimmed).matches()) {
                parent.image().height(2f).color(Color.darkGray).growX().pad(5f).row();
                continue;
            }

            // Headers
            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmed);
            if (headerMatcher.matches()) {
                int level = headerMatcher.group(1).length();
                String headerText = headerMatcher.group(2);
                renderHeader(parent, headerText, level, centered);
                continue;
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                String quoteText = trimmed.substring(1).trim();
                renderBlockquote(parent, quoteText);
                continue;
            }

            // List items
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String itemText = trimmed.substring(2);
                renderListItem(parent, itemText);
                continue;
            }

            // Numbered list
            if (trimmed.matches("^\\d+\\.\\s+.+")) {
                String itemText = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                String number = trimmed.split("\\.")[0];
                renderNumberedItem(parent, itemText, number);
                continue;
            }

            // Linked Images: [![alt](src)](href)
            // We need a specific check here because standard link or image patterns usually
            // consume parts of it
            if (trimmed.startsWith("[![") && trimmed.contains(")](")) {
                if (renderLinkedImageLine(parent, trimmed, centered))
                    continue;
            }

            // Regular paragraph - may contain images, links, formatting
            renderParagraph(parent, trimmed, centered);
        }

        // Flush remaining table buffer
        if (!tableBuffer.isEmpty()) {
            renderTable(parent);
        }
    }

    private void renderTable(Table parent) {
        if (tableBuffer.isEmpty())
            return;

        Table table = new Table();
        table.setBackground(Core.scene.getStyle(Button.ButtonStyle.class).up); // Use a background to distinguish

        // Assume first row is header
        String headerRow = tableBuffer.first();
        String[] headers = parseTableRow(headerRow);

        // Check if second row is separator (ignore content, but could use for alignment
        // parsing later)
        int startIndex = 1;
        if (tableBuffer.size > 1 && tableBuffer.get(1).matches(".*\\|-+\\|.*")) {
            startIndex = 2;
        }

        // Render headers
        for (String header : headers) {
            table.add(processInlineFormatting(header.trim()))
                    .color(Pal.accent)
                    .pad(5f)
                    .center()
                    .fontScale(1f);
            table.image().width(2f).color(Color.darkGray).growY(); // Vertical separator placeholder
        }
        table.row();

        table.image().height(2f).color(Pal.accent).growX().colspan(headers.length * 2).row();

        // Render rows
        for (int i = startIndex; i < tableBuffer.size; i++) {
            String[] cells = parseTableRow(tableBuffer.get(i));
            for (int j = 0; j < headers.length; j++) {
                String cellText = (j < cells.length) ? cells[j] : "";
                table.add(processInlineFormatting(cellText.trim()))
                        .pad(5f)
                        .left() // Default to left align for data
                        .wrap()
                        .width(contentWidth / headers.length - 10f); // Distribute width approx

                if (j < headers.length - 1) {
                    table.image().width(1f).color(Color.darkGray).growY().pad(2f);
                } else {
                    table.add(); // Empty placeholder for consistency
                }
            }
            table.row();
            // Row separator
            if (i < tableBuffer.size - 1) {
                table.image().height(1f).color(Color.darkGray).growX().colspan(headers.length * 2).pad(2f).row();
            }
        }

        // Wrap table in a horizontal scroll pane for wide tables
        ScrollPane scrollPane = new ScrollPane(table, Styles.smallPane);
        scrollPane.setScrollingDisabled(false, true); // Enable horizontal scroll only
        scrollPane.setFadeScrollBars(false);

        parent.add(scrollPane).width(contentWidth).pad(5f).row();
        tableBuffer.clear();
    }

    private String[] parseTableRow(String row) {
        // Remove leading/trailing pipes
        String clean = row.trim();
        if (clean.startsWith("|"))
            clean = clean.substring(1);
        if (clean.endsWith("|"))
            clean = clean.substring(0, clean.length() - 1);
        return clean.split("\\|");
    }

    private void renderHeader(Table parent, String text, int level, boolean centered) {
        text = convertEmoji(text);
        float scale = 1.5f - (level - 1) * 0.15f; // h1=1.5, h2=1.35, h3=1.2, etc.
        Color color = level <= 2 ? Pal.accent : Color.lightGray;

        // Check if header contains a link [text](url)
        Matcher linkMatcher = LINK_PATTERN.matcher(text);
        if (linkMatcher.matches()) {
            String linkText = linkMatcher.group(1);
            String url = linkMatcher.group(2);

            TextButton.TextButtonStyle style = Styles.cleart;
            parent.button(linkText, style, () -> {
                if (url.startsWith("http"))
                    Core.app.openURI(url);
            }).with(b -> {
                b.getLabel().setFontScale(scale);
                b.getLabel().setColor(color);
                b.getLabel().setAlignment(centered ? arc.util.Align.center : arc.util.Align.left);
            }).padTop(10f).padBottom(5f).growX().row();
        } else {
            arc.scene.ui.layout.Cell<Label> cell = parent.add(text).fontScale(scale).color(color);
            if (centered)
                cell.center();
            else
                cell.left();

            cell.padTop(10f).padBottom(5f).row();
        }

        // Add underline for h1 and h2
        if (level <= 2) {
            parent.image().height(1f).color(Color.darkGray).growX().padBottom(5f).row();
        }
    }

    private void renderBlockquote(Table parent, String text) {
        final String formatted = processInlineFormatting(text);
        final float width = contentWidth - 20f;
        parent.table(t -> {
            t.setBackground(Core.scene.getStyle(Button.ButtonStyle.class).over);
            t.image().width(4f).color(Pal.accent).growY().padRight(10f);
            t.add(formatted).wrap().width(width).color(Color.lightGray).left();
        }).growX().pad(5f).row();
    }

    private void renderListItem(Table parent, String text) {
        final String formatted = processInlineFormatting(text);
        final float width = contentWidth - 30f;
        parent.table(t -> {
            t.add("[accent]•[] ").padRight(5f).top();
            t.add(formatted).wrap().width(width).left();
        }).left().padLeft(10f).row();
    }

    private void renderNumberedItem(Table parent, String text, String number) {
        final String formatted = processInlineFormatting(text);
        final float width = contentWidth - 40f;
        parent.table(t -> {
            t.add("[accent]" + number + ".[] ").padRight(5f).top();
            t.add(formatted).wrap().width(width).left();
        }).left().padLeft(10f).row();
    }

    private void renderCodeBlock(Table parent, String code) {
        Table codeTable = new Table();
        codeTable.setBackground(Core.scene.getStyle(Button.ButtonStyle.class).over);
        codeTable.add(code).fontScale(0.9f).color(Color.lightGray).left().pad(10f);

        // Wrap in horizontal scroll pane for long lines
        ScrollPane scrollPane = new ScrollPane(codeTable, Styles.smallPane);
        scrollPane.setScrollingDisabled(false, true); // Enable horizontal scroll only
        scrollPane.setFadeScrollBars(false);

        parent.add(scrollPane).width(contentWidth).pad(5f).row();
    }

    private static final Pattern LINKED_IMAGE_PATTERN = Pattern.compile("\\[!\\[(.*?)\\]\\((.*?)\\)\\]\\((.*?)\\)");

    private boolean renderLinkedImageLine(Table parent, String line, boolean centered) {
        // Now handled inside renderParagraph for better flow, or strict line check here
        // If the line is JUST linked images, we can handle it here cleanly.
        // Let's use a strict check for "only linked images" to keep it simpler
        if (LINKED_IMAGE_PATTERN.matcher(line).find()) {
            // Check if it's mostly linked images?
            // Actually, let's just let renderParagraph handle it to be unified
            // BUT main loop calls this. Let's return false and let renderParagraph take
            // over?
            // OR implement strict check:
            String stripped = line.replaceAll("\\[!\\[(.*?)\\]\\((.*?)\\)\\]\\((.*?)\\)", "").trim();
            if (stripped.isEmpty()) {
                renderLineWithLinkedImages(parent, line, centered);
                return true;
            }
        }
        return false;
    }

    private arc.scene.Element renderLinkedImage(String src, String href, String alt) {
        Table container = new Table();
        Button btn = new Button(Styles.flatt); // Transparent button
        btn.clicked(() -> Core.app.openURI(href));

        renderImageInto(btn, src, alt);

        container.add(btn).pad(5f);
        return container;
    }

    private void renderImageInto(Table container, String url, String altText) {
        if (url.contains("img.shields.io") && !url.contains(".png")) {
            if (url.contains("?"))
                url = url.replaceFirst("\\?", ".png?");
            else
                url = url + ".png";
        }

        // SVG handling: Mindustry doesn't support SVGs natively.
        // If it's explicitly an SVG URL and not converted to PNG above, show
        // placeholder.
        if (url.endsWith(".svg") || url.contains("/svg") || url.contains("type=svg")) {
            container.add("[gray][SVG not supported][]").pad(10f);
            return;
        }

        final String finalUrl = url;

        // Placeholder
        container.add("[gray]Loading...[]").pad(10f);

        if (imageCache.containsKey(finalUrl)) {
            Core.app.post(() -> {
                container.clearChildren();
                addImageToContainer(container, imageCache.get(finalUrl), altText);
            });
            return;
        }

        if (pendingCallbacks.containsKey(finalUrl)) {
            pendingCallbacks.get(finalUrl).add(() -> {
                container.clearChildren();
                if (imageCache.containsKey(finalUrl)) {
                    addImageToContainer(container, imageCache.get(finalUrl), altText);
                } else {
                    container.add("[scarlet]X[]");
                }
            });
            return;
        }

        pendingCallbacks.put(finalUrl, new Seq<>());
        pendingCallbacks.get(finalUrl).add(() -> {
            container.clearChildren();
            if (imageCache.containsKey(finalUrl)) {
                addImageToContainer(container, imageCache.get(finalUrl), altText);
            } else {
                container.add("[scarlet]X[]");
            }
        });

        Http.get(finalUrl, response -> {
            try {
                byte[] bytes = response.getResult();
                // Pixmap creation can fail if bytes are invalid, better do it on thread but
                // Texture MUST be main thread
                // Actually Pixmap is native heap, safe on thread? Yes usually.
                // But Texture upload is definitely not.

                Core.app.post(() -> {
                    try {
                        Pixmap pixmap = new Pixmap(bytes);
                        Texture texture = new Texture(pixmap);
                        TextureRegion region = new TextureRegion(texture);
                        pixmap.dispose();

                        imageCache.put(finalUrl, region);

                        if (pendingCallbacks.containsKey(finalUrl)) {
                            for (Runnable callback : pendingCallbacks.get(finalUrl)) {
                                callback.run();
                            }
                            pendingCallbacks.remove(finalUrl);
                        }
                    } catch (Exception e) {
                        Log.err("Failed to load image from URL: @", finalUrl);
                        if (pendingCallbacks.containsKey(finalUrl)) {
                            for (Runnable callback : pendingCallbacks.get(finalUrl)) {
                                callback.run();
                            }
                            pendingCallbacks.remove(finalUrl);
                        }
                    }
                });
            } catch (Exception e) {
                Core.app.post(() -> {
                    if (pendingCallbacks.containsKey(finalUrl)) {
                        for (Runnable callback : pendingCallbacks.get(finalUrl)) {
                            callback.run();
                        }
                        pendingCallbacks.remove(finalUrl);
                    }
                });
            }
        }, error -> {
            Core.app.post(() -> {
                if (pendingCallbacks.containsKey(finalUrl)) {
                    for (Runnable callback : pendingCallbacks.get(finalUrl)) {
                        callback.run();
                    }
                    pendingCallbacks.remove(finalUrl);
                }
            });
        });
    }

    private arc.scene.Element renderImage(String url, String altText) {
        Table t = new Table();
        renderImageInto(t, url, altText);
        return t;
    }

    private void renderLineWithLinkedImages(Table parent, String line, boolean centered) {
        Seq<arc.scene.Element> items = new Seq<>();

        Matcher matcher = LINKED_IMAGE_PATTERN.matcher(line);
        while (matcher.find()) {
            String alt = matcher.group(1);
            String src = matcher.group(2);
            String href = matcher.group(3);

            items.add(renderLinkedImage(src, href, alt));
        }

        layoutFlow(parent, items, centered);
    }

    private void layoutFlow(Table parent, Seq<arc.scene.Element> items, boolean centered) {
        Table currentRow = new Table();
        // currentRow.setBackground(Styles.black6); // Debug background
        float currentW = 0f;
        // Use a bit less than contentWidth to be safe
        float maxW = contentWidth - 10f;

        for (arc.scene.Element item : items) {
            float itemW = item.getPrefWidth();
            // If item has no pref width yet (e.g. unloaded image), assume a default
            if (itemW < 10f)
                itemW = 100f;

            // Wrap if current width + item width exceeds max
            if (currentW + itemW > maxW && currentW > 0) {
                arc.scene.ui.layout.Cell cell = parent.add(currentRow);
                if (centered)
                    cell.center();
                else
                    cell.left();
                cell.row();

                currentRow = new Table();
                currentW = 0f;
            }

            currentRow.add(item).pad(2f);
            currentW += itemW + 4f;
        }

        // Add final row
        if (currentRow.getChildren().size > 0) {
            arc.scene.ui.layout.Cell cell = parent.add(currentRow);
            if (centered)
                cell.center();
            else
                cell.left();
            cell.row();
        }
    }

    private void renderParagraph(Table parent, String line, boolean centered) {
        // Check for linked images first (badges)
        Matcher linkedMatcher = LINKED_IMAGE_PATTERN.matcher(line);
        if (linkedMatcher.find()) {
            renderLineWithLinkedImages(parent, line, centered);
            return;
        }

        // Check for images
        Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
        if (imageMatcher.find()) {
            renderLineWithImages(parent, line, centered);
            return;
        }

        // Check for links
        Matcher linkMatcher = LINK_PATTERN.matcher(line);
        if (linkMatcher.find()) {
            renderLineWithLinks(parent, line, centered);
            return;
        }

        // Regular text
        String formatted = processInlineFormatting(line);
        arc.scene.ui.layout.Cell<Label> cell = parent.add(formatted).wrap().width(contentWidth);
        if (centered)
            cell.center();
        else
            cell.left();
        cell.row();
    }

    private void renderLineWithLinks(Table parent, String line, boolean centered) {
        Seq<arc.scene.Element> items = new Seq<>();

        Matcher matcher = LINK_PATTERN.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            // Text before - add as label
            if (matcher.start() > lastEnd) {
                String textBefore = line.substring(lastEnd, matcher.start());
                if (!textBefore.isEmpty()) {
                    String formatted = processInlineFormatting(textBefore);
                    items.add(new Label(formatted));
                }
            }

            // The link - add as button
            String text = matcher.group(1);
            String url = matcher.group(2);

            TextButton.TextButtonStyle style = Styles.cleart;
            text = "[sky]" + text + "[]";

            String finalUrl = url;
            TextButton btn = new TextButton(text, style);
            btn.clicked(() -> {
                if (finalUrl.startsWith("http"))
                    Core.app.openURI(finalUrl);
            });
            items.add(btn);

            lastEnd = matcher.end();
        }

        // Text after
        if (lastEnd < line.length()) {
            String textAfter = line.substring(lastEnd);
            items.add(new Label(processInlineFormatting(textAfter)));
        }

        layoutFlow(parent, items, centered);
    }

    private void renderLineWithImages(Table parent, String line, boolean centered) {
        Seq<arc.scene.Element> items = new Seq<>();

        Matcher matcher = IMAGE_PATTERN.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            // Text before image
            if (matcher.start() > lastEnd) {
                String textBefore = line.substring(lastEnd, matcher.start());
                if (!textBefore.trim().isEmpty()) {
                    items.add(new Label(processInlineFormatting(textBefore)));
                }
            }

            // Render image
            String altText = matcher.group(1);
            String imageUrl = matcher.group(2);
            items.add(renderImage(imageUrl, altText));

            lastEnd = matcher.end();
        }

        // Text after last image
        if (lastEnd < line.length()) {
            String textAfter = line.substring(lastEnd);
            if (!textAfter.trim().isEmpty()) {
                items.add(new Label(processInlineFormatting(textAfter)));
            }
        }

        layoutFlow(parent, items, centered);
    }

    private void addImageToContainer(Table container, TextureRegion region, String altText) {
        float w = region.width;
        float h = region.height;

        // Scale down if too large
        if (w > maxImageWidth) {
            float ratio = maxImageWidth / w;
            w = maxImageWidth;
            h *= ratio;
        }
        if (h > maxImageHeight) {
            float ratio = maxImageHeight / h;
            h = maxImageHeight;
            w *= ratio;
        }

        final float finalW = w;
        final float finalH = h;
        container.table(t -> {
            Image img = new Image(new TextureRegionDrawable(region));
            t.add(img).size(finalW, finalH);
        });
    }

    private String processInlineFormatting(String text) {
        // Bold **text** -> [accent]text[]
        text = BOLD_PATTERN.matcher(text).replaceAll("[accent]$1[]");

        // Italic *text* -> [lightgray]text[]
        text = ITALIC_PATTERN.matcher(text).replaceAll("[lightgray]$1[]");

        // Code `text` -> [gray]text[]
        text = CODE_PATTERN.matcher(text).replaceAll("[gray]$1[]");

        // Links [text](url) -> [sky]text[] (Fallback if processed as simple text)
        // If we are in processInlineFormatting, it means renderParagraph didn't catch
        // it as a "LineWithLinks"
        // or it's inside a table cell.
        // For table cells, we keep it as text but colored.
        text = LINK_PATTERN.matcher(text).replaceAll("[sky]$1[]");

        // PR/Issue references (#123)
        text = text.replaceAll("#(\\d+)", "[accent]#$1[]");

        // Username mentions (@user)
        text = text.replaceAll("@(\\w+)", "[sky]@$1[]");

        // Emojis
        text = convertEmoji(text);

        return text;
    }

    private String convertEmoji(String text) {
        // Common changelog emojis
        text = text.replace(":rocket:", "🚀");
        text = text.replace(":bug:", "🐛");
        text = text.replace(":sparkles:", "✨");
        text = text.replace(":memo:", "📝");
        text = text.replace(":art:", "🎨");
        text = text.replace(":zap:", "⚡");
        text = text.replace(":fire:", "🔥");
        text = text.replace(":hammer:", "🔨");
        text = text.replace(":wrench:", "🔧");
        text = text.replace(":package:", "📦");
        text = text.replace(":lock:", "🔒");
        text = text.replace(":arrow_up:", "⬆️");
        text = text.replace(":arrow_down:", "⬇️");
        text = text.replace(":white_check_mark:", "✅");
        text = text.replace(":x:", "❌");
        text = text.replace(":warning:", "⚠️");
        text = text.replace(":boom:", "💥");
        text = text.replace(":tada:", "🎉");
        text = text.replace(":construction:", "🚧");
        text = text.replace(":recycle:", "♻️");
        text = text.replace(":heavy_plus_sign:", "➕");
        text = text.replace(":heavy_minus_sign:", "➖");
        text = text.replace(":star:", "⭐");
        text = text.replace(":heart:", "❤️");
        text = text.replace(":eyes:", "👀");
        text = text.replace(":thumbsup:", "👍");
        text = text.replace(":thumbsdown:", "👎");
        text = text.replace(":100:", "💯");
        text = text.replace(":arrow_right:", "➡️");
        text = text.replace(":arrow_left:", "⬅️");
        text = text.replace(":new:", "🆕");
        text = text.replace(":up:", "🆙");
        text = text.replace(":cool:", "🆒");
        text = text.replace(":ok:", "🆗");
        text = text.replace(":sos:", "🆘");
        // Additional flags/content
        text = text.replace(":globe_with_meridians:", "🌍");
        text = text.replace(":us:", "🇺🇸");
        text = text.replace(":vn:", "🇻🇳");
        text = text.replace(":cn:", "🇨🇳");
        text = text.replace(":kr:", "🇰🇷");
        text = text.replace(":jp:", "🇯🇵");
        text = text.replace(":ru:", "🇷🇺");
        text = text.replace(":de:", "🇩🇪");
        text = text.replace(":fr:", "🇫🇷");
        text = text.replace(":es:", "🇪🇸");
        text = text.replace(":br:", "🇧🇷");
        text = text.replace(":pt:", "🇵🇹");
        text = text.replace(":it:", "🇮🇹");
        text = text.replace(":pl:", "🇵🇱");

        return text;
    }

    /**
     * Clear the image cache to free memory
     */
    public static void clearCache() {
        for (TextureRegion region : imageCache.values()) {
            if (region.texture != null) {
                region.texture.dispose();
            }
        }
        imageCache.clear();
    }
}
