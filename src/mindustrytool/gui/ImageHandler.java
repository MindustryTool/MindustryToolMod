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
import arc.struct.ObjectMap;
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

// Lớp ImageHandler mới, thay thế cho SchematicImage và MapImage
public class ImageHandler extends Image {
    // Enum để xác định loại tài nguyên
    public enum ImageType {
        SCHEMATIC("schematics", Main.schematicDir),
        MAP("maps", Main.mapsDir);

        public final String urlSegment;
        public final arc.files.Fi directory;

        ImageType(String urlSegment, arc.files.Fi directory) {
            this.urlSegment = urlSegment;
            this.directory = directory;
        }
    }

    // Các thuộc tính chung
    public float scaling = 16f;
    public float thickness = 4f;
    public Color borderColor = Pal.gray;
    
    // Thuộc tính riêng cho ImageHandler
    private final ImageType type;
    private final String id;
    private boolean isError = false; // Giữ lại cơ chế xử lý lỗi mạnh mẽ từ SchematicImage
    private TextureRegion lastTexture;

    // Cache dùng chung cho cả Schematics và Maps
    private static final ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    /**
     * Khởi tạo ImageHandler.
     * @param id ID của tài nguyên (schematic ID hoặc map ID).
     * @param type Loại tài nguyên (SCHEMATIC hoặc MAP).
     */
    public ImageHandler(String id, ImageType type) {
        super(Tex.clear);
        this.id = id;
        this.type = type;
        setScaling(Scaling.fit);
    }

    @Override
    public void draw() {
        super.draw();

        // 1. Cập nhật TextureRegion
        var next = textureCache.get(id);
        if (lastTexture != next) {
            lastTexture = next;
            setDrawable(next);
        }

        // 2. Vẽ viền (Vị trí này tương tự SchematicImage, vẽ trước logic tải)
        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();

        // 3. Kiểm tra lỗi và thoát sớm
        if (isError) {
            return;
        }

        // 4. Logic Tải Texture (chỉ khi chưa có trong cache)
        try {
            if (!textureCache.containsKey(id)) {
                // Đặt placeholder 'nomap' và 'lastTexture'
                textureCache.put(id, lastTexture = Core.atlas.find("nomap"));

                var file = type.directory.child(id + ".png");

                // Thử tải từ file cục bộ
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
                            Log.err("Error loading local " + type.name() + " texture: " + id, e);
                            isError = true;
                        }
                    });
                } 
                // Tải qua HTTP nếu không có file cục bộ
                else {
                    String url = Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview";
                    Http.get(url, res -> {
                        byte[] result = res.getResult();
                        try {
                            if (result.length == 0) return;

                            Pixmap pix = new Pixmap(result);

                            // Ghi file cục bộ (trên Main Executor)
                            Vars.mainExecutor.execute(() -> {
                                try {
                                    file.writeBytes(result);
                                } catch (Exception error) {
                                    Log.err("Error writing local " + type.name() + " file: " + id, error);
                                }
                            });

                            // Xử lý Texture (trên App thread)
                            Core.app.post(() -> {
                                try {
                                    var tex = new Texture(pix);
                                    tex.setFilter(TextureFilter.linear);
                                    textureCache.put(id, new TextureRegion(tex));
                                    pix.dispose();
                                } catch (Exception e) {
                                    Log.err("Error processing HTTP " + type.name() + " texture: " + id, e);
                                    isError = true;
                                }
                            });
                        } catch (Exception error) {
                            Log.err("Error handling HTTP response for " + type.name() + ": " + id, error);
                            isError = true;
                        }
                    }, error -> {
                        if (!(error instanceof HttpStatusException requestError)
                                || requestError.status != HttpStatus.NOT_FOUND) {
                            Log.err("HTTP error loading " + type.name() + ": " + id, error);
                            isError = true;
                        } else {
                            // NOT_FOUND là trường hợp hợp lệ, không set isError
                            // Có thể log ở mức debug
                            Log.debug("HTTP 404 for " + type.name() + ": " + id);
                        }
                    });
                }
            }
        } catch (Exception error) {
            Log.err("General error in " + type.name() + " draw logic: " + id, error);
            isError = true;
        }
    }
}