package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.util.Scaling;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;
import mindustrytool.core.util.BorderDrawer;

public class ImageHandler extends Image {
    public enum ImageType {
        SCHEMATIC("schematics", Main.schematicDir), MAP("maps", Main.mapsDir);
        public final String urlSegment; public final arc.files.Fi directory;
        ImageType(String u, arc.files.Fi d) { urlSegment = u; directory = d; }
    }

    public float thickness = 4f;
    public Color borderColor = Pal.gray;
    private volatile boolean loadStarted;
    private final String id;
    private final ImageType type;

    public ImageHandler(String id, ImageType type) {
        super(Tex.clear); this.id = id; this.type = type;
        setScaling(Scaling.fit); setDrawable(ImageCache.get(id)); startLoad();
    }

    private void startLoad() {
        if (loadStarted || ImageCache.has(id)) return;
        loadStarted = true;
        ImageLoader.load(id, type, type.directory.child(id + ".png"), pix -> Core.app.post(() -> setDrawable(ImageCache.createAndCache(id, pix))));
    }

    @Override public void draw() { super.draw(); BorderDrawer.draw(x, y, width, height, borderColor, thickness); }
}
