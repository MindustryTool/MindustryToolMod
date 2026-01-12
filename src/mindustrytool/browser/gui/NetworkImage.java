package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import arc.Core; // Import Core để truy cập app, graphics, etc.
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
import mindustry.gen.Icon; // Import các icon có sẵn
import mindustry.gen.Tex; // Import các texture có sẵn
import mindustry.graphics.Pal; // Import bảng màu Mindustry
import mindustrytool.browser.BrowserModule; // Import BrowserModule để truy cập thư mục cache

/**
 * Load ảnh từ network với file caching đơn giản và vẽ viền.
 * Class này xử lý HTTP fetch ngầm và lưu ảnh vào `BrowserModule.imageDir`
 * để tránh download lại nhiều lần.
 */
public class NetworkImage extends Image {
    // Màu viền của ảnh, mặc định là xám
    public Color borderColor = Pal.gray;
    // Hệ số scale (chưa sử dụng)
    public float scaling = 16f;
    // Độ dày viền
    public float thickness = 1f;

    // Flag đánh dấu đã có lỗi khi load ảnh
    private boolean isError = false;
    // URL của ảnh cần load
    private String url;
    // Texture region hiện tại đang hiển thị
    private TextureRegion lastTexture;

    // Cache tĩnh lưu trữ texture theo URL, dùng chung cho tất cả instances
    private static ObjectMap<String, TextureRegion> cache = new ObjectMap<>();

    /**
     * Constructor tạo NetworkImage với URL.
     * @param url URL của ảnh cần load
     */
    public NetworkImage(String url) {
        super(Tex.clear); // Khởi tạo với texture trong suốt
        this.url = url; // Lưu URL

        setScaling(Scaling.fit); // Thiết lập chế độ scale: fit vào container
    }

    /**
     * Override phương thức draw để load và hiển thị ảnh từ network.
     * Phương thức này được gọi mỗi frame khi render UI.
     */
    @Override
    public void draw() {
        super.draw(); // Gọi draw của parent class

        // Lấy texture từ cache theo URL
        var next = cache.get(url);

        // Nếu texture thay đổi, cập nhật UI
        if (lastTexture != next) {
            lastTexture = next; // Cập nhật reference
            setDrawable(next); // Thiết lập drawable mới

            // Vẽ viền xung quanh ảnh
            Draw.color(borderColor); // Đặt màu viền
            Lines.stroke(Scl.scl(thickness)); // Đặt độ dày viền (scaled)
            Lines.rect(x, y, width, height); // Vẽ hình chữ nhật viền
            Draw.reset(); // Reset màu về mặc định
        }

        // Nếu đã có lỗi thì không thử load lại
        if (isError) {
            return;
        }

        try {
            // Nếu URL chưa có trong cache, bắt đầu load
            if (!cache.containsKey(url)) {
                // Đặt icon refresh làm placeholder trong khi load
                cache.put(url, Icon.refresh.getRegion());

                // Kiểm tra URL có phải là file ảnh hợp lệ không
                if (!url.endsWith("png") && !url.endsWith("jpg") && !url.endsWith("jpeg")) {
                    return; // Không phải ảnh, bỏ qua
                }

                // Tạo đường dẫn file cache từ URL (thay thế ký tự đặc biệt)
                var file = BrowserModule.imageDir.child(url
                        .replace(":", "-")   // Thay : bằng -
                        .replace("/", "-")   // Thay / bằng -
                        .replace("?", "-")   // Thay ? bằng -
                        .replace("&", "-")); // Thay & bằng -

                // Nếu file đã tồn tại trong cache local
                if (file.exists()) {
                    try {
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
                                cache.put(url, new TextureRegion(tex));
                                // Giải phóng pixmap
                                pix.dispose();
                            } catch (Exception e) {
                                Log.err(url, e); // Log lỗi
                                isError = true; // Đánh dấu lỗi
                            }
                        });
                    } catch (Exception e) {
                        isError = true; // Đánh dấu lỗi
                        file.delete(); // Xóa file bị hỏng
                        Log.err(url, e); // Log lỗi
                    }

                } else {
                    // File không tồn tại, cần download từ network
                    Http.get(url + "?format=jpeg", res -> {
                        // Lấy kết quả bytes
                        byte[] result = res.getResult();
                        // Nếu kết quả rỗng, bỏ qua
                        if (result.length == 0)
                            return;

                        try {
                            // Lưu vào file cache
                            file.writeBytes(result);
                        } catch (Exception error) {
                            Log.err(url, error); // Log lỗi
                            isError = true; // Đánh dấu lỗi
                        }

                        // Post lên main thread để tạo texture
                        Core.app.post(() -> {
                            try {
                                // Tạo Pixmap từ bytes
                                Pixmap pix = new Pixmap(result);
                                // Tạo texture
                                var tex = new Texture(pix);
                                // Áp dụng linear filter
                                tex.setFilter(TextureFilter.linear);
                                // Lưu vào cache
                                cache.put(url, new TextureRegion(tex));
                                // Giải phóng pixmap
                                pix.dispose();

                            } catch (Exception e) {
                                Log.err(url, e); // Log lỗi
                                isError = true; // Đánh dấu lỗi
                            }
                        });

                    }, error -> {
                        // Handler lỗi HTTP
                        isError = true; // Đánh dấu lỗi
                        // Chỉ log nếu không phải lỗi 404 (ảnh không tồn tại là bình thường)
                        if (!(error instanceof HttpStatusException requestError)
                                || requestError.status != HttpStatus.NOT_FOUND) {
                            Log.err(url, error);
                        }
                    });
                }
            }

        } catch (Exception error) {
            // Catch-all cho các lỗi không mong muốn
            Log.err(url, error);
            isError = true;
        }
    }
}
