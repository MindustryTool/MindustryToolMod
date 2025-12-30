package mindustrytool.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Dialog that displays the mod's README.md documentation.
 * Uses MarkdownRenderer to display rich content including images, lists, and
 * formatting.
 */
public class ModInfoDialog extends BaseDialog {

    private static final String README_URL = "https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/main/README.md";

    private Table contentTable;
    private boolean loading = true;
    private String errorMessage = null;
    private String cachedReadme = null;

    public ModInfoDialog() {
        super("Mod Info");

        addCloseButton();

        // Add GitHub button
        buttons.button("GitHub", Icon.github, () -> {
            Core.app.openURI("https://github.com/MindustryTool/MindustryToolMod");
        });

        // Add Website button
        buttons.button("Website", Icon.planet, () -> {
            Core.app.openURI("https://mindustry-tool.com");
        });

        // Add Discord button
        buttons.button("Discord", Icon.players, () -> {
            Core.app.openURI("https://discord.gg/nQDrEHVkrt");
        });

        shown(this::onShown);
    }

    private void onShown() {
        if (cachedReadme != null) {
            // Use cached version
            loading = false;
            rebuildUI();
        } else {
            loading = true;
            errorMessage = null;
            rebuildUI();
            fetchReadme();
        }
    }

    private void fetchReadme() {
        Http.get(README_URL, response -> {
            try {
                cachedReadme = response.getResultAsString();

                // Clean up HTML-heavy content for better rendering
                cachedReadme = cleanupForRendering(cachedReadme);

                // Resolve relative paths
                String baseUrl = "https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/main/";
                cachedReadme = resolveRelativePaths(cachedReadme, baseUrl);

                loading = false;
                Core.app.post(this::rebuildUI);
            } catch (Exception e) {
                Log.err("Failed to parse README", e);
                errorMessage = "Failed to parse README: " + e.getMessage();
                loading = false;
                Core.app.post(this::rebuildUI);
            }
        }, error -> {
            Log.err("Failed to fetch README", error);
            errorMessage = "Network error: " + error.getMessage();
            loading = false;
            Core.app.post(this::rebuildUI);
        });
    }

    private String resolveRelativePaths(String content, String baseUrl) {
        // Resolve markdown images: ![alt](path)
        content = content.replaceAll("!\\[([^\\]]*)\\]\\((?!http)([^)]+)\\)", "![$1](" + baseUrl + "$2)");
        // Resolve markdown links (optional, if we want them to point to repo files):
        // [text](path)
        // content = content.replaceAll("\\[([^\\]]+)\\]\\((?!http)([^)]+)\\)", "[$1]("
        // + baseUrl + "$2)");
        return content;
    }

