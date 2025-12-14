package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.*;
import arc.scene.ui.Image;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;
import mindustrytool.core.config.Config;
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
        arc.files.Fi file = type.directory.child(id + ".png");
        Vars.mainExecutor.execute(() -> {
            if (file.exists()) loadFile(file, () -> { file.delete(); loadHttp(file); });
            else loadHttp(file);
        });
    }

    private void loadFile(arc.files.Fi file, Runnable onError) {
        try { byte[] d = file.readBytes(); Core.app.post(() -> { try { applyPixmap(new Pixmap(d)); } catch (Exception e) { onError.run(); } }); }
        catch (Exception e) { onError.run(); }
    }

    private void loadHttp(arc.files.Fi file) {
        Http.get(Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview", res -> {
            byte[] d = res.getResult(); if (d == null || d.length < 8) return;
            Core.app.post(() -> { try { applyPixmap(new Pixmap(d)); file.writeBytes(d); } catch (Exception e) { file.delete(); } });
        }, e -> {});
    }

    private void applyPixmap(Pixmap pix) { setDrawable(ImageCache.createAndCache(id, pix)); }

    @Override public void draw() { super.draw(); BorderDrawer.draw(x, y, width, height, borderColor, thickness); }
}
