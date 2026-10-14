package mindustrytool.features.modmanager;

import arc.util.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Semantic and stage-aware version comparison utility ported from Helium Mod.
 * Supports dot/dash/underscore-separated tokens, overflow-safe integer comparison,
 * and pre-release/post-release stage ordering.
 */
public final class VersionCompareHelper {

    private static final int STAGE_SNAPSHOT = 0;
    private static final int STAGE_PRE = 1;
    private static final int STAGE_ALPHA = 2;
    private static final int STAGE_BETA = 3;
    private static final int STAGE_RC = 4;
    private static final int STAGE_UNKNOWN = 5;
    private static final int STAGE_RELEASE = 6;
    private static final int STAGE_POST = 7;

    private VersionCompareHelper() {
    }

    /**
     * Compares two version strings.
     *
     * @param first  the first version string
     * @param second the second version string
     * @return negative if {@code first < second}, zero if equal, positive if {@code first > second}
     */
    public static int tryCompareVersion(@Nullable String first, @Nullable String second) {
        String a = normalizeVersion(first);
        String b = normalizeVersion(second);
        if (a.equals(b)) {
            return 0;
        }
        if (a.isEmpty()) {
            return -1;
        }
        if (b.isEmpty()) {
            return 1;
        }

        List<String> tokensA = tokenize(a);
        List<String> tokensB = tokenize(b);
        int max = Math.max(tokensA.size(), tokensB.size());

        for (int i = 0; i < max; i++) {
            String tokA = i < tokensA.size() ? tokensA.get(i) : null;
            String tokB = i < tokensB.size() ? tokensB.get(i) : null;

            if (tokA == null) {
                return resolveTrailingWait(tokB, true);
            }
            if (tokB == null) {
                return resolveTrailingWait(tokA, false);
            }

            int cmp = compareToken(tokA, tokB);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int resolveTrailingWait(String remainingToken, boolean firstEnded) {
        if (isNumeric(remainingToken)) {
            boolean nonZero = !new BigInteger(remainingToken).equals(BigInteger.ZERO);
            if (!nonZero) {
                return 0;
            }
            return firstEnded ? -1 : 1;
        }
        int stage = stageRank(remainingToken);
        if (stage < STAGE_RELEASE) {
            // A version with a pre-release suffix (e.g. 1.0-beta) is older than 1.0
            return firstEnded ? 1 : -1;
        }
        if (stage > STAGE_RELEASE) {
            // A version with a post-release suffix (e.g. 1.0-hotfix) is newer than 1.0
            return firstEnded ? -1 : 1;
        }
        return 0;
    }

    private static int compareToken(String tokA, String tokB) {
        boolean numA = isNumeric(tokA);
        boolean numB = isNumeric(tokB);
        if (numA && numB) {
            return new BigInteger(tokA).compareTo(new BigInteger(tokB));
        }
        if (numA != numB) {
            // Numeric segment has higher precedence than a pre-release qualifier at the same position
            int stage = stageRank(numA ? tokB : tokA);
            if (stage < STAGE_RELEASE) {
                return numA ? 1 : -1;
            }
            return numA ? -1 : 1;
        }

        int rankA = stageRank(tokA);
        int rankB = stageRank(tokB);
        if (rankA != rankB) {
            return Integer.compare(rankA, rankB);
        }
        if (rankA != STAGE_UNKNOWN) {
            return 0;
        }
        return tokA.compareToIgnoreCase(tokB);
    }

    private static int stageRank(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "snapshot":
            case "nightly":
            case "dev":
                return STAGE_SNAPSHOT;
            case "pre":
            case "ea":
                return STAGE_PRE;
            case "alpha":
            case "a":
                return STAGE_ALPHA;
            case "beta":
            case "b":
            case "milestone":
            case "m":
                return STAGE_BETA;
            case "rc":
            case "cr":
                return STAGE_RC;
            case "release":
            case "stable":
            case "ga":
            case "final":
            case "r":
                return STAGE_RELEASE;
            case "hotfix":
            case "sp":
            case "build":
            case "patch":
                return STAGE_POST;
            default:
                return STAGE_UNKNOWN;
        }
    }

    private static String normalizeVersion(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int plusIndex = trimmed.indexOf('+');
        if (plusIndex >= 0) {
            trimmed = trimmed.substring(0, plusIndex).trim();
        }
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    static List<String> tokenize(String version) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int prevType = 0; // 0 = none, 1 = digit, 2 = letter

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c == '.' || c == '-' || c == '_' || c == '+' || Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                prevType = 0;
                continue;
            }
            int type = Character.isDigit(c) ? 1 : 2;
            if (prevType != 0 && prevType != type && current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
            prevType = type;
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static boolean isNumeric(String token) {
        if (token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
