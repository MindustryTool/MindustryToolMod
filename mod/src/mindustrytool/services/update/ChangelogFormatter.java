package mindustrytool.services.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import arc.util.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mindustrytool.utils.TimeZones;

/**
 * Pure changelog formatting extracted from {@code UpdateService.fetchReleasesAndShowDialog}. No
 * network, no Arc, no bundle dependency — pure Java for unit testing.
 */
public final class ChangelogFormatter {

	private static final int MAX_RELEASES = 20;
	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(TimeZones.systemDefaultOrUtc());
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ChangelogFormatter() {}

	/** Release DTO for pure formatting. */
	public static final class ReleaseInfo {
		public final String tagName;
		public final String body;
		public final String publishedAt;
		public final int downloadCount;
		public final boolean prerelease;

		public ReleaseInfo(String tagName, String body, String publishedAt, int downloadCount) {
			this(tagName, body, publishedAt, downloadCount, false);
		}

		public ReleaseInfo(String tagName, String body, String publishedAt, int downloadCount, boolean prerelease) {
			this.tagName = tagName != null ? tagName : "";
			this.body = body;
			this.publishedAt = publishedAt != null ? publishedAt : "";
			this.downloadCount = downloadCount;
			this.prerelease = prerelease;
		}
	}

	/** Partitioned releases separating the matched target update from other releases. */
	public static final class PartitionedReleases {
		public final @Nullable ReleaseInfo targetRelease;
		public final List<ReleaseInfo> otherReleases;

		public PartitionedReleases(@Nullable ReleaseInfo targetRelease, List<ReleaseInfo> otherReleases) {
			this.targetRelease = targetRelease;
			this.otherReleases = otherReleases != null ? otherReleases : Collections.emptyList();
		}
	}

	/** Formats raw GitHub releases JSON string (array) into Mindustry markup. Caps at 20 entries. */
	public static String format(String releasesJson) {
		return format(releasesJson, true);
	}

	/** Parses raw GitHub releases JSON string into structured ReleaseInfo list. */
	public static List<ReleaseInfo> parseReleases(@Nullable String releasesJson, boolean includePrereleases) {
		if (releasesJson == null || releasesJson.trim().isEmpty()) {
			return Collections.emptyList();
		}
		try {
			JsonNode root = MAPPER.readTree(releasesJson);
			if (root == null || !root.isArray()) {
				return Collections.emptyList();
			}
			List<ReleaseInfo> list = new ArrayList<>();
			for (JsonNode node : root) {
				if (list.size() >= MAX_RELEASES) break;
				boolean prerelease = node.path("prerelease").asBoolean(false);
				if (!includePrereleases && prerelease) {
					continue;
				}
				String tagName = node.path("tag_name").asText("");
				String body = node.has("body") && !node.path("body").isNull()
						? node.path("body").asText("No description provided.")
						: "No description provided.";
				String publishedAt = node.path("published_at").asText("");
				int downloadCount = 0;
				JsonNode assets = node.path("assets");
				if (assets != null && assets.isArray()) {
					for (JsonNode asset : assets) {
						downloadCount += asset.path("download_count").asInt(0);
					}
				}
				list.add(new ReleaseInfo(tagName, body, publishedAt, downloadCount, prerelease));
			}
			return list;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Partitions releases into the target update release and remaining releases.
	 */
	public static PartitionedReleases partition(@Nullable String releasesJson, @Nullable String targetVersion, boolean includePrereleases) {
		if (releasesJson == null || releasesJson.trim().isEmpty()) {
			return new PartitionedReleases(null, Collections.emptyList());
		}
		List<ReleaseInfo> list = parseReleases(releasesJson, includePrereleases);
		if (list.isEmpty()) {
			return new PartitionedReleases(null, Collections.emptyList());
		}
		if (targetVersion == null || targetVersion.trim().isEmpty()) {
			return new PartitionedReleases(null, list);
		}

		String targetClean = targetVersion.trim();
		int targetIndex = -1;

		// 1. Exact tag name match
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).tagName.equalsIgnoreCase(targetClean)) {
				targetIndex = i;
				break;
			}
		}

