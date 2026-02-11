package mindustrytool.features.background;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.MenuRenderer;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;
import arc.graphics.g2d.TextureRegion;

public class BackgroundFeature implements Feature {
    private static final String SETTING_KEY = "mindustrytool.background.path";
    private static final String SETTING_OPACITY_KEY = "mindustrytool.background.opacity";
    private MenuRenderer originalRenderer;
    private CustomMenuRenderer customRenderer;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.background")
                .description("Custom Background")
                .icon(Icon.image)
                .build();
    }

    @Override
    public void init() {
    }

    @Override
    public void onEnable() {
        String path = Core.settings.getString(SETTING_KEY, null);

        if (path != null) {
            Fi file = Main.backgroundsDir.child(path);

            if (!file.exists()) {
                file = Core.files.absolute(path);
            }

            if (file.exists() && !file.isDirectory()) {
                applyBackground(file);
            }
        }
    }

    @Override
    public void onDisable() {
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

    private void applyBackground(Fi file) {
        if (!file.exists() || file.isDirectory()) {
            Vars.ui.showInfo("Background file invalid: " + file.absolutePath());
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
            customRenderer = new CustomMenuRenderer(texture, originalRenderer);
            Reflect.set(Vars.ui.menufrag, "renderer", customRenderer);
        } catch (Exception e) {
            Vars.ui.showException("Failed to apply background", e);
        }
    }

    @Override
    public Optional<Dialog> setting() {
        BaseDialog dialog = new BaseDialog("Background Settings");

        dialog.addCloseButton();
        dialog.name = "backgroundSettingDialog";

        Table table = dialog.cont;
        table.button("Select Background Image", Icon.file, () -> {
            Vars.platform.showFileChooser(true, "png", file -> {
                try {
                    if (file != null) {
                        Fi dest = Main.backgroundsDir.child(file.name());
                        file.copyTo(dest);
                        Core.settings.put(SETTING_KEY, dest.name());
                        Core.settings.forceSave();
                        applyBackground(dest);
                    }
                } catch (Exception e) {
                    Vars.ui.showException("Failed to apply background", e);
                }
            });
        }).size(250, 60);

        table.row();
        table.slider(5, 100, 5, Core.settings.getInt(SETTING_OPACITY_KEY, 100), value -> {
            Core.settings.put(SETTING_OPACITY_KEY, (int) value);
        }).width(180).padTop(10);
        table.label(() -> Core.settings.getInt(SETTING_OPACITY_KEY, 100) + "%").padTop(10).padLeft(10);

        return Optional.of(dialog);
    }

    public static class CustomMenuRenderer extends MenuRenderer {
        private final Texture texture;
        private final TextureRegion region;
        private final MenuRenderer originalRenderer;

        public CustomMenuRenderer(Texture texture, MenuRenderer originalRenderer) {
            super();
            this.texture = texture;
            this.region = new TextureRegion(texture);
            this.originalRenderer = originalRenderer;
        }

        @Override
        public void render() {
            try {
                int opacity = Core.settings.getInt(SETTING_OPACITY_KEY, 100);

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
