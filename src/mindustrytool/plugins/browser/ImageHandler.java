package mindustrytool.plugins.browser;

import arc.Core;
import arc.files.Fi;
import arc.graphics.*;
import arc.scene.ui.Image;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;

public class ImageHandler extends Image {
    public enum ImageType {
        SCHEMATIC("schematics", BrowserDirInit.schematicDir), 
        MAP("maps", BrowserDirInit.mapsDir);
        
        public final String urlSegment; 
        public final Fi directory;
        
        ImageType(String u, Fi d) { urlSegment = u; directory = d; }
    }

    public float thickness = 4f;
    public Color borderColor = Pal.gray;
    private volatile boolean loadStarted;
    private final String id;
    private final ImageType type;

    public ImageHandler(String id, ImageType type) {
        super(Tex.clear); 
        this.id = id; 
        this.type = type;
        setScaling(Scaling.fit); 
        setDrawable(ImageCache.get(id)); 
        startLoad();
    }

    private void startLoad() {
        if (loadStarted || ImageCache.has(id)) return;
        loadStarted = true;
        Fi file = type.directory.child(id + ".png");
        Vars.mainExecutor.execute(() -> {
            if (file.exists()) loadFile(file, () -> { file.delete(); loadHttp(file); });
            else loadHttp(file);
        });
    }

    private void loadFile(Fi file, Runnable onError) {
        try { 
            byte[] d = file.readBytes(); 
            Core.app.post(() -> { 
                try { applyPixmap(new Pixmap(d)); } 
                catch (Exception e) { onError.run(); } 
            }); 
        } catch (Exception e) { onError.run(); }
    }

    private void loadHttp(Fi file) {
        Http.get(Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview", res -> {
            byte[] d = res.getResult(); 
            if (d == null || d.length < 8) return;
            Core.app.post(() -> { 
                try { 
                    applyPixmap(new Pixmap(d)); 
                    file.writeBytes(d); 
                } catch (Exception e) { 
                    file.delete(); 
                } 
            });
        }, e -> {});
    }

    private void applyPixmap(Pixmap pix) { 
        setDrawable(ImageCache.createAndCache(id, pix)); 
    }

    @Override 
    public void draw() { 
        super.draw(); 
        BorderDrawer.draw(x, y, width, height, borderColor, thickness); 
    }
}
