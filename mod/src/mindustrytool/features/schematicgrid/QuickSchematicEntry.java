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
    public String schematicName;
    public @Nullable String schematicFile;
    public @Nullable String customIconType;
    public @Nullable String customIconName;
    public @Nullable String customLabel;

    public QuickSchematicEntry() {
    }

    public QuickSchematicEntry(
            String id,
            String schematicName,
            @Nullable String schematicFile,
            @Nullable String customIconType,
            @Nullable String customIconName,
            @Nullable String customLabel) {
        this.id = id;
        this.schematicName = schematicName;
        this.schematicFile = schematicFile;
        this.customIconType = customIconType;
        this.customIconName = customIconName;
        this.customLabel = customLabel;
    }

    public static QuickSchematicEntry of(String schematicName, @Nullable String schematicFile) {
        String name = schematicName != null ? schematicName : "";
        return new QuickSchematicEntry(UUID.randomUUID().toString(), name, schematicFile, null, null, null);
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
        return equalsNullable(id, that.id)
                && equalsNullable(schematicName, that.schematicName)
                && equalsNullable(schematicFile, that.schematicFile)
                && equalsNullable(customIconType, that.customIconType)
                && equalsNullable(customIconName, that.customIconName)
                && equalsNullable(customLabel, that.customLabel);
    }

    @Override
    public int hashCode() {
        int result = hashNullable(id);
        result = 31 * result + hashNullable(schematicName);
        result = 31 * result + hashNullable(schematicFile);
        result = 31 * result + hashNullable(customIconType);
        result = 31 * result + hashNullable(customIconName);
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