    /**
     * Clean up README content for better rendering in Mindustry UI.
     * Removes all HTML, converts to clean Markdown.
     */
    private String cleanupForRendering(String content) {
        // Remove mermaid code blocks first
        content = content.replaceAll("(?s)```mermaid.*?```", "\n[Diagram - View on GitHub]\n");

        // Replace unsupported forthebadge.com SVGs with shields.io PNG equivalents
        content = content.replace("https://forthebadge.com/images/badges/built-with-love.svg",
                "https://img.shields.io/badge/built%20with-love-important.png?style=for-the-badge");
        content = content.replace("https://forthebadge.com/images/badges/open-source.svg",
                "https://img.shields.io/badge/open-source-success.png?style=for-the-badge");
        content = content.replace("https://forthebadge.com/images/badges/made-with-java.svg",
                "https://img.shields.io/badge/made%20with-java-blue.png?style=for-the-badge");

        // Remove HTML comments
        content = content.replaceAll("<!--.*?-->", "");

        // Handle alignment tags
        // <p align="center">foo</p> -> :::center foo
        // Use DOTALL (?s) to match content spanning multiple lines
        java.util.regex.Pattern pAlignPattern = java.util.regex.Pattern
                .compile("(?is)<p[^>]*align=[\"']center[\"'][^>]*>(.*?)</p>");
        java.util.regex.Matcher pAlignMatcher = pAlignPattern.matcher(content);
        StringBuffer pAlignSb = new StringBuffer();
        while (pAlignMatcher.find()) {
            String innerContent = pAlignMatcher.group(1);
            // Flatten newlines within the block to keep content on one "line" for renderer
            innerContent = innerContent.replaceAll("\\s*\\n\\s*", " ").trim();
            pAlignMatcher.appendReplacement(pAlignSb,
                    "\n:::center " + java.util.regex.Matcher.quoteReplacement(innerContent) + "\n");
        }
        pAlignMatcher.appendTail(pAlignSb);
        content = pAlignSb.toString();

        // Same for div
        java.util.regex.Pattern divAlignPattern = java.util.regex.Pattern
                .compile("(?is)<div[^>]*align=[\"']center[\"'][^>]*>(.*?)</div>");
        java.util.regex.Matcher divAlignMatcher = divAlignPattern.matcher(content);
        StringBuffer divAlignSb = new StringBuffer();
        while (divAlignMatcher.find()) {
            String innerContent = divAlignMatcher.group(1);
            innerContent = innerContent.replaceAll("\\s*\\n\\s*", " ").trim();
            divAlignMatcher.appendReplacement(divAlignSb,
                    "\n:::center " + java.util.regex.Matcher.quoteReplacement(innerContent) + "\n");
        }
        divAlignMatcher.appendTail(divAlignSb);
        content = divAlignSb.toString();

        // Centered headers - loop to handle newlines correctly
        // We iterate 1..6 to handle each header level
        java.util.regex.Pattern headerPattern = java.util.regex.Pattern
                .compile("(?is)<h([1-6])[^>]*align=[\"']center[\"'][^>]*>(.*?)</h\\1>");
        java.util.regex.Matcher headerMatcher = headerPattern.matcher(content);
        StringBuffer headerSb = new StringBuffer();
        while (headerMatcher.find()) {
            String level = headerMatcher.group(1);
            String inner = headerMatcher.group(2);
            // Scrub newlines like we do for paragraphs
            inner = inner.replaceAll("\\s*\\n\\s*", " ").trim();
            // Generate standard markdown header with center marker
            String hashes = new String(new char[Integer.parseInt(level)]).replace("\0", "#");
            headerMatcher.appendReplacement(headerSb,
                    "\n:::center " + hashes + " " + java.util.regex.Matcher.quoteReplacement(inner) + "\n");
        }
        headerMatcher.appendTail(headerSb);
        content = headerSb.toString();

        // Convert <img> tags to Markdown images
        // Enhanced simple regex for attributes
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img([^>]+)>");
        java.util.regex.Matcher imgMatcher = imgPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (imgMatcher.find()) {
            String attributes = imgMatcher.group(1);
            String src = "";
            String alt = "";

            java.util.regex.Matcher srcMatch = java.util.regex.Pattern.compile("src=\"([^\"]+)\"").matcher(attributes);
            if (srcMatch.find())
                src = srcMatch.group(1);

            java.util.regex.Matcher altMatch = java.util.regex.Pattern.compile("alt=\"([^\"]+)\"").matcher(attributes);
            if (altMatch.find())
                alt = altMatch.group(1);

            if (!src.isEmpty()) {
                // Preserve just the image markdown, let renderer handle if it's inside a link
                imgMatcher.appendReplacement(sb, "![" + alt + "](" + src + ")");
            } else {
                imgMatcher.appendReplacement(sb, "");
            }
        }
        imgMatcher.appendTail(sb);
        content = sb.toString();

        // Convert <strong> and <b> to Markdown bold
        content = content.replaceAll("<strong>([^<]*)</strong>", "**$1**");
        content = content.replaceAll("<b>([^<]*)</b>", "**$1**");

        // Convert <em> and <i> to Markdown italic
        content = content.replaceAll("<em>([^<]*)</em>", "*$1*");
        content = content.replaceAll("<i>([^<]*)</i>", "*$1*");

        // Convert <code> to Markdown inline code
        content = content.replaceAll("<code>([^<]*)</code>", "`$1`");

        // Convert HTML links to Markdown - handle multiline properly
        // Note: nesting check is needed for Linked Images: <a href...><img ...></a>
        // If we converted img tags above to ![...](...), then <a href="..."><img
        // ...></a> became <a href="...">![...](...)</a>
        // Now valid markdown for linked image is [![...](...)](...)
        // So we need to ensure the connection remains.

        java.util.regex.Pattern linkPattern = java.util.regex.Pattern
                .compile("(?s)<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>");
        java.util.regex.Matcher linkMatcher = linkPattern.matcher(content);
        StringBuffer linkSb = new StringBuffer();
        while (linkMatcher.find()) {
            String url = linkMatcher.group(1);
            String linkText = linkMatcher.group(2);

            // If linkText contains markdown image ![...](...), we have a linked image!
            // Clean up newlines only if it's text, don't break the image syntax
            if (!linkText.contains("![")) {
                linkText = linkText.replaceAll("\\n", " ").replaceAll("\\r", " ").trim();
                linkText = linkText.replaceAll("\\s+", " ");
            } else {
                linkText = linkText.trim();
            }

            linkMatcher.appendReplacement(linkSb,
                    java.util.regex.Matcher.quoteReplacement("[" + linkText + "](" + url + ")"));
        }
        linkMatcher.appendTail(linkSb);
        content = linkSb.toString();

        // Convert <h1> - <h6> to Markdown headers
        // Handle headers that weren't centered
        content = content.replaceAll("<h1[^>]*>([^<]*)</h1>", "# $1");
        content = content.replaceAll("<h2[^>]*>([^<]*)</h2>", "## $1");
        content = content.replaceAll("<h3[^>]*>([^<]*)</h3>", "### $1");
        content = content.replaceAll("<h4[^>]*>([^<]*)</h4>", "#### $1");
        content = content.replaceAll("<h5[^>]*>([^<]*)</h5>", "##### $1");
        content = content.replaceAll("<h6[^>]*>([^<]*)</h6>", "###### $1");

        // Convert <summary> to bold text
        content = content.replaceAll("<summary>([^<]*)</summary>", "**$1**");

        // Convert <br> tags to newlines
        content = content.replaceAll("<br\\s*/?>", "\n");

        // Remove all remaining HTML tags
        content = content.replaceAll("<[^>]+>", "");

        // Clean up HTML entities
        content = content.replace("&nbsp;", " ");
        content = content.replace("&lt;", "<");
        content = content.replace("&gt;", ">");
        content = content.replace("&amp;", "&");
        content = content.replace("&quot;", "\"");

        // Fix excessive blank lines but preserve ONE blank line for paragraph
        // separation
        content = content.replaceAll("\n{3,}", "\n\n");

        String[] lines = content.split("\n");
        StringBuilder finalSb = new StringBuilder();
        for (String line : lines) {
            // START PATCH: Preserve full line for tables, don't trim if it breaks pipe
            // alignment?
            // Actually trimming is fine for logical pipes, but let's be careful.
            // Also, :::center lines should be trimmed.
            finalSb.append(line.trim()).append("\n");
        }
        content = finalSb.toString();

        // Final cleanups after all conversions
        // Remove contrib.rocks images (converted from HTML or Markdown)
        content = content.replaceAll("!\\[.*?\\]\\(https://contrib\\.rocks/image.*?\\)", "");
        content = content.replaceAll("(?i)<img[^>]*contrib\\.rocks[^>]*>", "");

        // Remove broken shields.io badges that have malformed URLs (e.g. starting with
        // ? or ??)
        // Fix emoji in shields.io URLs - emojis like 🔒 get corrupted to ?
        // Pattern: badge/EMOJI_Label -> badge/Label (remove corrupted emoji prefix)
        content = content.replaceAll("(img\\.shields\\.io/badge/)[^\\-]*_", "$1");

        content = content.replace("https://img.shields.io/badge/??", "https://img.shields.io/badge/");
        content = content.replace("https://img.shields.io/badge/?_", "https://img.shields.io/badge/");
        // Filter out URLs that are likely to fail or cause spam
        content = content.replaceAll("!\\[.*?\\]\\(https://img\\.shields\\.io/badge/\\?_.*?\\)", "");
        content = content.replaceAll("!\\[.*?\\]\\(https://img\\.shields\\.io/badge/\\?\\?_.*?\\)", "");

        return content.trim();
    }