		// 2. Parsed version comparison
		if (targetIndex < 0) {
			int[] targetParts = VersionUtils.parseVersion(targetClean);
			if (targetParts.length > 0) {
				String lowerTarget = targetClean.toLowerCase();
				boolean targetIsBeta = lowerTarget.contains("beta") || lowerTarget.contains("alpha") || lowerTarget.contains("preview");

				// Priority pass: match channel (stable vs beta)
				for (int i = 0; i < list.size(); i++) {
					ReleaseInfo r = list.get(i);
					int[] rParts = VersionUtils.parseVersion(r.tagName);
					if (VersionUtils.compare(targetParts, rParts) == 0) {
						boolean rIsBeta = r.prerelease || r.tagName.toLowerCase().contains("beta")
								|| r.tagName.toLowerCase().contains("alpha") || r.tagName.toLowerCase().contains("preview");
						if (targetIsBeta == rIsBeta) {
							targetIndex = i;
							break;
						}
					}
				}

				// Fallback pass: match any version component match
				if (targetIndex < 0) {
					for (int i = 0; i < list.size(); i++) {
						ReleaseInfo r = list.get(i);
						int[] rParts = VersionUtils.parseVersion(r.tagName);
						if (VersionUtils.compare(targetParts, rParts) == 0) {
							targetIndex = i;
							break;
						}
					}
				}
			}
		}

		if (targetIndex >= 0) {
			ReleaseInfo target = list.remove(targetIndex);
			return new PartitionedReleases(target, list);
		}

