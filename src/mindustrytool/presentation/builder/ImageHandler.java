package mindustrytool.presentation.builder;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.util.Scaling;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;

public class ImageHandler extends Image {
    public enum ImageType {
        SCHEMATIC("schematics", Main.schematicDir), MAP("maps", Main.mapsDir);
        public final String urlSegment;
        public final arc.files.Fi directory;
        ImageType(String u, arc.files.Fi d) { urlSegment = u; directory = d; }
    }

    public float thickness = 4f;
    public Color borderColor = Pal.gray;
    private TextureRegion currentTexture;

    public ImageHandler(String id, ImageType type) {
        super(Tex.clear);
        setScaling(Scaling.fit);
        currentTexture = ImageCache.get(id);
        setDrawable(currentTexture);
        if (!ImageCache.has(id)) ImageLoader.load(id, type, type.directory.child(id + ".png"), pix -> Core.app.post(() -> {
            currentTexture = ImageCache.createAndCache(id, pix);
            setDrawable(currentTexture);
        }));
    }

    @Override public void draw() {
        super.draw();
        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();
    }
}