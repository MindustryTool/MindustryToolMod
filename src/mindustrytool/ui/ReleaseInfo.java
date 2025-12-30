package mindustrytool.ui;

import arc.struct.Seq;
import arc.util.serialization.Jval;

/**
 * Data class representing a GitHub Release.
 */
public class ReleaseInfo {
    public final String tagName; // e.g., "v2.19.2"
    public final String name; // Release title
    public final String body; // Changelog/description (Markdown)
    public final String publishedAt; // ISO date string
    public final String htmlUrl; // Link to release page
    public final boolean prerelease; // Is beta/dev release
    public final boolean draft;
    public final int downloadCount; // Total downloads across all assets
    public final String jarDownloadUrl; // Direct download URL for the mod JAR

    public ReleaseInfo(Jval json) {
        this.tagName = json.getString("tag_name", "unknown");
        this.name = json.getString("name", tagName);
        this.body = json.getString("body", "");
        this.publishedAt = json.getString("published_at", "");
        this.htmlUrl = json.getString("html_url", "");
        this.prerelease = json.getBool("prerelease", false);
        this.draft = json.getBool("draft", false);

        // Calculate total downloads and find JAR asset
        int downloads = 0;
        String jarUrl = "";
        Jval assets = json.get("assets");
        if (assets != null && assets.isArray()) {
            for (Jval asset : assets.asArray()) {
                downloads += asset.getInt("download_count", 0);
                String assetName = asset.getString("name", "");
                if (assetName.endsWith(".jar")) {
                    jarUrl = asset.getString("browser_download_url", "");
                }
            }
        }
        this.downloadCount = downloads;
        this.jarDownloadUrl = jarUrl;
    }

    /**
     * Get a formatted relative date string like "2 days ago"
     */
    public String getRelativeDate() {
        if (publishedAt.isEmpty())
            return "";
        try {
            // Parse ISO date: 2025-12-30T10:30:00Z
            String[] parts = publishedAt.split("T")[0].split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            // Simple relative date (good enough for display)
            java.time.LocalDate releaseDate = java.time.LocalDate.of(year, month, day);
            java.time.LocalDate today = java.time.LocalDate.now();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(releaseDate, today);

            if (daysDiff == 0)
                return "Today";
            if (daysDiff == 1)
                return "Yesterday";
            if (daysDiff < 7)
                return daysDiff + " days ago";
            if (daysDiff < 30)
                return (daysDiff / 7) + " weeks ago";
            if (daysDiff < 365)
                return (daysDiff / 30) + " months ago";
            return (daysDiff / 365) + " years ago";
        } catch (Exception e) {
            // Fallback to raw date
            return publishedAt.split("T")[0];
        }
    }

    /**
     * Get formatted download count like "1.2k"
     */
    public String getFormattedDownloads() {
        if (downloadCount >= 1000000) {
            return String.format("%.1fM", downloadCount / 1000000.0);
        } else if (downloadCount >= 1000) {
            return String.format("%.1fk", downloadCount / 1000.0);
        }
        return String.valueOf(downloadCount);
    }

    /**
     * Parse the changelog body into formatted lines for Mindustry UI.
     * Supports:
     * - Headers (## Section) -> [accent]Section[]
     * - List items (- item) -> • item
     * - Bold (**text**) -> [accent]text[]
     * - Links [text](url) -> text
     * - Code (`code`) -> [gray]code[]
     * - Emoji shortcuts
     */
    public Seq<String> getChangelogLines() {
        Seq<String> lines = new Seq<>();
        if (body == null || body.isEmpty()) {
            lines.add("[gray]No changelog available.[]");
            return lines;
        }

        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // Handle headers (## Section)
            if (line.startsWith("#")) {
                String header = line.replaceAll("^#+\\s*", "");
                header = convertEmoji(header);
                lines.add(""); // Add spacing before header
                lines.add("[accent]" + header + "[]");
                continue;
            }

            // Handle list items
            if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "[lightgray]•[] " + line.substring(2);
            }

            // Convert markdown formatting
            line = convertMarkdownToMindustry(line);
            lines.add(line);
        }

        // If still empty after parsing, add placeholder
        if (lines.isEmpty()) {
            lines.add("[gray]No changes documented.[]");
        }

        return lines;
    }

    /**
     * Convert GitHub Markdown to Mindustry color tags
     */
    private String convertMarkdownToMindustry(String text) {
        // Bold **text** -> [accent]text[]
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "[accent]$1[]");

        // Italic *text* or _text_ -> [lightgray]text[]
        text = text.replaceAll("\\*([^*]+)\\*", "[lightgray]$1[]");
        text = text.replaceAll("_([^_]+)_", "[lightgray]$1[]");

        // Code `text` -> [gray]text[]
        text = text.replaceAll("`([^`]+)`", "[gray]$1[]");

        // Links [text](url) -> just text (can't click in Mindustry labels)
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");

        // PR/Issue references (#123) -> [accent]#123[]
        text = text.replaceAll("#(\\d+)", "[accent]#$1[]");

        // Username mentions (@user) -> [sky]@user[]
        text = text.replaceAll("@(\\w+)", "[sky]@$1[]");

        // Convert common emojis
        text = convertEmoji(text);

        return text;
    }

    /**
     * Convert GitHub emoji shortcuts to unicode or text
     */
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

        return text;
    }

    /**
     * Get the Version object for comparison
     */
    public mindustrytool.utils.Version getVersion() {
        return new mindustrytool.utils.Version(tagName);
    }

    @Override
    public String toString() {
        return tagName;
    }
}
