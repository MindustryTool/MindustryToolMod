package mindustrytool;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import arc.struct.Seq;
import arc.util.Log;

/**
 * Minimal module loader: only discovers and initializes modules.
 * Each module is responsible for its own lifecycle (menu buttons, events, dispose).
 */
public class ModuleLoader {

    /**
     * Public nested Module interface so the project only needs this single file
     * for module definitions and loading. Modules should implement this interface
     * and be discoverable via ServiceLoader or legacy .module files.
     */
    public static interface Module {
        String getName();
        void init();
    }

    private static final String MODULE_PATH = "META-INF/modules/";
    private static final Seq<Module> modules = new Seq<>();

    /**
     * Discover and initialize all modules.
     */
    public static void loadModules() {
        Log.info("[ModuleLoader] Scanning for modules...");

        ClassLoader classLoader = ModuleLoader.class.getClassLoader();

        // 1. ServiceLoader discovery
        try {
            ServiceLoader<Module> loader = ServiceLoader.load(Module.class, classLoader);
            for (Module m : loader) {
                try {
                    Log.info("[ModuleLoader] ServiceLoader found: @", m.getName());
                    m.init();
                    modules.add(m);
                } catch (Exception e) {
                    Log.err(e);
                }
            }
        } catch (Throwable t) {
            Log.info("[ModuleLoader] ServiceLoader unavailable");
        }

        // 2. Legacy .module property files
        Seq<String> moduleClasses = new Seq<>();
        scanModuleFiles(classLoader, moduleClasses);
        tryLoadModuleFile(classLoader, MODULE_PATH + "browser.module", moduleClasses);
        tryLoadModuleFile(classLoader, MODULE_PATH + "playerconnect.module", moduleClasses);

        for (String className : moduleClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                if (Module.class.isAssignableFrom(clazz)) {
                    Module module = (Module) clazz.getDeclaredConstructor().newInstance();
                    if (modules.find(m -> m.getClass().equals(module.getClass())) == null) {
                        Log.info("[ModuleLoader] Loading: @", module.getName());
                        module.init();
                        modules.add(module);
                    }
                }
            } catch (Exception e) {
                Log.err("[ModuleLoader] Failed to load: @", className);
                Log.err(e);
            }
        }

        Log.info("[ModuleLoader] Loaded @ modules", modules.size);
    }

    private static void scanModuleFiles(ClassLoader classLoader, Seq<String> moduleClasses) {
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_PATH);
            while (resources.hasMoreElements()) {
                URL dirUrl = resources.nextElement();
                try {
                    if ("file".equals(dirUrl.getProtocol())) {
                        File dir = new File(dirUrl.toURI());
                        if (dir.exists() && dir.isDirectory()) {
                            File[] files = dir.listFiles(f -> f.getName().endsWith(".module"));
                            if (files != null) for (File f : files) {
                                try (InputStream is = f.toURI().toURL().openStream()) {
                                    Properties props = new Properties();
                                    props.load(is);
                                    String mc = props.getProperty("module.class");
                                    if (mc != null && !mc.isEmpty() && !moduleClasses.contains(mc)) moduleClasses.add(mc);
                                }
                            }
                        }
                    } else if ("jar".equals(dirUrl.getProtocol()) || dirUrl.getPath().contains(".jar!")) {
                        String path = dirUrl.getPath();
                        int idx = path.indexOf("!");
                        if (idx != -1) path = path.substring(0, idx);
                        if (path.startsWith("file:")) path = path.substring(5);
                        path = URLDecoder.decode(path, "UTF-8");
                        try (JarFile jf = new JarFile(path)) {
                            Enumeration<JarEntry> entries = jf.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.startsWith(MODULE_PATH) && name.endsWith(".module")) {
                                    try (InputStream is = jf.getInputStream(entry)) {
                                        Properties props = new Properties();
                                        props.load(is);
                                        String mc = props.getProperty("module.class");
                                        if (mc != null && !mc.isEmpty() && !moduleClasses.contains(mc)) moduleClasses.add(mc);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void tryLoadModuleFile(ClassLoader classLoader, String path, Seq<String> moduleClasses) {
        try {
            Enumeration<URL> urls = classLoader.getResources(path);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream is = url.openStream()) {
                    Properties props = new Properties();
                    props.load(is);
                    String mc = props.getProperty("module.class");
                    if (mc != null && !mc.isEmpty() && !moduleClasses.contains(mc)) moduleClasses.add(mc);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Get loaded modules.
     */
    public static Seq<Module> getModules() {
        return modules;
    }
}
