package mindustrytool.features.modmanager;

import arc.Core;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mindustrytool.utils.JsonUtils;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Store managing local favorite mod folders, JSON export/import, and conflict resolution.
 */
public final class LocalFavoritesStore {

    public static final String SETTINGS_KEY = "mindustrytool.mod-manager.favorites";

    public enum ConflictResolution {
        KEEP_BOTH,
        MERGE,
        REPLACE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteFolder {
        private String name = "";
        private boolean collapsed = false;
        private List<ModName> mods = new ArrayList<>();

        public FavoriteFolder copy() {
            List<ModName> copiedMods = new ArrayList<>();
            if (mods != null) {
                for (ModName mod : mods) {
                    if (mod != null) {
                        copiedMods.add(new ModName(mod.getName(), mod.getAuthor(), mod.getRepo()));
                    }
                }
            }
            return new FavoriteFolder(name != null ? name : "", collapsed, copiedMods);
        }

        public boolean contains(ModName target) {
            if (mods == null || target == null) {
                return false;
            }
            for (ModName mod : mods) {
                if (mod != null && mod.matches(target)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final LocalFavoritesStore INSTANCE = new LocalFavoritesStore();

    private final Signal<List<FavoriteFolder>> foldersSignal = Signal.of(new ArrayList<>());
    private final Signal<Integer> versionSignal = Signal.of(0);
    private boolean loaded = false;

    public static LocalFavoritesStore getInstance() {
        INSTANCE.ensureLoaded();
        return INSTANCE;
    }

    /**
     * Package-private constructor for isolated unit testing.
     */
    LocalFavoritesStore() {
    }

    public Readable<List<FavoriteFolder>> folders() {
        return foldersSignal;
    }

    public Readable<Integer> version() {
        return versionSignal;
    }

    public synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Core.settings == null) {
            return;
        }
        String raw = Core.settings.getString(SETTINGS_KEY, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            List<FavoriteFolder> parsed = JsonUtils.fromJsonArray(FavoriteFolder.class, raw);
            if (parsed != null) {
                List<FavoriteFolder> sanitized = new ArrayList<>();
                for (FavoriteFolder folder : parsed) {
                    if (folder != null && folder.getName() != null && !folder.getName().trim().isEmpty()) {
                        if (folder.getMods() == null) {
                            folder.setMods(new ArrayList<>());
                        }
                        sanitized.add(folder);
                    }
                }
                foldersSignal.set(Collections.unmodifiableList(sanitized));
                bumpVersion();
            }
        } catch (Exception e) {
            Log.warn("Failed to parse saved favorite folders: @", e.getMessage());
        }
    }

    public synchronized void save() {
        List<FavoriteFolder> current = new ArrayList<>(foldersSignal.peek());
        foldersSignal.set(Collections.unmodifiableList(current));
        bumpVersion();
        if (Core.settings != null) {
            try {
                Core.settings.put(SETTINGS_KEY, JsonUtils.toJson(current));
            } catch (Exception e) {
                Log.warn("Failed to save favorite folders: @", e.getMessage());
            }
        }
    }

    private void bumpVersion() {
        Integer v = versionSignal.peek();
        versionSignal.set((v != null ? v : 0) + 1);
    }

    public synchronized boolean hasFolder(String name) {
        return findFolder(name) != null;
    }

    public synchronized @Nullable FavoriteFolder findFolder(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        for (FavoriteFolder folder : foldersSignal.peek()) {
            if (folder.getName().equalsIgnoreCase(trimmed)) {
                return folder;
            }
        }
        return null;
    }

    public synchronized FavoriteFolder createFolder(String baseName) {
        String clean = baseName != null && !baseName.trim().isEmpty() ? baseName.trim() : "Favorites";
        String uniqueName = deduplicateName(clean);
        List<FavoriteFolder> next = copyList();
        FavoriteFolder created = new FavoriteFolder(uniqueName, false, new ArrayList<>());
        next.add(created);
        foldersSignal.set(Collections.unmodifiableList(next));
        save();
        return created;
    }

    public synchronized boolean renameFolder(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }
        String targetName = newName.trim();
        FavoriteFolder existingTarget = findFolder(targetName);
        if (existingTarget != null && !existingTarget.getName().equalsIgnoreCase(oldName.trim())) {
            return false;
        }
        List<FavoriteFolder> next = copyList();
        for (FavoriteFolder folder : next) {
            if (folder.getName().equalsIgnoreCase(oldName.trim())) {
                folder.setName(targetName);
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteFolder(String name) {
        if (name == null) {
            return false;
        }
        List<FavoriteFolder> next = copyList();
        boolean removed = next.removeIf(f -> f.getName().equalsIgnoreCase(name.trim()));
        if (removed) {
            foldersSignal.set(Collections.unmodifiableList(next));
            save();
        }
        return removed;
    }

    public synchronized void toggleCollapsed(String name) {
        if (name == null) {
            return;
        }
        List<FavoriteFolder> next = copyList();
        for (FavoriteFolder folder : next) {
            if (folder.getName().equalsIgnoreCase(name.trim())) {
                folder.setCollapsed(!folder.isCollapsed());
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return;
            }
        }
    }

    public synchronized boolean toggleModInFolder(String folderName, ModName mod) {
        if (folderName == null || mod == null) {
            return false;
        }
        List<FavoriteFolder> next = copyList();
        for (FavoriteFolder folder : next) {
            if (folder.getName().equalsIgnoreCase(folderName.trim())) {
                List<ModName> mods = folder.getMods();
                boolean removed = mods.removeIf(m -> m != null && m.matches(mod));
                if (!removed) {
                    mods.add(new ModName(mod.getName(), mod.getAuthor(), mod.getRepo()));
                }
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return !removed;
            }
        }
        return false;
    }

    public synchronized boolean isFavorited(@Nullable ModName mod) {
        if (mod == null) {
            return false;
        }
        for (FavoriteFolder folder : foldersSignal.peek()) {
            if (folder.contains(mod)) {
                return true;
            }
        }
        return false;
    }

    public static String exportFolderJson(FavoriteFolder folder) {
        return JsonUtils.toJsonPretty(folder.copy());
    }

    public static FavoriteFolder parseFolderJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty folder JSON");
        }
        FavoriteFolder parsed = JsonUtils.fromJson(FavoriteFolder.class, json.trim());
        if (parsed == null || parsed.getName() == null || parsed.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid favorite folder JSON: missing name");
        }
        if (parsed.getMods() == null) {
            parsed.setMods(new ArrayList<>());
        }
        parsed.setName(parsed.getName().trim());
        return parsed;
    }

    public synchronized FavoriteFolder importFolder(FavoriteFolder imported, ConflictResolution resolution) {
        FavoriteFolder clean = imported.copy();
        if (clean.getName().trim().isEmpty()) {
            clean.setName("Imported");
        }
        List<FavoriteFolder> next = copyList();
        int conflictIndex = -1;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).getName().equalsIgnoreCase(clean.getName())) {
                conflictIndex = i;
                break;
            }
        }

        if (conflictIndex < 0) {
            next.add(clean);
            foldersSignal.set(Collections.unmodifiableList(next));
            save();
            return clean;
        }

        switch (resolution) {
            case REPLACE:
                next.set(conflictIndex, clean);
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return clean;
            case MERGE:
                FavoriteFolder target = next.get(conflictIndex);
                for (ModName mod : clean.getMods()) {
                    if (mod != null && !target.contains(mod)) {
                        target.getMods().add(mod);
                    }
                }
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return target;
            case KEEP_BOTH:
            default:
                clean.setName(deduplicateName(clean.getName()));
                next.add(clean);
                foldersSignal.set(Collections.unmodifiableList(next));
                save();
                return clean;
        }
    }

    private String deduplicateName(String baseName) {
        if (!hasFolder(baseName)) {
            return baseName;
        }
        int suffix = 1;
        while (hasFolder(baseName + " (" + suffix + ")")) {
            suffix++;
        }
        return baseName + " (" + suffix + ")";
    }

    private List<FavoriteFolder> copyList() {
        List<FavoriteFolder> copy = new ArrayList<>();
        for (FavoriteFolder f : foldersSignal.peek()) {
            copy.add(f.copy());
        }
        return copy;
    }
}
