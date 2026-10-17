package mindustrytool.features.modmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.Settings;
import arc.files.Fi;
import java.io.File;
import java.util.ArrayList;
import mindustrytool.features.modmanager.LocalFavoritesStore.ConflictResolution;
import mindustrytool.features.modmanager.LocalFavoritesStore.FavoriteFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFavoritesStoreTest {

    @TempDir
    File tempDir;

    private LocalFavoritesStore store;

    @BeforeEach
    void setUp() {
        Core.settings = new Settings();
        Core.settings.setDataDirectory(new Fi(tempDir));
        store = new LocalFavoritesStore();
    }

    @Test
    void createsRenamesDeletesFoldersAndManagesFavoriteMods() {
        FavoriteFolder folder = store.createFolder("Combat Mods");
        assertNotNull(folder);
        assertEquals("Combat Mods", folder.getName());
        assertEquals(1, store.folders().peek().size());

        ModName mod = new ModName("Helium", "EB_Wilson", "EB-Wilson/Helium");
        assertTrue(store.toggleModInFolder("Combat Mods", mod));
        assertTrue(store.isFavorited(mod));
        assertEquals(1, store.folders().peek().get(0).getMods().size());

        // Toggling again removes the mod from the folder
        assertFalse(store.toggleModInFolder("Combat Mods", mod));
        assertFalse(store.isFavorited(mod));

        // Add back and rename folder
        assertTrue(store.toggleModInFolder("Combat Mods", mod));
        assertTrue(store.renameFolder("Combat Mods", "QoL Essentials"));
        assertEquals("QoL Essentials", store.folders().peek().get(0).getName());

        assertTrue(store.deleteFolder("QoL Essentials"));
        assertTrue(store.folders().peek().isEmpty());
    }

    @Test
    void exportsParsesAndImportsFoldersWithConflictResolutions() {
        store.createFolder("My Pack");
        store.toggleModInFolder("My Pack", new ModName("Mod A", "Author A", "owner/mod-a"));

        FavoriteFolder external = new FavoriteFolder("My Pack", false, new ArrayList<>());
        external.getMods().add(new ModName("Mod A", "Author A", "owner/mod-a"));
        external.getMods().add(new ModName("Mod B", "Author B", "owner/mod-b"));

        String json = LocalFavoritesStore.exportFolderJson(external);
        FavoriteFolder parsed = LocalFavoritesStore.parseFolderJson(json);
        assertNotNull(parsed);
        assertEquals("My Pack", parsed.getName());
        assertEquals(2, parsed.getMods().size());

        // 1. MERGE into existing folder
        FavoriteFolder merged = store.importFolder(parsed, ConflictResolution.MERGE);
        assertEquals(1, store.folders().peek().size());
        assertEquals(2, merged.getMods().size());

        // 2. KEEP_BOTH creates suffixed folder name
        FavoriteFolder keptBoth = store.importFolder(parsed, ConflictResolution.KEEP_BOTH);
        assertEquals(2, store.folders().peek().size());
        assertEquals("My Pack (1)", keptBoth.getName());

        // 3. REPLACE overwrites existing folder's mods
        FavoriteFolder replacementSource = new FavoriteFolder("My Pack", false, new ArrayList<>());
        replacementSource.getMods().add(new ModName("Mod C", "Author C", "owner/mod-c"));
        FavoriteFolder replaced = store.importFolder(replacementSource, ConflictResolution.REPLACE);
        assertEquals(1, replaced.getMods().size());
        assertEquals("Mod C", replaced.getMods().get(0).getName());
    }
}
