package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import arc.Core; // Import Core để truy cập atlas, app, etc.
import arc.graphics.Color; // Import class màu sắc
import arc.graphics.Pixmap; // Import Pixmap để xử lý ảnh pixel
import arc.graphics.Texture; // Import Texture để tạo texture từ ảnh
import arc.graphics.Texture.TextureFilter; // Import filter cho texture
import arc.graphics.g2d.Draw; // Import Draw để vẽ
import arc.graphics.g2d.Lines; // Import Lines để vẽ đường
import arc.graphics.g2d.TextureRegion; // Import TextureRegion
import arc.scene.ui.Image; // Import Image widget
import arc.scene.ui.layout.Scl; // Import Scl để scale UI
import arc.struct.ObjectMap; // Import ObjectMap - HashMap của Arc
import arc.util.Http; // Import Http để gọi HTTP requests
import arc.util.Http.HttpStatus; // Import HTTP status codes
import arc.util.Http.HttpStatusException; // Import HTTP exception
import arc.util.Log; // Import Log để ghi log
import arc.util.Scaling; // Import Scaling mode cho ảnh
import mindustry.Vars; // Import Vars để truy cập mainExecutor
import mindustry.gen.Tex; // Import các texture có sẵn
import mindustry.graphics.Pal; // Import bảng màu Mindustry
import mindustrytool.browser.BrowserModule; // Import BrowserModule để truy cập thư mục cache
import mindustrytool.browser.BrowserConfig; // Import cấu hình browser (URLs)

/**
 * Load và render ảnh preview schematic với local file caching.
 * Hoạt động tương tự MapImage, request ảnh variant preview.
 */
public class SchematicImage extends Image {
    // Hệ số scale (chưa sử dụng)
    public float scaling = 16f;
    // Độ dày viền, mặc định 4px
    public float thickness = 4f;
    // Màu viền, mặc định xám
    public Color borderColor = Pal.gray;
    // Flag đánh dấu đã có lỗi khi load
    private boolean isError = false;

    // ID của schematic cần hiển thị ảnh
    private String id;
    // Texture region đang hiển thị
    private TextureRegion lastTexture;

    // Cache tĩnh lưu texture theo schematic ID
    private static ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    /**
     * Constructor tạo SchematicImage với schematic ID.
     * @param id ID của schematic
     */
    public SchematicImage(String id) {
        super(Tex.clear); // Khởi tạo với texture trong suốt
        this.id = id; // Lưu schematic ID
        setScaling(Scaling.fit); // Thiết lập chế độ scale: fit vào container
    }

    /**
     * Override phương thức draw để load và hiển thị ảnh schematic.
     * Được gọi mỗi frame khi render UI.
     */
    @Override
    public void draw() {
        super.draw(); // Gọi draw của parent class

        // Lấy texture từ cache theo ID
        var next = textureCache.get(id);
        // Nếu texture thay đổi, cập nhật drawable
        if (lastTexture != next) {
            lastTexture = next;
            setDrawable(next);
        }

        // Vẽ viền xung quanh ảnh
        Draw.color(borderColor); // Đặt màu viền
        Lines.stroke(Scl.scl(thickness)); // Đặt độ dày viền (scaled)
        Lines.rect(x, y, width, height); // Vẽ hình chữ nhật viền
        Draw.reset(); // Reset màu về mặc định

        // Nếu đã có lỗi thì không thử load lại
        if (isError) {
            return;
        }

        // Textures chỉ được request khi render; điều này hỗ trợ culling
        // (không load ảnh cho các item ngoài viewport)
        try {
            // Nếu ID chưa có trong cache, bắt đầu load
            if (!textureCache.containsKey(id)) {
                // Đặt placeholder "nomap" trong khi load
                textureCache.put(id, lastTexture = Core.atlas.find("nomap"));

                // Tạo đường dẫn file cache
                var file = BrowserModule.schematicDir.child(id + ".png");

                // Nếu file đã tồn tại trong cache local
                if (file.exists()) {
                    // Đọc bytes từ file
                    byte[] result = file.readBytes();
                    // Tạo Pixmap từ bytes
                    Pixmap pix = new Pixmap(result);
                    // Post lên main thread để tạo texture (OpenGL yêu cầu)
                    Core.app.post(() -> {
                        try {
                            // Tạo texture từ pixmap
                            var tex = new Texture(pix);
                            // Áp dụng linear filter để mượt hơn
                            tex.setFilter(TextureFilter.linear);
                            // Lưu vào cache
                            textureCache.put(id, new TextureRegion(tex));
                            // Giải phóng pixmap
                            pix.dispose();
                        } catch (Exception e) {
                            Log.err(id, e); // Log lỗi
                            isError = true; // Đánh dấu lỗi
                        }
                    });
                } else {
                    // File không tồn tại, download từ server
                    Http.get(BrowserConfig.IMAGE_URL + "schematics/" + id + ".png?variant=preview", res -> {
                        // Lấy kết quả bytes
                        byte[] result = res.getResult();
                        try {
                            // Nếu rỗng, bỏ qua
                            if (result.length == 0)
                                return;

                            // Tạo Pixmap từ bytes
                            Pixmap pix = new Pixmap(result);

                            // Lưu file cache trên background thread
                            Vars.mainExecutor.execute(() -> {
                                try {
                                    file.writeBytes(result); // Ghi vào file
                                } catch (Exception error) {
                                    Log.err(id, error); // Log lỗi
                                }
                            });

                            // Post lên main thread để tạo texture
                            Core.app.post(() -> {
                                try {
                                    // Tạo texture từ pixmap
                                    var tex = new Texture(pix);
                                    // Áp dụng linear filter
                                    tex.setFilter(TextureFilter.linear);
                                    // Lưu vào cache
                                    textureCache.put(id, new TextureRegion(tex));
                                    // Giải phóng pixmap
                                    pix.dispose();
                                } catch (Exception e) {
                                    Log.err(id, e); // Log lỗi
                                    isError = true; // Đánh dấu lỗi
                                }
                            });
                        } catch (Exception error) {
                            // Lỗi xử lý response
                            Log.err(id, error);
                            isError = true;
                        }

                    }, error -> {
                        // Handler lỗi HTTP - chỉ log nếu không phải 404
                        if (!(error instanceof HttpStatusException requestError)
                                || requestError.status != HttpStatus.NOT_FOUND) {
                            Log.err(error);
                            isError = true;
                        }
                    });
                }
            }

        } catch (Exception error) {
            // Catch-all cho các lỗi không mong muốn
            Log.err(id, error);
            isError = true;
        }
    }
}
