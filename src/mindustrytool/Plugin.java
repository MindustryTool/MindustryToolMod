package mindustrytool;

import arc.util.Log;
import arc.struct.Seq;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Interface for MindustryTool plugins. Also contains static loader helpers that
 * read per-plugin meta.txt files from src/mindustrytool/plugins/<plugin>/meta.txt
 */
public interface Plugin {
    /** Plugin name for logging. */
    String getName();

    /** Priority for loading order (higher = load first). Default is 0. */
    default int getPriority() {
        return 0;
    }

    /** Initialize the plugin. Called during mod init. */
    void init();

    /** List of plugin names that this plugin depends on. */
    default String[] getDependencies() {
        return new String[0];
    }

    /** Dispose plugin resources. Called during mod dispose. */
    default void dispose() {
    }

    static Seq<Plugin> loadAll() {
        Seq<Plugin> found = new Seq<>();
        // resources dir
        scanDir(new File("build/resources/main/mindustrytool/plugins"), found);
        // source dir (when running from sources)
        scanDir(new File("src/mindustrytool/plugins"), found);
        // classpath / jar
        found.addAll(scanClasspath());
        return found;
    }

    static void scanDir(File pluginsDir, Seq<Plugin> out) {
        try {
            Log.info("[PluginLoader] Scanning dir: @", pluginsDir.getAbsolutePath());
            if (!pluginsDir.exists() || !pluginsDir.isDirectory()) return;
            File[] files = pluginsDir.listFiles();
            if (files == null) return;
            for (File pluginFolder : files) {
                if (!pluginFolder.isDirectory()) continue;
                File meta = new File(pluginFolder, "meta.txt");
                if (!meta.exists()) continue;
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(meta.toPath());
                    String cls = new String(bytes, StandardCharsets.UTF_8).trim();
                    if (!cls.isEmpty()) {
                        Plugin p = instantiate(cls);
                        if (p != null) out.add(p);
                    }
                } catch (Exception e) {
                    Log.err("[PluginLoader] Error reading meta.txt: @", meta.getAbsolutePath());
                    Log.err(e);
                }
            }
        } catch (Exception e) {
            Log.err("[PluginLoader] Error scanning dir: @", pluginsDir.getAbsolutePath());
            Log.err(e);
        }
    }

    static Seq<Plugin> scanClasspath() {
        Seq<Plugin> out = new Seq<>();
        try {
            java.net.URL loc = Plugin.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc != null) {
                java.io.File f = new java.io.File(loc.toURI());
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(f)) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry e = entries.nextElement();
                            String name = e.getName();
                            if (name.matches("mindustrytool/plugins/[^/]+/meta\\.txt")) {
                                try (InputStream is = jar.getInputStream(e)) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] buf = new byte[4096];
                                    int r;
                                    while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
                                    String cls = new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
                                    if (!cls.isEmpty()) {
                                        Plugin p = instantiate(cls);
                                        if (p != null) out.add(p);
                                    }
                                } catch (Exception ex) {
                                    Log.err("[PluginLoader] Error reading meta.txt from JAR: @", name);
                                    Log.err(ex);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.err("[PluginLoader] Error scanning classpath for meta.txt");
            Log.err(e);
        }
        return out;
    }

    static Plugin instantiate(String className) {
        try {
            Class<?> c = Class.forName(className);
            if (!Plugin.class.isAssignableFrom(c)) {
                Log.info("[PluginLoader] Skipping non-plugin: @", className);
                return null;
            }
            return (Plugin) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Log.err("[PluginLoader] Failed to instantiate plugin: @", className);
            Log.err(e);
            return null;
        }
    }
}
