package mindustrytool.features.modmanager;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model representing a Modpack configuration, its validation bitmask,
 * and serializable {@code pack.json} metadata compatible with Helium Mod.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackModel {

    public static final String MODE_DISTRIBUTE = "Distribute";
    public static final String MODE_SHADOW = "Shadow";
    public static final String MODE_OVERRIDE = "Override";

    public static final int STAT_NON_MOD_SELECT = 1 << 0;
    public static final int STAT_MISSING_INFO = 1 << 1;
    public static final int STAT_ICON_NON_SQUARE = 1 << 2;
    public static final int STAT_FILE_CONFLICT = 1 << 3;
    public static final int STAT_DEPEND_MISSING = 1 << 4;
    public static final int STAT_MOD_INVALID = 1 << 5;

    private static final int ERROR_MASK = STAT_NON_MOD_SELECT
            | STAT_MISSING_INFO
            | STAT_ICON_NON_SQUARE
            | STAT_FILE_CONFLICT;

    private static final int WARNING_MASK = STAT_DEPEND_MISSING | STAT_MOD_INVALID;

    private String displayName = "";
    private String name = "";
    private String description = "";
    private String version = "1.0";
    private String author = "";
    private @Nullable String iconPath;

    private String mode = MODE_DISTRIBUTE;
    private boolean skipRepeat = true;
    private boolean rawBackup = false;
    private boolean force = false;
    private boolean uncheck = false;

    private List<ModEntry> mods = new ArrayList<>();
    private List<FileEntry> files = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModEntry {
        private String name = "";
        private String displayName = "";
        private String version = "";
        private String author = "";
        private String fileName = "";
        private String sourcePath = "";
        private String shortDescription = "";
        private List<String> dependencies = new ArrayList<>();
        private int stat = 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileEntry {
        private String targetPath = "";
        private String sourcePath = "";
        private String category = "custom";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackMetadata {
        private String displayName = "";
        private String name = "";
        private String description = "";
        private String version = "1.0";
        private String author = "";
        private String mode = MODE_DISTRIBUTE;
        private boolean skipRepeat = true;
        private boolean rawBackup = false;
        private boolean force = false;
        private boolean uncheck = false;
        private List<ModMetaEntry> mods = new ArrayList<>();
        private List<String> files = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModMetaEntry {
        private String name = "";
        private String version = "";
        private String author = "";
        private String file = "";
    }

    public static boolean isStatValid(int stat) {
        return (stat & ERROR_MASK) == 0;
    }

    public static boolean hasStatWarning(int stat) {
        return (stat & WARNING_MASK) != 0;
    }

    public static boolean hasNonModSelect(int stat) {
        return (stat & STAT_NON_MOD_SELECT) != 0;
    }

    public static boolean hasMissingInfo(int stat) {
        return (stat & STAT_MISSING_INFO) != 0;
    }

    public static boolean hasIconNonSquare(int stat) {
        return (stat & STAT_ICON_NON_SQUARE) != 0;
    }

    public static boolean hasFileConflict(int stat) {
        return (stat & STAT_FILE_CONFLICT) != 0;
    }

    public static boolean hasDependMissing(int stat) {
        return (stat & STAT_DEPEND_MISSING) != 0;
    }

    public static boolean hasModInvalid(int stat) {
        return (stat & STAT_MOD_INVALID) != 0;
    }
}
