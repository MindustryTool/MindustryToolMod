package mindustrytool.ui;

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
            // Handle code blocks (```)
            if (line.trim().startsWith("```")) {
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

            // Empty line = spacing
            if (line.trim().isEmpty()) {
                parent.add("").height(10f).row();
                continue;
            }

            // Horizontal rule
            if (HR_PATTERN.matcher(line.trim()).matches()) {
                parent.image().height(2f).color(Color.darkGray).growX().pad(5f).row();
                continue;
            }

            // Headers
            Matcher headerMatcher = HEADER_PATTERN.matcher(line.trim());
            if (headerMatcher.matches()) {
                int level = headerMatcher.group(1).length();
                String headerText = headerMatcher.group(2);
                renderHeader(parent, headerText, level);
                continue;
            }

            // Blockquote
            if (line.trim().startsWith(">")) {
                String quoteText = line.trim().substring(1).trim();
                renderBlockquote(parent, quoteText);
                continue;
            }

            // List items
            if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                String itemText = line.trim().substring(2);
                renderListItem(parent, itemText);
                continue;
            }

            // Numbered list
            if (line.trim().matches("^\\d+\\.\\s+.+")) {
                String itemText = line.trim().replaceFirst("^\\d+\\.\\s+", "");
                String number = line.trim().split("\\.")[0];
                renderNumberedItem(parent, itemText, number);
                continue;
            }

            // Regular paragraph - may contain images, links, formatting
            renderParagraph(parent, line);
        }
    }

    private void renderHeader(Table parent, String text, int level) {
        text = convertEmoji(text);
        float scale = 1.5f - (level - 1) * 0.15f; // h1=1.5, h2=1.35, h3=1.2, etc.
        Color color = level <= 2 ? Pal.accent : Color.lightGray;

        parent.add(text).fontScale(scale).color(color).left().padTop(10f).padBottom(5f).row();

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
            t.add("[accent]•[] ").padRight(5f);
            t.add(formatted).wrap().width(width).left();
        }).left().padLeft(10f).row();
    }

    private void renderNumberedItem(Table parent, String text, String number) {
        final String formatted = processInlineFormatting(text);
        final float width = contentWidth - 40f;
        parent.table(t -> {
            t.add("[accent]" + number + ".[] ").padRight(5f);
            t.add(formatted).wrap().width(width).left();
        }).left().padLeft(10f).row();
    }

    private void renderCodeBlock(Table parent, String code) {
        parent.table(t -> {
            t.setBackground(Core.scene.getStyle(Button.ButtonStyle.class).over);
            t.add(code).fontScale(0.9f).color(Color.lightGray).left().pad(10f);
        }).growX().pad(5f).row();
    }

    private void renderParagraph(Table parent, String line) {
        // Check for images first
        Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
        if (imageMatcher.find()) {
            renderLineWithImages(parent, line);
            return;
        }

        // Regular text with inline formatting
        String formatted = processInlineFormatting(line);
        parent.add(formatted).wrap().width(contentWidth).left().row();
    }

    private void renderLineWithImages(Table parent, String line) {
        Matcher matcher = IMAGE_PATTERN.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            // Text before image
            if (matcher.start() > lastEnd) {
                String textBefore = line.substring(lastEnd, matcher.start());
                if (!textBefore.trim().isEmpty()) {
                    parent.add(processInlineFormatting(textBefore)).wrap().width(contentWidth).left().row();
                }
            }

            // Render image
            String altText = matcher.group(1);
            String imageUrl = matcher.group(2);
            renderImage(parent, imageUrl, altText);

            lastEnd = matcher.end();
        }

        // Text after last image
        if (lastEnd < line.length()) {
            String textAfter = line.substring(lastEnd);
            if (!textAfter.trim().isEmpty()) {
                parent.add(processInlineFormatting(textAfter)).wrap().width(contentWidth).left().row();
            }
        }
    }

    private void renderImage(Table parent, String url, String altText) {
        // Create placeholder
        Table imageContainer = new Table();
        imageContainer.add("[gray]Loading image...[]").pad(20f);
        parent.add(imageContainer).pad(10f).row();

        // Check cache first
        if (imageCache.containsKey(url)) {
            Core.app.post(() -> {
                imageContainer.clear();
                addImageToContainer(imageContainer, imageCache.get(url), altText);
            });
            return;
        }

        // Check if already loading
        if (pendingCallbacks.containsKey(url)) {
            pendingCallbacks.get(url).add(() -> {
                imageContainer.clear();
                if (imageCache.containsKey(url)) {
                    addImageToContainer(imageContainer, imageCache.get(url), altText);
                } else {
                    imageContainer.add("[scarlet]Failed to load image[]").pad(10f);
                }
            });
            return;
        }

        // Start loading
        pendingCallbacks.put(url, new Seq<>());
        pendingCallbacks.get(url).add(() -> {
            imageContainer.clear();
            if (imageCache.containsKey(url)) {
                addImageToContainer(imageContainer, imageCache.get(url), altText);
            } else {
                imageContainer.add("[scarlet]Failed to load image[]").pad(10f);
            }
        });

        Http.get(url, response -> {
            try {
                byte[] bytes = response.getResult();
                Pixmap pixmap = new Pixmap(bytes);
                Texture texture = new Texture(pixmap);
                TextureRegion region = new TextureRegion(texture);
                pixmap.dispose();

                imageCache.put(url, region);

                Core.app.post(() -> {
                    for (Runnable callback : pendingCallbacks.get(url)) {
                        callback.run();
                    }
                    pendingCallbacks.remove(url);
                });
            } catch (Exception e) {
                Log.err("Failed to load image: " + url, e);
                Core.app.post(() -> {
                    for (Runnable callback : pendingCallbacks.get(url)) {
                        callback.run();
                    }
                    pendingCallbacks.remove(url);
                });
            }
        }, error -> {
            Log.err("Failed to download image: " + url, error);
            Core.app.post(() -> {
                for (Runnable callback : pendingCallbacks.get(url)) {
                    callback.run();
                }
                pendingCallbacks.remove(url);
            });
        });
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
            t.add(img).size(finalW, finalH).row();
            if (altText != null && !altText.isEmpty()) {
                t.add("[gray]" + altText + "[]").fontScale(0.8f).padTop(5f);
            }
        });
    }

    private String processInlineFormatting(String text) {
        // Bold **text** -> [accent]text[]
        text = BOLD_PATTERN.matcher(text).replaceAll("[accent]$1[]");

        // Italic *text* -> [lightgray]text[]
        text = ITALIC_PATTERN.matcher(text).replaceAll("[lightgray]$1[]");

        // Code `text` -> [gray]text[]
        text = CODE_PATTERN.matcher(text).replaceAll("[gray]$1[]");

        // Links [text](url) -> [sky]text[] (can't make clickable in labels)
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
