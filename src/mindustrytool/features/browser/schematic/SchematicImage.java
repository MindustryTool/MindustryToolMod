package mindustrytool.features.browser.schematic;

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
import arc.util.Timer;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustrytool.Main;
import mindustrytool.Config;

public class SchematicImage extends Image {
    public float scaling = 16f;
    public float thickness = 4f;
    public Color borderColor = Pal.gray;

    private String id;
    private TextureRegion lastTexture;
    private final String imageUrl;

    private static ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    public SchematicImage(String id, boolean preview) {
        super(Tex.clear);
        this.id = id;

        StringBuilder sb = new StringBuilder(Config.IMAGE_URL);
        sb.append("schematics/")
                .append(id)
                .append("/image");
        if (preview) {
            sb.append("?variant=preview");
        }
        imageUrl = sb.toString();

        setScaling(Scaling.fit);
    }

    @Override
    public void draw() {
        super.draw();

        var next = textureCache.get(imageUrl);
        if (lastTexture != next && next != null) {
            lastTexture = next;
            setDrawable(next);
        }

        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();

        // textures are only requested when the rendering happens; this assists with
        // culling
        try {
            if (!textureCache.containsKey(imageUrl)) {
                textureCache.put(imageUrl, lastTexture = Core.atlas.find("nomap"));

                var file = Main.schematicDir.child(id + ".png");

                if (file.exists()) {
                    byte[] result = file.readBytes();
                    Pixmap pix = new Pixmap(result);
                    Core.app.post(() -> {
                        try {
                            var tex = new Texture(pix);
                            tex.setFilter(TextureFilter.linear);
                            textureCache.put(imageUrl, new TextureRegion(tex));
                            pix.dispose();
                        } catch (Exception e) {
                            Log.err(id, e);
                        }
                    });
                } else {
                    Http.get(imageUrl)
                            .timeout(60_000)
                            .error(error -> {
                                if (!(error instanceof HttpStatusException requestError)
                                        || requestError.status != HttpStatus.NOT_FOUND) {
                                    Log.err(error);
                                    Timer.schedule(() -> textureCache.remove(imageUrl), 5);
                                }
                            })
                            .submit(res -> {
                                byte[] result = res.getResult();
                                try {
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
                                            textureCache.put(imageUrl, new TextureRegion(tex));
                                            pix.dispose();
                                        } catch (Exception e) {
                                            Log.err(id, e);
                                        }
                                    });
                                } catch (Exception error) {
                                    Log.err(id, error);
                                }

                            });
                }
            }

        } catch (Exception error) {
            Log.err(id, error);
        }
    }
}
