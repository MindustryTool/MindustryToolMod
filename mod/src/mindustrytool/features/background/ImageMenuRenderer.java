package mindustrytool.features.background;

import arc.graphics.Texture;
import arc.util.Nullable;
import mindustry.graphics.MenuRenderer;
import solim.config.ConfigValue;

public class ImageMenuRenderer extends BackgroundMenuRenderer {

    private final Texture texture;

    public ImageMenuRenderer(
            Texture texture,
            @Nullable MenuRenderer originalRenderer,
            ConfigValue<Integer> opacityConfig,
            ConfigValue<String> scalingConfig) {
        super(originalRenderer, opacityConfig, scalingConfig);
        this.texture = texture;
    }

    @Override
    public void render() {
        drawBackground(texture);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