		return new PartitionedReleases(null, list);
	}

	/**
	 * Formats releases, optionally excluding prereleases.
	 * When {@code includePrereleases} is false, entries with {@code "prerelease": true} are skipped.
	 */
	public static String format(String releasesJson, boolean includePrereleases) {
		if (releasesJson == null || releasesJson.trim().isEmpty()) {
			return "Could not parse release notes.";
		}
		try {
			JsonNode root = MAPPER.readTree(releasesJson);
			if (root == null || !root.isArray()) {
				return "Could not parse release notes.";
			}
			List<ReleaseInfo> list = parseReleases(releasesJson, includePrereleases);
			return formatReleases(list);
		} catch (Exception e) {
			return "Could not parse release notes.";
		}
	}

	/** Pure formatting from structured releases. */
	public static String formatReleases(List<ReleaseInfo> releases) {
		return formatReleases(releases, TimeZones.systemDefaultOrUtc(), DATE_FORMATTER);
	}

	/**
	 * Finds the maximum release tag over all entries in raw GitHub releases JSON.
	 * Pure and crash-proof: never throws, returns {@code null} for null/blank input,
	 * malformed JSON, non-array payloads, or when no entry carries a parseable tag.
	 * Entries with missing, blank, or version-unparseable {@code tag_name} are skipped.
	 *
	 * @param releasesJson raw GitHub releases array JSON (may be null)
	 * @return the raw winning tag (e.g. {@code v5.0.3-v8-beta}), or null when none qualifies
	 */
	public static @Nullable String findLatestTag(@Nullable String releasesJson) {
		if (releasesJson == null || releasesJson.trim().isEmpty()) {
			return null;
		}
		try {
			JsonNode root = MAPPER.readTree(releasesJson);
			if (root == null || !root.isArray()) {
				return null;
			}
			String bestTag = null;
			int[] bestVersion = new int[0];
			for (JsonNode node : root) {
				if (node == null || node.isNull()) {
					continue;
				}
				String tag = node.path("tag_name").asText("");
				if (tag == null || tag.trim().isEmpty()) {
					continue;
				}
				int[] version;
				try {
					version = VersionUtils.parseVersion(tag);
				} catch (Exception ignored) {
					continue;
				}
				if (version == null || version.length == 0) {
					continue;
				}
				if (bestTag == null || VersionUtils.isGreater(version, bestVersion)) {
					bestTag = tag;
					bestVersion = version;
				}
			}
			return bestTag;
		} catch (Exception ignored) {
			return null;
		}
	}

	/** Pure formatting with prerelease filtering. */
	public static String formatReleases(List<ReleaseInfo> releases, boolean includePrereleases) {
		if (!includePrereleases && releases != null) {
			List<ReleaseInfo> filtered = new ArrayList<>();
			for (ReleaseInfo release : releases) {
				if (release != null && !release.prerelease) {
					filtered.add(release);
				}
			}
			return formatReleases(filtered);
		}
		return formatReleases(releases);
	}

	/** Formats a single release entry without extra trailing newlines. */
	public static String formatSingleRelease(@Nullable ReleaseInfo release) {
		return formatSingleRelease(release, TimeZones.systemDefaultOrUtc(), DATE_FORMATTER);
	}

	/** Overload with injectable zone/formatter for deterministic tests. */
	public static String formatSingleRelease(@Nullable ReleaseInfo release, ZoneId zoneId, DateTimeFormatter formatter) {
		if (release == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[accent]").append(release.tagName).append("[white]\n");
		if (release.publishedAt != null && !release.publishedAt.isEmpty()) {
			try {
				Instant instant = Instant.parse(release.publishedAt);
				DateTimeFormatter fmt = formatter != null ? formatter : DATE_FORMATTER;
				if (fmt.getZone() == null && zoneId != null) {
					fmt = fmt.withZone(zoneId);
				}
				sb.append("[lightgray]").append(fmt.format(instant)).append("[white] - ");
			} catch (Exception ignored) {
			}
		}
		sb.append("[gold]Download count: ").append(release.downloadCount).append("[white]\n");
		String body = release.body != null && !release.body.isEmpty() ? release.body : "No description provided.";
		sb.append(renderMarkdown(body));
		return sb.toString();
	}

	/** Formats a target release entry badged with a subtle green dot indicator. */
	public static String formatTargetRelease(@Nullable ReleaseInfo release) {
		return formatTargetRelease(release, TimeZones.systemDefaultOrUtc(), DATE_FORMATTER);
	}

	/** Overload with injectable zone/formatter for deterministic tests. */
	public static String formatTargetRelease(@Nullable ReleaseInfo release, ZoneId zoneId, DateTimeFormatter formatter) {
		if (release == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[green]● [accent]").append(release.tagName).append("[white]\n");
		if (release.publishedAt != null && !release.publishedAt.isEmpty()) {
			try {
				Instant instant = Instant.parse(release.publishedAt);
				DateTimeFormatter fmt = formatter != null ? formatter : DATE_FORMATTER;
				if (fmt.getZone() == null && zoneId != null) {
					fmt = fmt.withZone(zoneId);
				}
				sb.append("[lightgray]").append(fmt.format(instant)).append("[white] - ");
			} catch (Exception ignored) {
			}
		}
		sb.append("[gold]Download count: ").append(release.downloadCount).append("[white]\n");
		String body = release.body != null && !release.body.isEmpty() ? release.body : "No description provided.";
		sb.append(renderMarkdown(body));
		return sb.toString();
	}

	/** Overload with injectable zone/formatter for deterministic tests. */
	public static String formatReleases(List<ReleaseInfo> releases, ZoneId zoneId, DateTimeFormatter formatter) {
		if (releases == null || releases.isEmpty()) {
			return "";
		}
		StringBuilder changelog = new StringBuilder();
		int count = 0;
		for (ReleaseInfo release : releases) {
			if (count >= MAX_RELEASES) break;
			changelog.append(formatSingleRelease(release, zoneId, formatter)).append("\n\n");
			count++;
		}
		return changelog.toString();
	}

	private static final Pattern COMMIT_KEYWORD_PATTERN = Pattern.compile(
			"(?im)(^|[•\\-*]\\s*)(feat|fix|chore|refactor|perf|docs|style|test)(\\([^)\\r\\n]+\\))?(!?)(\\s*:)");

	/** Markdown to Mindustry markup, copied from {@code old.mindustrytool.Utils.renderMarkdown}. */
	public static String renderMarkdown(String text) {
		if (text == null) return "";
		text = text.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "[sky]$1[white]");
		text = text.replaceAll("(?m)^#{1,6}\\s+(.*)$", "[accent]$1[white]");
		text = text.replaceAll("(?m)^\\s*[-*]\\s+(.*)$", "• $1");
		text = text.replaceAll("\\*\\*(.*?)\\*\\*", "[white]$1[white]");
		text = text.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "[lightgray]$1[white]");
		text = text.replaceAll("`([^`]*)`", "[cyan]$1[white]");

		Matcher matcher = COMMIT_KEYWORD_PATTERN.matcher(text);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String prefix = matcher.group(1);
			String type = matcher.group(2);
			String scope = matcher.group(3);
			String breaking = matcher.group(4);
			String colon = matcher.group(5);

			String color = colorForCommitType(type);
			StringBuilder replacement = new StringBuilder();
			replacement.append(prefix);
			replacement.append("[").append(color).append("]").append(type);
			if (scope != null && !scope.isEmpty()) {
				replacement.append("[lightgray]").append(scope);
			}
			if (breaking != null && !breaking.isEmpty()) {
				replacement.append("[crimson]").append(breaking);
			}
			replacement.append("[white]").append(colon);

			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String colorForCommitType(String type) {
		if (type == null) return "#A1A1AA";
		switch (type.toLowerCase()) {
			case "feat": return "#82C341";
			case "fix": return "#EF4444";
			case "perf": return "#FAA31B";
			case "refactor": return "#88C6ED";
			case "docs": return "#38BDF8";
			case "test": return "#F472B6";
			case "style": return "#C084FC";
			case "chore":
			default: return "#A1A1AA";
		}
	}
}
