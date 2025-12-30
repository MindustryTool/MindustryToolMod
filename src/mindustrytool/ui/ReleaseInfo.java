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
     * Parse the changelog body into clean lines
     */
    public Seq<String> getChangelogLines() {
        Seq<String> lines = new Seq<>();
        if (body == null || body.isEmpty()) {
            lines.add("No changelog available.");
            return lines;
        }

        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            // Skip markdown headers for cleaner display
            if (line.startsWith("#")) {
                line = line.replaceAll("^#+\\s*", "");
            }
            // Convert markdown list items
            if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "• " + line.substring(2);
            }
            lines.add(line);
        }
        return lines;
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
