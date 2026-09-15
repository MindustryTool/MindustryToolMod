package mindustrytool.features.background;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import solim.overlay.SolimDialog;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.MenuRenderer;
import mindustrytool.Folders;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;

public class BackgroundFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<String> pathConfig;
    public final ConfigValue<Integer> opacityConfig;

    private MenuRenderer originalRenderer;
    private CustomMenuRenderer customRenderer;
    private @Nullable BackgroundSettingsDialog settingDialog;

    public BackgroundFeature() {
        super(FeatureMetadata.builder()
                .id("background")
                .icon(Icon.image)
                .build());

        config = configGroup();

        pathConfig = config.stringValue("path", "");
        opacityConfig = config.intValue("opacity", 100);

        pathConfig.signal().subscribe(path -> {
            if (isEnabled()) {
                applyPath(path);
            }
        });
    }

    @Override
    public @Nullable SolimDialog getSettingDialog() {
        if (settingDialog == null) {
            settingDialog = new BackgroundSettingsDialog(this);
        }
        return settingDialog;
    }

    @Override
    public void onEnable() {
        applyPath(pathConfig.get());
    }

    @Override
    public void onDisable() {
        restoreOriginalRenderer();
    }

    public void resetBackground() {
        pathConfig.reset();
        opacityConfig.reset();
        restoreOriginalRenderer();
    }

    public void applyPath(@Nullable String path) {
        if (path != null && !path.trim().isEmpty()) {
            Fi file = Folders.backgroundsDir.child(path);
            if (!file.exists()) {
                file = Core.files.absolute(path);
            }
            if (file.exists() && !file.isDirectory()) {
                applyBackground(file);
                return;
            }
        }
        restoreOriginalRenderer();
    }

    public void applyBackground(Fi file) {
        if (!file.exists() || file.isDirectory()) {
            Core.app.post(() -> {
                Vars.ui.showInfo(Core.bundle.format("feature.background.error.invalid-file", file.absolutePath()));
            });
            return;
        }

        try {
            if (originalRenderer == null) {
                originalRenderer = Reflect.get(Vars.ui.menufrag, "renderer");
            }

            if (customRenderer != null) {
                customRenderer.dispose();
            }

            Texture texture = new Texture(file);
            customRenderer = new CustomMenuRenderer(texture, originalRenderer, opacityConfig);
            Reflect.set(Vars.ui.menufrag, "renderer", customRenderer);
        } catch (Exception e) {
            Core.app.post(() -> {
                Vars.ui.showException(Core.bundle.get("feature.background.error.apply"), e);
            });
        }
    }

    public void restoreOriginalRenderer() {
        if (originalRenderer != null) {
            try {
                Reflect.set(Vars.ui.menufrag, "renderer", originalRenderer);
                if (customRenderer != null) {
                    customRenderer.dispose();
                    customRenderer = null;
                }
            } catch (Exception e) {
                Log.err("Failed to restore background", e);
            }
        }
    }

    public static class CustomMenuRenderer extends MenuRenderer {
        private final Texture texture;
        private final TextureRegion region;
        private final MenuRenderer originalRenderer;
        private final ConfigValue<Integer> opacityConfig;

        public CustomMenuRenderer(Texture texture, MenuRenderer originalRenderer, ConfigValue<Integer> opacityConfig) {
            super();
            this.texture = texture;
            this.region = new TextureRegion(texture);
            this.originalRenderer = originalRenderer;
            this.opacityConfig = opacityConfig;
        }

        @Override
        public void render() {
            try {
                Integer op = opacityConfig.get();
                int opacity = op != null ? op : 100;

                if (opacity < 100 && originalRenderer != null) {
                    originalRenderer.render();
                }

                Draw.reset();
                if (opacity < 100) {
                    Draw.alpha(opacity / 100f);
                }

                Draw.rect(region, Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 2f,
                        Core.graphics.getWidth(), Core.graphics.getHeight());
            } catch (Exception e) {
                Log.err(e);
            } finally {
                Draw.reset();
            }
        }

        @Override
        public void dispose() {
            if (texture != null) {
                texture.dispose();
            }
        }
    }
}
