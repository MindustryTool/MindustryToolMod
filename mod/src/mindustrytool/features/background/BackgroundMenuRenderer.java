package mindustrytool.features.background;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Nullable;
import mindustry.graphics.MenuRenderer;
import solim.config.ConfigValue;

public abstract class BackgroundMenuRenderer extends MenuRenderer {

    protected final MenuRenderer originalRenderer;
    protected final ConfigValue<Integer> opacityConfig;
    protected final ConfigValue<String> scalingConfig;
    protected final TextureRegion region = new TextureRegion();

    protected BackgroundMenuRenderer(
            @Nullable MenuRenderer originalRenderer,
            ConfigValue<Integer> opacityConfig,
            ConfigValue<String> scalingConfig) {
        this.originalRenderer = originalRenderer;
        this.opacityConfig = opacityConfig;
        this.scalingConfig = scalingConfig;

        // MenuRenderer builds the default 3D menu scene in its constructor. Custom backgrounds
        // override render() and never use those resources, so release them right away instead of
        // leaking a cache plus framebuffer for every background that gets applied.
        super.dispose();
    }

    public void update() {
    }

    public boolean isVideo() {
        return false;
    }

    public boolean blocksMenuMusic() {
        return false;
    }

    public void setVolume(int volume) {
    }

    protected final void drawBackground(@Nullable Texture texture) {
        if (texture == null) {
            drawOriginal();
            return;
        }

        Integer configured = opacityConfig.get();
        int opacity = configured != null ? configured : 100;

        if (opacity < 100) {
            drawOriginal();
        }

        float screenWidth = Core.graphics.getWidth();
        float screenHeight = Core.graphics.getHeight();
        boolean crop = BackgroundScaling.isCrop(scalingConfig.get());

        try {
            region.set(texture);
            Draw.reset();
            if (opacity < 100) {
                Draw.alpha(opacity / 100f);
            }
            Draw.rect(region, screenWidth / 2f, screenHeight / 2f,
                    BackgroundScaling.drawWidth(texture.width, texture.height, screenWidth, screenHeight, crop),
                    BackgroundScaling.drawHeight(texture.width, texture.height, screenWidth, screenHeight, crop));
        } finally {
            Draw.reset();
        }
    }

    protected final void drawOriginal() {
        if (originalRenderer != null) {
            originalRenderer.render();
        }
    }
}