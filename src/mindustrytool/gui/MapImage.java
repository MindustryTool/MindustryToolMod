package mindustrytool.gui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.util.Http;
import arc.util.Http.HttpStatus;
import arc.util.Http.HttpStatusException;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;
import mindustrytool.config.Config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class MapImage extends Image {
    public float scaling = 16f;
    public float thickness = 4f;
    public Color borderColor = Pal.gray;

    private String id;
    private TextureRegion lastTexture;

    private static Cache<String, TextureRegion> textureCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public MapImage(String id) {
        super(Tex.clear);
        this.id = id;

        setScaling(Scaling.fit);
    }

    @Override
    public void draw() {
        super.draw();

        try {
            if (textureCache.getIfPresent(id) == null) {
                textureCache.put(id, lastTexture = Core.atlas.find("nomap"));
                var file = Main.mapsDir.child(id + ".jpg");

                if (file.exists()) {
                    byte[] result = file.readBytes();
                    Pixmap pix = new Pixmap(result);
                    Core.app.post(() -> {
                        try {
                            var tex = new Texture(pix);
                            tex.setFilter(TextureFilter.linear);
                            textureCache.put(id, new TextureRegion(tex));
                            pix.dispose();
                        } catch (Exception e) {
                            Log.err(id, e);
                        }
                    });

                } else {
                    Http.get(Config.IMAGE_URL + "maps/" + id + ".jpg?variant=preview", res -> {
                        byte[] result = res.getResult();
                        if (result.length == 0)
                            return;

                        Pixmap pix = new Pixmap(result);
                        Vars.mainExecutor.execute(() -> {
                            try {
                                file.writeBytes(result);

                            } catch (Exception error) {
                                Log.err(id, error);
                            }
                        });

                        Core.app.post(() -> {
                            try {
                                var tex = new Texture(pix);
                                tex.setFilter(TextureFilter.linear);
                                textureCache.put(id, new TextureRegion(tex));
                                pix.dispose();
                            } catch (Exception e) {
                                Log.err(id, e);
                            }
                        });

                    }, error -> {
                        if (!(error instanceof HttpStatusException requestError)
                                || requestError.status != HttpStatus.NOT_FOUND) {
                            Log.err(id, error);
                        }
                    });
                }
            }

            var next = textureCache.getIfPresent(id);
            if (lastTexture != next) {
                lastTexture = next;
                setDrawable(next);
            }

            Draw.color(borderColor);
            Lines.stroke(Scl.scl(thickness));
            Lines.rect(x, y, width, height);
            Draw.reset();

        } catch (Exception error) {
            Log.err(id, error);
        }
    }
}
