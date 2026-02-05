package mindustrytool.ui;

import java.util.concurrent.ConcurrentHashMap;

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
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;

public class NetworkImage extends Image {
    public Color borderColor = Pal.gray;
    public float scaling = 16f;
    public float thickness = 1f;

    private boolean isError = false;
    private String url;
    private TextureRegion lastTexture;

    private static ConcurrentHashMap<String, TextureRegion> cache = new ConcurrentHashMap<>();

    public NetworkImage(String url) {
        super(Tex.clear);
        this.url = url;

        setScaling(Scaling.fit);
    }

    public static boolean isValidImageLink(String url) {
        return url.matches("https?://.*\\.(png|jpg|jpeg)");
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

                if (!url.endsWith("png") && !url.endsWith("jpg") && !url.endsWith("jpeg")) {
                    return;
                }

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
                    Http.get(url + "?format=jpeg")
                            .timeout(10000)
                            .error(error -> {
                                isError = true;
                                if (!(error instanceof HttpStatusException requestError)
                                        || requestError.status != HttpStatus.NOT_FOUND) {
                                    Log.err(url, error);
                                }
                            })
                            .submit(res -> {
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

                            });
                }
            }

        } catch (Exception error) {
            Log.err(url, error);
            isError = true;
        }
    }
}
