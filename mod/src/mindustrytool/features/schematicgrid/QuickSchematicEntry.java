package mindustrytool.features.schematicgrid;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mindustrytool.utils.JsonUtils;

/**
 * Serializable entry linking a quick-grid slot to a local schematic.
 * Resolution prefers exact file match, then falls back to schematic name.
 */
public class QuickSchematicEntry {

    public String id;
    public int page;
    public int row;
    public int col;
    public String schematicName;
    public @Nullable String schematicFile;
    public @Nullable String customIcon;
    public @Nullable String customLabel;

    public QuickSchematicEntry() {
    }

    public QuickSchematicEntry(
            String id,
            int page,
            int row,
            int col,
            String schematicName,
            @Nullable String schematicFile,
            @Nullable String customIcon,
            @Nullable String customLabel) {
        this.id = id;
        this.page = page;
        this.row = row;
        this.col = col;
        this.schematicName = schematicName;
        this.schematicFile = schematicFile;
        this.customIcon = customIcon;
        this.customLabel = customLabel;
    }

    public QuickSchematicEntry(
            String id,
            String schematicName,
            @Nullable String schematicFile,
            @Nullable String customIcon,
            @Nullable String customLabel) {
        this(id, 0, 0, 0, schematicName, schematicFile, customIcon, customLabel);
    }

    public static QuickSchematicEntry of(int page, int row, int col, String schematicName, @Nullable String schematicFile) {
        String name = schematicName != null ? schematicName : "";
        return new QuickSchematicEntry(UUID.randomUUID().toString(), page, row, col, name, schematicFile, null, null);
    }

    public static QuickSchematicEntry of(String schematicName, @Nullable String schematicFile) {
        return of(0, 0, 0, schematicName, schematicFile);
    }

    public boolean hasCustomIcon() {
        return customIcon != null && !customIcon.trim().isEmpty();
    }

    public String displayName() {
        if (customLabel != null && !customLabel.trim().isEmpty()) {
            return customLabel;
        }
        return schematicName != null ? schematicName : "";
    }

    public static String toJson(List<QuickSchematicEntry> entries) {
        List<QuickSchematicEntry> safe = entries != null ? entries : new ArrayList<>();
        return JsonUtils.toJson(safe);
    }

    public static List<QuickSchematicEntry> fromJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<QuickSchematicEntry> parsed = JsonUtils.fromJsonArray(QuickSchematicEntry.class, json);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static List<QuickSchematicEntry> copyOf(List<QuickSchematicEntry> entries) {
        return new ArrayList<>(entries != null ? entries : new ArrayList<QuickSchematicEntry>());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuickSchematicEntry)) {
            return false;
        }
        QuickSchematicEntry that = (QuickSchematicEntry) other;
        return page == that.page
                && row == that.row
                && col == that.col
                && equalsNullable(id, that.id)
                && equalsNullable(schematicName, that.schematicName)
                && equalsNullable(schematicFile, that.schematicFile)
                && equalsNullable(customIcon, that.customIcon)
                && equalsNullable(customLabel, that.customLabel);
    }

    @Override
    public int hashCode() {
        int result = hashNullable(id);
        result = 31 * result + page;
        result = 31 * result + row;
        result = 31 * result + col;
        result = 31 * result + hashNullable(schematicName);
        result = 31 * result + hashNullable(schematicFile);
        result = 31 * result + hashNullable(customIcon);
        result = 31 * result + hashNullable(customLabel);
        return result;
    }

    private static boolean equalsNullable(@Nullable Object left, @Nullable Object right) {
        return left != null ? left.equals(right) : right == null;
    }

    private static int hashNullable(@Nullable Object value) {
        return value != null ? value.hashCode() : 0;
    }
}
