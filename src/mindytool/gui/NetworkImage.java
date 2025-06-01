package mindytool.gui;

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
import arc.struct.ObjectMap;
import arc.util.Http;
import arc.util.Http.HttpStatus;
import arc.util.Http.HttpStatusException;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindytool.Main;

public class NetworkImage extends Image {
    public Color borderColor = Pal.gray;
    public float scaling = 16f;
    public float thickness = 1f;

    private boolean isError = false;
    private String url;
    private TextureRegion lastTexture;

    private static ObjectMap<String, TextureRegion> cache = new ObjectMap<>();

    public NetworkImage(String url) {
        super(Tex.clear);
        this.url = url;

        setScaling(Scaling.fit);
    }

    @Override
    public void draw() {
        super.draw();

        var next = cache.get(url);

        if (lastTexture != next) {
            lastTexture = next;
            setDrawable(next);

            Draw.color(borderColor);
            Lines.stroke(Scl.scl(thickness));
            Lines.rect(x, y, width, height);
            Draw.reset();
        }

        if (isError) {
            return;
        }

        try {
            if (!cache.containsKey(url)) {
                cache.put(url, Icon.refresh.getRegion());

                var file = Main.imageDir.child(url//
                        .replace(":", "-")//
                        .replace("/", "-")//
                        .replace("?", "-")//
                        .replace("&", "-"));

                if (file.exists()) {
                    try {
                        byte[] result = file.readBytes();
                        Pixmap pix = new Pixmap(result);
                        Core.app.post(() -> {
                            try {
                                var tex = new Texture(pix);
                                tex.setFilter(TextureFilter.linear);
                                cache.put(url, new TextureRegion(tex));
                                pix.dispose();
                            } catch (Exception e) {
                                Log.err(url, e);
                                isError = true;
                            }
                        });
                    } catch (Exception e) {
                        isError = true;
                        file.delete();
                        Log.err(url, e);
                    }

                } else {
                    Http.get(url + "?format=jpeg", res -> {
                        byte[] result = res.getResult();
                        if (result.length == 0)
                            return;

                        try {
                            file.writeBytes(result);
                        } catch (Exception error) {
                            Log.err(url, error);
                            isError = true;
                        }

                        Core.app.post(() -> {
                            try {
                                Pixmap pix = new Pixmap(result);
                                var tex = new Texture(pix);
                                tex.setFilter(TextureFilter.linear);
                                cache.put(url, new TextureRegion(tex));
                                pix.dispose();

                            } catch (Exception e) {
                                Log.err(url, e);
                                isError = true;
                            }
                        });

                    }, error -> {
                        isError = true;
                        if (!(error instanceof HttpStatusException requestError)
                                || requestError.status != HttpStatus.NOT_FOUND) {
                            Log.err(url, error);
                        }
                    });
                }
            }

        } catch (Exception error) {
            Log.err(url, error);
            isError = true;
        }
    }
}
