package mindustrytool;

import arc.Core;
import arc.files.Fi;
import arc.struct.ObjectMap;
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
 * To add a new plugin: just add its class name to plugins.txt - no code changes
 * needed.
 * Plugins are loaded dynamically - if a plugin is deleted, others still work.
 * (Release v2.18.5 - Cache Hit Test)
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
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
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
        // Skip VoiceChat plugin on mobile platforms (javax.sound not available)
        // Must check BEFORE Class.forName() to prevent class verification crash
        if (Core.app.isMobile() && className.contains("VoiceChat")) {
            Log.info("[PluginLoader] Plugin @ skipped (Desktop only)", className);
            return false;
        }

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
        } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
            // This happens when a plugin depends on classes not available on this platform
            // e.g., VoiceChatPlugin requires javax.sound which is not available on Android
            Log.info("[PluginLoader] Plugin @ skipped (platform unsupported): @", className, e.getMessage());
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

        // Sort plugins by dependency (topological sort) and then priority
        try {
            Seq<Plugin> sorted = sortPlugins(plugins);
            plugins.clear();
            plugins.addAll(sorted);
        } catch (Exception e) {
            Log.err("[PluginLoader] Plugin sorting failed (Circular dependency?): @", e.getMessage());
            Log.err(e);
            // Fallback to simple priority sort
            plugins.sort((a, b) -> b.getPriority() - a.getPriority());
        }

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

        // Development hot-reload: Press F12 to reload all plugins
        // Workflow: Edit code -> ./gradlew jar -> Press F12 in game
        arc.Events.run(mindustry.game.EventType.Trigger.update, () -> {
            boolean shift = Core.input.keyDown(arc.input.KeyCode.shiftLeft)
                    || Core.input.keyDown(arc.input.KeyCode.shiftRight);
            boolean ctrl = Core.input.keyDown(arc.input.KeyCode.controlLeft)
                    || Core.input.keyDown(arc.input.KeyCode.controlRight);
            if (!Core.scene.hasField() && shift && ctrl && Core.input.keyTap(arc.input.KeyCode.f12)) {
                Log.info("[Dev] Ctrl+Shift+F12 pressed - Reloading all plugins...");
                reloadAllPlugins();
                Vars.ui.showInfoToast("[accent]Plugins reloaded!", 2f);
            }
        });
        Log.info("[Dev] Ctrl+Shift+F12 hot-reload registered. Edit code -> ./gradlew jar -> Ctrl+Shift+F12");
    }

    /** Reload all plugins - call this from F12 or other trigger */
    public static void reloadAllPlugins() {
        for (Plugin plugin : plugins) {
            try {
                Log.info("[Dev] Reloading: @", plugin.getName());
                plugin.reload();
            } catch (Exception e) {
                Log.err("[Dev] Failed to reload: @", plugin.getName());
                Log.err(e);
            }
        }
        Log.info("[Dev] All @ plugins reloaded", plugins.size);
    }

    /**
     * Topological sort handling dependencies and priority. Uses Kahn's algorithm.
     */
    private Seq<Plugin> sortPlugins(Seq<Plugin> input) {
        ObjectMap<String, Plugin> nameMap = new ObjectMap<>();
        ObjectMap<Plugin, Integer> inDegree = new ObjectMap<>();
        ObjectMap<Plugin, Seq<Plugin>> graph = new ObjectMap<>();

        input.each(p -> {
            nameMap.put(p.getName(), p);
            inDegree.put(p, 0);
            graph.put(p, new Seq<>());
        });

        // Build Graph
        for (Plugin p : input) {
            for (String depName : p.getDependencies()) {
                Plugin dep = nameMap.get(depName);
                if (dep == null) {
                    Log.warn("[PluginLoader] Plugin '@' requires missing dependency '@'", p.getName(), depName);
                    continue; // Skip missing dependencies
                }
                // Edge: dep -> p
                graph.get(dep).add(p);
                inDegree.put(p, inDegree.get(p) + 1);
            }
        }

        // Queue of plugins with no incoming edges (ready to load)
        Seq<Plugin> queue = new Seq<>();
        inDegree.each((p, degree) -> {
            if (degree == 0)
                queue.add(p);
        });

        // Sort initial queue by priority (higher first)
        queue.sort((a, b) -> b.getPriority() - a.getPriority());

        Seq<Plugin> result = new Seq<>();
        while (!queue.isEmpty()) {
            Plugin u = queue.first();
            queue.remove(0);
            result.add(u);

            if (graph.containsKey(u)) {
                for (Plugin v : graph.get(u)) {
                    inDegree.put(v, inDegree.get(v) - 1);
                    if (inDegree.get(v) == 0) {
                        queue.add(v);
                    }
                }
                // Re-sort queue to maintain priority order
                queue.sort((a, b) -> b.getPriority() - a.getPriority());
            }
        }

        if (result.size != input.size) {
            throw new RuntimeException("Cycle detected! Loaded " + result.size + " of " + input.size + " plugins.");
        }

        return result;
    }

    /** Get all loaded plugins. */
    public static Seq<Plugin> getPlugins() {
        return plugins;
    }

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

    public static void checkForUpdate() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        String currentStr = mod.meta.version;

        Http.get(API_REPO_URL, res -> {
            try {
                Jval json = Jval.read(res.getResultAsString());
                String latestStr = json.getString("version");

                mindustrytool.utils.Version current = new mindustrytool.utils.Version(currentStr);
                mindustrytool.utils.Version latest = new mindustrytool.utils.Version(latestStr);

                if (latest.isNewerThan(current)) {
                    Log.info("Update available: @ -> @", current, latest);

                    // Determine update type
                    String title = "Update Available";
                    String color = "[accent]";
                    String typeClean = "Patch";

                    if (latest.major > current.major) {
                        title = "[red]MAJOR UPDATE!";
                        color = "[red]";
                        typeClean = "Major Update";
                    } else if (latest.minor > current.minor) {
                        title = "[gold]New Features!";
                        color = "[gold]";
                        typeClean = "Feature Update";
                    } else if (latest.type == mindustrytool.utils.Version.SuffixType.FIX) {
                        title = "[green]Bug Fixes";
                        color = "[green]";
                        typeClean = "Fix";
                    }

                    final String finalTitle = title;
                    final String finalColor = color;
                    final String finalType = typeClean;

                    Core.app.post(() -> {
                        // For Fix/Dev updates, show Toast first (Passive)
                        if (latest.type.priority <= mindustrytool.utils.Version.SuffixType.FIX.priority
                                && latest.major == current.major && latest.minor == current.minor) {

                            // Show a toast that clickable? No, standard toast isn't clickable.
                            // We'll show a non-intrusive dialog or just a toast instructing to check
                            // settings?
                            // User requirement: "Toast... Người dùng phải bấm vào mới hiện".
                            // For simplicity in this step: Show a smaller, less obtrusive dialog.
                        }

                        mindustry.ui.dialogs.BaseDialog dialog = new mindustry.ui.dialogs.BaseDialog(finalType);

                        // Header
                        dialog.cont.table(t -> {
                            t.image(mindustry.gen.Icon.upload).size(50f).padRight(10f)
                                    .color(mindustry.graphics.Pal.accent);
                            t.add(finalTitle).fontScale(1.2f).color(mindustry.graphics.Pal.accent);
                        }).row();

                        dialog.cont.image().height(4f).color(arc.graphics.Color.gray).growX().pad(10f).row();

                        // Enhanced Version Info
                        dialog.cont.table(t -> {
                            t.defaults().pad(5f);
                            t.add("Current:").color(arc.graphics.Color.gray);
                            t.add(current.toString()).color(arc.graphics.Color.gray);
                            t.row();
                            t.image(mindustry.gen.Icon.downOpen).color(arc.graphics.Color.gray).row();
                            t.add("Latest:").color(arc.graphics.Color.lightGray);
                            t.add(latest.toString()).color(mindustry.graphics.Pal.accent).fontScale(1.1f);

                            // Tag label
                            if (latest.type == mindustrytool.utils.Version.SuffixType.BETA) {
                                t.add(" (BETA)").color(arc.graphics.Color.orange);
                            } else if (latest.type == mindustrytool.utils.Version.SuffixType.FIX) {
                                t.add(" (FIX)").color(mindustry.graphics.Pal.heal);
                            }
                        }).pad(10f).row();

                        // Changelog Placeholder (Will be enhanced later)
                        dialog.cont.labelWrap("Version " + latest + " is ready to download.").pad(10f).width(400f)
                                .center().row();

                        // Buttons
                        dialog.buttons.defaults().size(160f, 55f).pad(8f);
                        dialog.buttons.button("@cancel", mindustry.gen.Icon.cancel, dialog::hide);
                        dialog.buttons.button("GitHub", mindustry.gen.Icon.github, () -> {
                            Core.app.openURI("https://github.com/" + REPO_URL + "/releases");
                        });

                        dialog.buttons.button("Update Now", mindustry.gen.Icon.download, () -> {
                            dialog.hide();
                            Vars.ui.mods.githubImportMod(REPO_URL, true, null);
                        }).color(mindustry.graphics.Pal.accent);

                        dialog.show();
                    });
                } else {
                    Log.info("Mod is up to date (@)", current);
                }
            } catch (Exception e) {
                Log.err("Failed to check for updates", e);
            }
        });
    }
}
