package mindustrytool.features.maprules;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.maps.Map;

/**
 * Saves the active playing map with its current rules.
 */
public class MapSaveService {

    public boolean hasCollision(String name) {
        if (name == null || Vars.maps == null) {
            return false;
        }
        Map existing = Vars.maps.byName(name.trim());
        return existing != null;
    }

    public @Nullable String currentMapName() {
        try {
            if (Vars.state != null && Vars.state.map != null) {
                String name = Vars.state.map.name();
                return name != null ? name : "";
            }
        } catch (Exception e) {
            Log.err("MapRules: failed to read current map name", e);
        }
        return "";
    }

    public @Nullable Map save(String name, boolean overwrite) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        if (Vars.maps == null || Vars.state == null || Vars.world == null) {
            return null;
        }
        String trimmed = name.trim();
        if (!overwrite && hasCollision(trimmed)) {
            return null;
        }
        try {
            ObjectMap<String, String> tags = new ObjectMap<>();
            tags.put("name", trimmed);
            tags.put("author", authorTag());
            tags.put("description", descriptionTag());
            return Vars.maps.saveMap(tags, false);
        } catch (Exception e) {
            Log.err("MapRules: failed to save map", e);
            return null;
        }
    }

    private String authorTag() {
        try {
            if (Vars.player != null && Vars.player.name != null) {
                return Vars.player.name;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String descriptionTag() {
        try {
            if (Vars.state != null && Vars.state.map != null) {
                String desc = Vars.state.map.description();
                return desc != null ? desc : "";
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