    private void rebuildUI() {
        cont.clear();

        if (loading) {
            cont.add("Loading README...").color(Color.lightGray).pad(50f);
            return;
        }

        if (errorMessage != null) {
            cont.table(t -> {
                t.image(Icon.warning).size(48f).color(Pal.remove).padBottom(10f).row();
                t.add("Failed to load README").color(Pal.remove).padBottom(5f).row();
                t.add(errorMessage).color(Color.gray).wrap().width(400f).padBottom(15f).row();
                t.button("Retry", Icon.refresh, Styles.flatt, () -> {
                    loading = true;
                    errorMessage = null;
                    cachedReadme = null;
                    rebuildUI();
                    fetchReadme();
                }).size(120f, 40f);
            }).pad(30f);
            return;
        }

        // Main content
        contentTable = new Table();
        contentTable.defaults().left().padBottom(5f);

        // Use MarkdownRenderer
        MarkdownRenderer renderer = new MarkdownRenderer()
                .setContentWidth(550f)
                .setMaxImageSize(500f, 350f);
        renderer.render(contentTable, cachedReadme);

        // Scroll pane
        ScrollPane scroll = new ScrollPane(contentTable, Styles.smallPane);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        // Limit height to 80% of screen height and width to look like a dialog
        cont.add(scroll).width(600f).maxHeight(Core.graphics.getHeight() * 0.8f).pad(10f);
    }

    /**
     * Static method to open the dialog.
     */
    public static void open() {
        new ModInfoDialog().show();
    }
}
