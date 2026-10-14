package mindustrytool.features.modmanager;

import arc.util.Nullable;
import mindustry.core.Version;
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Mods.ModState;

/**
 * 13-bit bitmask representing installed or remote mod status flags, ported from Helium Mod.
 */
public final class ModStat {

    public static final int ENABLED = 1 << 0;
    public static final int CLIENT_ONLY = 1 << 1;
    public static final int JAR_MOD = 1 << 2;
    public static final int JS_MOD = 1 << 3;
    public static final int UP_TO_DATE = 1 << 4;
    public static final int LOCAL_FILE = 1 << 5;
    public static final int DEPRECATED = 1 << 6;
    public static final int UNSUPPORTED = 1 << 7;
    public static final int LIB_MISSING = 1 << 8;
    public static final int LIB_INCOMPLETE = 1 << 9;
    public static final int LIB_CIRCLE_DEPENDING = 1 << 10;
    public static final int ERROR = 1 << 11;
    public static final int BLACKLIST = 1 << 12;

    private static final int INVALID_MASK = DEPRECATED
            | UNSUPPORTED
            | LIB_MISSING
            | LIB_INCOMPLETE
            | LIB_CIRCLE_DEPENDING
            | ERROR
            | BLACKLIST;

    public static final int MIN_VALID_GAME_VERSION = 136;

    private ModStat() {
    }

    public static boolean isEnabled(int stat) {
        return (stat & ENABLED) != 0;
    }

    public static boolean isClientOnly(int stat) {
        return (stat & CLIENT_ONLY) != 0;
    }

    public static boolean isJAR(int stat) {
        return (stat & JAR_MOD) != 0;
    }

    public static boolean isJS(int stat) {
        return (stat & JS_MOD) != 0;
    }

    public static boolean isUpToDate(int stat) {
        return (stat & UP_TO_DATE) != 0;
    }

    public static boolean isLocalFile(int stat) {
        return (stat & LOCAL_FILE) != 0;
    }

    public static boolean isDeprecated(int stat) {
        return (stat & DEPRECATED) != 0;
    }

    public static boolean isUnsupported(int stat) {
        return (stat & UNSUPPORTED) != 0;
    }

    public static boolean isLibMissing(int stat) {
        return (stat & LIB_MISSING) != 0;
    }

    public static boolean isLibIncomplete(int stat) {
        return (stat & LIB_INCOMPLETE) != 0;
    }

    public static boolean isLibCircleDepending(int stat) {
        return (stat & LIB_CIRCLE_DEPENDING) != 0;
    }

    public static boolean isError(int stat) {
        return (stat & ERROR) != 0;
    }

    public static boolean isBlackListed(int stat) {
        return (stat & BLACKLIST) != 0;
    }

    public static boolean isValid(int stat) {
        return (stat & INVALID_MASK) == 0;
    }

    /**
     * Evaluates game version compatibility flags ({@link #DEPRECATED} or {@link #UNSUPPORTED})
     * for a given {@code minGameVersion} string against {@code currentBuild}.
     */
    public static int checkGameVersion(@Nullable String minGameVersion, int currentBuild) {
        int major = parseMajorVersion(minGameVersion);
        if (major < MIN_VALID_GAME_VERSION) {
            return DEPRECATED;
        }
        if (currentBuild > 0 && major > currentBuild) {
            return UNSUPPORTED;
        }
        return 0;
    }

    public static int parseMajorVersion(@Nullable String minGameVersion) {
        if (minGameVersion == null || minGameVersion.trim().isEmpty()) {
            return 0;
        }
        String trimmed = minGameVersion.trim();
        int dotIndex = trimmed.indexOf('.');
        String majorPart = dotIndex >= 0 ? trimmed.substring(0, dotIndex) : trimmed;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < majorPart.length(); i++) {
            char c = majorPart.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Computes the status bitmask for an installed {@link LoadedMod}.
     */
    public static int checkModStat(@Nullable LoadedMod mod, boolean updateAvailable) {
        if (mod == null) {
            return ERROR;
        }
        int stat = 0;
        if (mod.enabled()) {
            stat |= ENABLED;
        }
        if (mod.meta != null && mod.meta.hidden) {
            stat |= CLIENT_ONLY;
        }
        if (mod.isJava()) {
            stat |= JAR_MOD;
        }
        if (mod.root != null && mod.root.child("scripts").exists()) {
            stat |= JS_MOD;
        }
        if (!updateAvailable) {
            stat |= UP_TO_DATE;
        }
        String repo = mod.getRepo();
        if (repo == null || repo.trim().isEmpty()) {
            stat |= LOCAL_FILE;
        }
        if (mod.isBlacklisted()) {
            stat |= BLACKLIST;
        }
        int currentBuild = Version.build > 0 ? Version.build : 146;
        String minVer = mod.meta != null ? mod.meta.minGameVersion : null;
        stat |= checkGameVersion(minVer, currentBuild);

        if (!mod.hasUnmetDependencies()) {
            // Dependencies satisfied
        } else {
            stat |= LIB_MISSING;
        }

        if (mod.state == ModState.missingDependencies) {
            stat |= LIB_MISSING;
        } else if (mod.state == ModState.incompleteDependencies) {
            stat |= LIB_INCOMPLETE;
        } else if (mod.state == ModState.circularDependencies) {
            stat |= LIB_CIRCLE_DEPENDING;
        } else if (mod.state == ModState.contentErrors || mod.hasContentErrors()) {
            stat |= ERROR;
        } else if (mod.state == ModState.unsupported) {
            stat |= UNSUPPORTED;
        }

        return stat;
    }

    /**
     * Computes the status bitmask for a remote {@link ModListing} from the mod browser.
     */
    public static int checkRemoteModStat(@Nullable ModListing listing) {
        if (listing == null) {
            return ERROR;
        }
        int stat = 0;
        if (listing.isHasJava()) {
            stat |= JAR_MOD;
        }
        if (listing.isHasScripts()) {
            stat |= JS_MOD;
        }
        int currentBuild = Version.build > 0 ? Version.build : 146;
        stat |= checkGameVersion(listing.getMinGameVersion(), currentBuild);
        return stat;
    }
}
