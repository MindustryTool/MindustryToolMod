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
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;
import arc.graphics.g2d.TextureRegion;

public class BackgroundFeature implements Feature {
    private static final String SETTING_KEY = "mindustrytool.background.path";
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
            Fi file = Core.files.absolute(path);
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
            Log.err("Background file invalid: @", file.absolutePath());
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
            customRenderer = new CustomMenuRenderer(texture);
            Reflect.set(Vars.ui.menufrag, "renderer", customRenderer);
        } catch (Exception e) {
            Log.err("Failed to apply background", e);
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
                if (file != null) {
                    Core.settings.put(SETTING_KEY, file.absolutePath());
                    Core.settings.forceSave();
                    applyBackground(file);
                }
            });
        }).size(250, 60);

        return Optional.of(dialog);
    }

    public static class CustomMenuRenderer extends MenuRenderer {
        private final Texture texture;
        private final TextureRegion region;

        public CustomMenuRenderer(Texture texture) {
            super();
            this.texture = texture;
            this.region = new TextureRegion(texture);
        }

        @Override
        public void render() {
            try {
                Draw.reset();
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
