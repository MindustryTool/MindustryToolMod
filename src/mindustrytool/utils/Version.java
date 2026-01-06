package mindustrytool.utils;

import arc.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Semantic Versioning (vX.Y.Z-Suffix) with custom priority.
 * Priority: Fix > Stable (No suffix) > Beta > Dev
 */
public class Version implements Comparable<Version> {
    private static final Pattern PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?$");

    public final int major;
    public final int minor;
    public final int patch;
    public final String suffix;
    public final SuffixType type;

    public enum SuffixType {
        DEV(0), // ...-dev
        BETA(1), // ...-beta
        STABLE(2), // (no suffix)
        FIX(3); // ...-fix

        public final int priority;

        SuffixType(int priority) {
            this.priority = priority;
        }
    }

    public Version(String version) {
        Matcher m = PATTERN.matcher(version.trim());
        if (m.matches()) {
            this.major = Integer.parseInt(m.group(1));
            this.minor = Integer.parseInt(m.group(2));
            this.patch = Integer.parseInt(m.group(3));
            this.suffix = m.group(4); // Can be null
            this.type = parseType(this.suffix);
        } else {
            // Fallback for invalid versions (treat as 0.0.0-dev)
            Log.warn("Invalid version format: @", version);
            this.major = 0;
            this.minor = 0;
            this.patch = 0;
            this.suffix = version;
            this.type = SuffixType.DEV;
        }
    }

    private SuffixType parseType(String suffix) {
        if (suffix == null || suffix.isEmpty())
            return SuffixType.STABLE;
        String s = suffix.toLowerCase();
        if (s.startsWith("fix") || s.startsWith("hotfix"))
            return SuffixType.FIX;
        if (s.startsWith("beta") || s.startsWith("rc"))
            return SuffixType.BETA;
        return SuffixType.DEV;
    }

    @Override
    public int compareTo(Version other) {
        if (this.major != other.major)
            return Integer.compare(this.major, other.major);
        if (this.minor != other.minor)
            return Integer.compare(this.minor, other.minor);
        if (this.patch != other.patch)
            return Integer.compare(this.patch, other.patch);
        return Integer.compare(this.type.priority, other.type.priority);
    }

    public boolean isNewerThan(Version other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public String toString() {
        return "v" + major + "." + minor + "." + patch + (suffix != null ? "-" + suffix : "");
    }
}
