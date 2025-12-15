package mindustrytool;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;

/**
 * MindustryTool Mod - Dynamic Plugin Loader.
 * Automatically loads all plugins listed in assets/plugins.txt.
 * To add a new plugin: just add its class name to plugins.txt - no code changes needed.
 * Plugins are loaded dynamically - if a plugin is deleted, others still work.
 */
public class Main extends Mod {
    public static final Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static final Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static final Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");
    
    private static final String API_REPO_URL = "https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/v8/mod.hjson";
    private static final String API_URL = "https://api.mindustry-tool.com/api/v4/";
    private static final String REPO_URL = "MindustryTool/MindustryToolMod";
    
    /** All loaded plugins. */
    private static final Seq<Plugin> plugins = new Seq<>();

    public Main() { 
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();
    }
    
    /** Load plugins from assets/plugins.txt file. */
    private void loadPluginsFromConfig() {
        try {
            Fi pluginFile = Vars.mods.getMod(Main.class).root.child("plugins.txt");
            if (!pluginFile.exists()) {
                Log.warn("[PluginLoader] plugins.txt not found");
                return;
            }
            String content = pluginFile.readString();
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                tryLoadPlugin(line);
            }
            Log.info("[PluginLoader] Found @ plugin(s) in plugins.txt", plugins.size);
        } catch (Exception e) {
            Log.err("[PluginLoader] Failed to read plugins.txt");
            Log.err(e);
        }
    }
    
    /** Try to load a plugin by class name. Returns false if not found. */
    private static boolean tryLoadPlugin(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Plugin.class.isAssignableFrom(clazz)) {
                Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                plugins.add(plugin);
                Log.info("[PluginLoader] Registered: @", plugin.getName());
                return true;
            }
        } catch (ClassNotFoundException e) {
            Log.info("[PluginLoader] Plugin not found (skipped): @", className);
        } catch (Exception e) {
            Log.err("[PluginLoader] Failed to register: @", className);
            Log.err(e);
        }
        return false;
    }

    @Override
    public void init() {
        checkForUpdate();
        
        // Load plugins from assets/plugins.txt
        loadPluginsFromConfig();
        
        // Sort by priority (higher first) and initialize
        plugins.sort((a, b) -> b.getPriority() - a.getPriority());
        
        for (Plugin plugin : plugins) {
            try {
                Log.info("[PluginLoader] Initializing: @ (priority: @)", plugin.getName(), plugin.getPriority());
                plugin.init();
                Log.info("[PluginLoader] Loaded: @", plugin.getName());
            } catch (Exception e) {
                Log.err("[PluginLoader] Failed to load: @", plugin.getName());
                Log.err(e);
            }
        }
        
        Log.info("[PluginLoader] All @ plugins loaded", plugins.size);
    }
    
    /** Get all loaded plugins. */
    public static Seq<Plugin> getPlugins() { return plugins; }
    
    /** Get a plugin by type. */
    @SuppressWarnings("unchecked")
    public static <T extends Plugin> T getPlugin(Class<T> type) {
        return (T) plugins.find(p -> type.isInstance(p));
    }
    
    /** Check if a plugin is loaded by class name (for soft dependencies). */
    public static boolean hasPlugin(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return plugins.contains(p -> clazz.isInstance(p));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void checkForUpdate() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        String currentVersion = mod.meta.version;

        Http.get(API_REPO_URL, res -> {
            Jval json = Jval.read(res.getResultAsString());
            String latestVersion = json.getString("version");

            if (!latestVersion.equals(currentVersion)) {
                Log.info("Mod requires update, current version: @, latest version: @", currentVersion, latestVersion);
                Vars.ui.showConfirm(Core.bundle.format("message.new-version", currentVersion, latestVersion) 
                        + "\nDiscord: https://discord.gg/72324gpuCd", () -> {
                    if (currentVersion.endsWith("v8")) return;
                    Core.app.post(() -> Vars.ui.mods.githubImportMod(REPO_URL, true, null));
                });
            } else {
                Log.info("Mod is up to date");
            }
        });

        Http.get(API_URL + "ping?client=mod-v8").submit(result -> Log.info("Ping"));
    }
}
