package mindustrytool.features.tools.background;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustrytool.Feature;

public class BackgroundFeature implements Feature {

    private static BackgroundFeature instance;
    private Texture backgroundTexture;
    private TextureRegion backgroundRegion;
    private boolean enabled = true;
    private Fi backgroundFile;

    private mindustry.graphics.MenuRenderer currentRenderer;
    private mindustry.graphics.MenuRenderer originalRenderer;

    public BackgroundFeature() {
        instance = this;
    }

    public static BackgroundFeature getInstance() {
        return instance;
    }

    @Override
    public void init() {
        // Get stored background file path, or use default
        String storedPath = Core.settings.getString("mt-custom-bg-path", "");
        Fi bgDir = Vars.dataDirectory.child("mindustry-tool-backgrounds");
        bgDir.mkdirs();

        if (!storedPath.isEmpty()) {
            backgroundFile = bgDir.child(storedPath);
        } else {
            // Find any existing background file in the directory
            Fi[] files = bgDir.list();
            backgroundFile = files.length > 0 ? files[0] : bgDir.child("custom-bg.png");
        }

        enabled = Core.settings.getBool("mt-custom-bg-enabled", true);
        loadTexture();

        // Inject Custom Renderer
        Events.on(EventType.ClientLoadEvent.class, e -> {
            replaceRenderer();
        });

        // Hot reload support
        if (Vars.ui != null && Vars.ui.menufrag != null) {
            Core.app.post(this::replaceRenderer);
        }

        Log.info("[BackgroundFeature] Initialized. Background file: @, exists: @", backgroundFile.path(),
                backgroundFile.exists());
    }

    private void replaceRenderer() {
        if (!Vars.state.isMenu())
            return;

        try {
            // Find the 'renderer' field in MenuFragment
            // SECURITY: Accessing private 'renderer' field to inject custom background
            // logic.
            // No public setter exists in MenuFragment.
            java.lang.reflect.Field rendererField = null;
            Class<?> clazz = Vars.ui.menufrag.getClass();

            while (clazz != null) {
                try {
                    rendererField = clazz.getDeclaredField("renderer");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (rendererField != null) {
                rendererField.setAccessible(true);

                // Save original if not already saved
                if (originalRenderer == null) {
                    originalRenderer = (mindustry.graphics.MenuRenderer) rendererField.get(Vars.ui.menufrag);
                }

                // Don't double-replace if already set to ours
                Object current = rendererField.get(Vars.ui.menufrag);
                if (current instanceof CustomMenuRenderer) {
                    // Already replaced, just update reference if needed
                    return;
                }

                // Create and set custom renderer
                currentRenderer = new CustomMenuRenderer();
                rendererField.set(Vars.ui.menufrag, currentRenderer);

                Log.info("[BackgroundFeature] Successfully replaced MenuRenderer.");
            } else {
                Log.err("[BackgroundFeature] Could not find 'renderer' field in MenuFragment.");
            }

        } catch (Exception e) {
            Log.err("[BackgroundFeature] Failed to replace MenuRenderer", e);
        }
    }

    public class CustomMenuRenderer extends mindustry.graphics.MenuRenderer {
        @Override
        public void render() {
            if (backgroundTexture != null && enabled) {
                float w = Core.graphics.getWidth();
                float h = Core.graphics.getHeight();

                // Draw Custom Background
                Draw.reset();
                Draw.color(Color.white);

                // Explicit projection
                arc.math.Mat currentProj = new arc.math.Mat(Draw.proj());
                Core.camera.position.set(w / 2f, h / 2f);
                Core.camera.resize(w, h);
                Core.camera.update();
                Draw.proj(Core.camera);

                Draw.rect(backgroundRegion, w / 2f, h / 2f, w, h);
                Draw.flush();

                Draw.proj(currentProj);
            } else {
                // Fallback to original renderer
                if (originalRenderer != null) {
                    originalRenderer.render();
                } else {
                    super.render();
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Core.settings.put("mt-custom-bg-enabled", enabled);
        if (enabled) {
            loadTexture();
        } else {
            disposeTexture();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setBackgroundFile(Fi file) {
        // Clear old background files first
        Fi bgDir = Vars.dataDirectory.child("mindustry-tool-backgrounds");
        for (Fi f : bgDir.list()) {
            f.delete();
        }

        // Copy file preserving original extension
        String newName = "custom-bg." + file.extension();
        backgroundFile = bgDir.child(newName);
        file.copyTo(backgroundFile);

        // Store the filename for next load
        Core.settings.put("mt-custom-bg-path", newName);

        loadTexture();
        Log.info("[BackgroundFeature] Set background: @ -> @", file.name(), backgroundFile.path());
    }

    public void clearBackground() {
        if (backgroundFile != null && backgroundFile.exists()) {
            backgroundFile.delete();
        }
        Core.settings.put("mt-custom-bg-path", "");
        disposeTexture();
    }

    public boolean hasBackground() {
        return backgroundFile != null && backgroundFile.exists();
    }

    public Fi getBackgroundFile() {
        return backgroundFile;
    }

    private void loadTexture() {
        if (backgroundFile == null || !backgroundFile.exists())
            return;

        try {
            disposeTexture();

            backgroundTexture = new Texture(backgroundFile);
            backgroundTexture.setFilter(Texture.TextureFilter.linear);
            backgroundRegion = new TextureRegion(backgroundTexture);

            Log.info("[BackgroundFeature] Loaded custom background: @ (@x@)", backgroundFile.name(),
                    backgroundTexture.width, backgroundTexture.height);
        } catch (Exception e) {
            Log.err("[BackgroundFeature] Failed to load background", e);
        }
    }

    private void disposeTexture() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
            backgroundRegion = null;
        }
    }

    @Override
    public String getName() {
        return "Background Customizer";
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
