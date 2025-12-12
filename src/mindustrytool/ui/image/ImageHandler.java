package mindustrytool.ui.image;

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
    private volatile TextureRegion currentTexture;
    private volatile boolean loadStarted = false;
    private final String id;
    private final ImageType type;

    public ImageHandler(String id, ImageType type) {
        super(Tex.clear);
        this.id = id;
        this.type = type;
        setScaling(Scaling.fit);
        currentTexture = ImageCache.get(id);
        setDrawable(currentTexture);
        // Start loading immediately
        startLoad();
    }
    
    private void startLoad() {
        if (loadStarted || ImageCache.has(id)) return;
        loadStarted = true;
        ImageLoader.load(id, type, type.directory.child(id + ".png"), pix -> {
            Core.app.post(() -> {
                TextureRegion region = ImageCache.createAndCache(id, pix);
                currentTexture = region;
                setDrawable(currentTexture);
            });
        });
    }

    @Override public void draw() {
        super.draw();
        
        // Draw border
        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();
    }
}
