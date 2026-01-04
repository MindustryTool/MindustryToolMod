package mindustrytool.features.tools.background;

import arc.files.Fi;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class BackgroundSettingsDialog extends BaseDialog {

    private arc.graphics.Texture previewTexture;

    public BackgroundSettingsDialog() {
        super("Background Settings");

        addCloseListener();

        buttons.defaults().pad(3);
        buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        buttons.button("Reset to defaults", Icon.refresh, () -> {
            BackgroundFeature.getInstance().clearBackground();
            setup();
        }).size(250f, 64f);

        onResize(this::setup);
        shown(this::setup);
        hidden(this::disposePreview);
    }

    public static void open() {
        new BackgroundSettingsDialog().show();
    }

    private void disposePreview() {
        if (previewTexture != null) {
            previewTexture.dispose();
            previewTexture = null;
        }
    }

    private void setup() {
        cont.clear();
        disposePreview();

        BackgroundFeature Feature = BackgroundFeature.getInstance();
        if (Feature == null) {
            cont.add("Feature not loaded!").color(mindustry.graphics.Pal.remove);
            return;
        }

        // Settings Table
        Table settings = new Table();
        settings.defaults().pad(5);

        // 1. Buttons
        Table buttons = new Table();
        buttons.defaults().size(220, 60).pad(10);

        buttons.button("Select Image", Icon.fileImage, () -> {
            Vars.platform.showFileChooser(true, "Image files", file -> {
                Feature.setBackgroundFile(file);
                setup(); // Rebuild to show new preview
            });
        });

        settings.add(buttons).row();

        // 3. Preview
        settings.add("Preview").color(arc.graphics.Color.lightGray).padTop(10).row();

        Table previewContainer = new Table();
        previewContainer.background(Styles.black8);

        if (Feature.hasBackground()) {
            try {
                // Load texture for preview
                Fi file = Feature.getBackgroundFile();
                if (file.exists()) {
                    previewTexture = new arc.graphics.Texture(file);
                    // Use linear filter for better preview quality
                    previewTexture.setFilter(arc.graphics.Texture.TextureFilter.linear);

                    Image img = new Image(new arc.graphics.g2d.TextureRegion(previewTexture));
                    img.setScaling(Scaling.fit);
                    previewContainer.add(img).grow();
                } else {
                    previewContainer.add("File missing").color(mindustry.graphics.Pal.remove);
                }
            } catch (Exception e) {
                previewContainer.add("Preview Error").color(mindustry.graphics.Pal.remove);
            }
        } else {
            previewContainer.add("Default Background").color(arc.graphics.Color.gray);
        }

        settings.add(previewContainer).size(480, 270).pad(10); // 16:9

        cont.add(settings);
    }
}
