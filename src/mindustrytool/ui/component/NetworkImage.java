package mindustrytool.ui.component;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustrytool.ui.image.NetworkImageLoader;

public class NetworkImage extends Image {
    public Color borderColor = Pal.gray;
    public float scaling = 16f, thickness = 1f;
    private boolean isError = false;
    private String url;
    private TextureRegion lastTexture;
    private static ObjectMap<String, TextureRegion> cache = new ObjectMap<>(128);

    public NetworkImage(String url) { super(Tex.clear); this.url = url; setScaling(Scaling.fit); }

    @Override public void draw() {
        super.draw();
        TextureRegion next = cache.get(url);
        if (lastTexture != next) { lastTexture = next; setDrawable(next); Draw.color(borderColor); Lines.stroke(Scl.scl(thickness)); Lines.rect(x, y, width, height); Draw.reset(); }
        if (isError) return;
        try {
            if (!cache.containsKey(url)) NetworkImageLoader.load(url, cache, () -> isError = true);
        } catch (Exception error) { Log.err(url, error); isError = true; }
    }
}
