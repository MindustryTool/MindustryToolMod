package mindustrytool.services.update;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure version utilities extracted from {@code old.mindustrytool.services.UpdateService}.
 *
 * <p>Stripping rule for {@link #parseVersion(String)}:
 *
 * <ul>
 *   <li>If {@code null} or empty → {@code int[0]}.
 *   <li>Trim whitespace.
 *   <li>Remove leading {@code v} / {@code V} if present.
 *   <li>Strip suffix after first {@code -} or {@code +} (e.g. {@code 1.2.3-beta}, {@code
 *       2.0+build}).
 *   <li>Keep only digits and dots via {@code [^0-9.]}.
 *   <li>Split on {@code \.}, drop empty segments, parse ints. Any failure → {@code int[0]}.
 * </ul>
 *
 * Fixes legacy bug where {@code version.contains("v") ? substring(v, indexOf("-"))} throws when
 * {@code v} present without {@code -}.
 */
public final class VersionUtils {

	private VersionUtils() {}

	/**
	 * Parses a version string into int components.
	 *
	 * @param version raw version string (may be null)
	 * @return int[] components, empty if unparseable
	 */
	public static int[] parseVersion(String version) {
		if (version == null) {
			return new int[0];
		}
		version = version.trim();
		if (version.isEmpty()) {
			return new int[0];
		}
		try {
			// Remove leading v/V
			if (version.startsWith("v") || version.startsWith("V")) {
				version = version.substring(1);
			} else {
				// also handle embedded "v" like "v8-136" after trim already handled;
				// legacy code searched for 'v' anywhere, we keep leading-only semantics
				int vIndex = version.indexOf('v');
				int VIndex = version.indexOf('V');
				int idx = vIndex >= 0 ? vIndex : VIndex;
				if (idx >= 0) {
					version = version.substring(idx + 1);
				}
			}

			// Strip suffix after '-' or '+'
			int dash = version.indexOf('-');
			int plus = version.indexOf('+');
			int cut = -1;
			if (dash >= 0 && plus >= 0) {
				cut = Math.min(dash, plus);
			} else if (dash >= 0) {
				cut = dash;
			} else if (plus >= 0) {
				cut = plus;
			}
			if (cut >= 0) {
				version = version.substring(0, cut);
			}

			String clean = version.replaceAll("[^0-9.]", "");
			if (clean.isEmpty()) {
				return new int[0];
			}
			// Handle leading/trailing dots and consecutive dots
			// e.g. "1..2" → split gives ["1","","2"]; we filter empties
			String[] parts = clean.split("\\.");
			List<Integer> numbers = new ArrayList<>();
			for (String part : parts) {
				if (part.isEmpty()) {
					continue;
				}
				numbers.add(Integer.parseInt(part));
			}
			if (numbers.isEmpty()) {
				return new int[0];
			}
			int[] result = new int[numbers.size()];
			for (int i = 0; i < numbers.size(); i++) {
				result[i] = numbers.get(i);
			}
			return result;
		} catch (Exception e) {
			return new int[0];
		}
	}

	/** Returns true if v1 is greater than v2 lexicographically, longer wins if prefix equal. */
	public static boolean isGreater(int[] v1, int[] v2) {
		if (v1 == null) v1 = new int[0];
		if (v2 == null) v2 = new int[0];
		for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
			if (v1[i] > v2[i]) {
				return true;
			} else if (v1[i] < v2[i]) {
				return false;
			}
		}
		return v1.length > v2.length;
	}

	/** Compares two version arrays. Negative if v1 &lt; v2, zero if equal, positive if v1 &gt; v2. */
	public static int compare(int[] v1, int[] v2) {
		if (v1 == null) v1 = new int[0];
		if (v2 == null) v2 = new int[0];
		for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
			int cmp = Integer.compare(v1[i], v2[i]);
			if (cmp != 0) return cmp;
		}
		return Integer.compare(v1.length, v2.length);
	}

	/** Formats version array as dot-joined string. */
	public static String format(int[] version) {
		if (version == null || version.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < version.length; i++) {
			sb.append(version[i]);
			if (i < version.length - 1) {
				sb.append(".");
			}
		}
		return sb.toString();
	}
}
