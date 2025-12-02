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

// Lớp ImageHandler mới, tập trung vào việc phản ứng với trạng thái (state)
public class ImageHandler extends Image {
    // --- STATE & PROPS (Tương tự như Props và State trong React) ---

    // Props: Định danh và Loại
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
    
    private final ImageType type;
    private final String id;

    // Optional Props: Cấu hình giao diện
    public float thickness = 4f;
    public Color borderColor = Pal.gray;
    
    // State: (Nội bộ, chỉ thay đổi qua việc tải)
    private TextureRegion currentTexture; // Texture hiện tại được render
    private boolean isError = false;      // Đánh dấu lỗi tải vĩnh viễn
    private boolean isLoading = false;    // Đánh dấu đang trong quá trình tải bất đồng bộ

    // Cache dùng chung
    private static final ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    // Placeholder
    private static final TextureRegion PLACEHOLDER = Core.atlas.find("nomap");

    // --- Constructor (Thiết lập Props và Initial State) ---

    public ImageHandler(String id, ImageType type) {
        // Thiết lập Props
        super(Tex.clear);
        this.id = id;
        this.type = type;
        setScaling(Scaling.fit);

        // Initial State
        this.currentTexture = textureCache.get(id);

        // Trigger Effect: Bắt đầu tải nếu chưa có trong Cache
        if (currentTexture == null) {
            startLoading();
        } else {
            // Nếu có trong cache, hiển thị ngay
            setDrawable(currentTexture);
        }
    }

    // --- Effects / Side Effects (Logic Tải Bất Đồng Bộ) ---

    /**
     * Hàm này tương đương với useEffect(() => { ... }, [id, type])
     * Kích hoạt việc tải nếu chưa có trong cache và không bị lỗi.
     */
    private void startLoading() {
        if (isError || isLoading) {
            return; // Đã lỗi hoặc đang tải, không làm gì
        }

        // 1. Đặt State: Bắt đầu tải
        isLoading = true;
        
        // 2. Đặt Placeholder trên UI
        currentTexture = PLACEHOLDER;
        setDrawable(currentTexture);

        // 3. Logic Tải Bất Đồng Bộ
        var file = type.directory.child(id + ".png");

        if (file.exists()) {
            loadFromLocal(file);
        } else {
            loadFromHttp(file);
        }
    }

    private void loadFromLocal(arc.files.Fi file) {
        try {
            byte[] result = file.readBytes();
            Pixmap pix = new Pixmap(result);
            
            // Chuyển sang luồng App để xử lý Texture (Bắt buộc)
            Core.app.post(() -> handleTexture(pix));
        } catch (Exception e) {
            Log.err("Error loading local " + type.name() + " texture: " + id, e);
            setStateError();
        }
    }

    private void loadFromHttp(arc.files.Fi localFile) {
        String url = Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview";

        Http.get(url, 
            // Callback Thành công
            res -> {
                byte[] result = res.getResult();
                if (result.length == 0) {
                    setStateError();
                    return;
                }

                try {
                    Pixmap pix = new Pixmap(result);
                    
                    // Side Effect 1: Ghi file cục bộ (trên Main Executor)
                    Vars.mainExecutor.execute(() -> {
                        try {
                            localFile.writeBytes(result);
                        } catch (Exception error) {
                            Log.err("Error writing local " + type.name() + " file: " + id, error);
                        }
                    });

                    // Side Effect 2: Xử lý Texture trên luồng App
                    Core.app.post(() -> handleTexture(pix));

                } catch (Exception error) {
                    Log.err("Error handling HTTP response for " + type.name() + ": " + id, error);
                    setStateError();
                }
            }, 
            // Callback Lỗi
            error -> {
                if (!(error instanceof HttpStatusException requestError)
                    || requestError.status != HttpStatus.NOT_FOUND) {
                    Log.err("HTTP error loading " + type.name() + ": " + id, error);
                    setStateError();
                } else {
                    Log.debug("HTTP 404 for " + type.name() + ": " + id);
                    // 404 là trường hợp hợp lệ, vẫn kết thúc quá trình tải
                    setStateFinished();
                }
            }
        );
    }
    
    // Xử lý Texture và Cập nhật State thành công
    private void handleTexture(Pixmap pix) {
        try {
            var tex = new Texture(pix);
            tex.setFilter(TextureFilter.linear);
            var region = new TextureRegion(tex);

            // Cập nhật State thành công
            textureCache.put(id, region);
            currentTexture = region;
            setDrawable(currentTexture); // Cập nhật Drawable của Image widget
            
            setStateFinished();
            
            pix.dispose();
        } catch (Exception e) {
            Log.err("Error processing texture: " + id, e);
            setStateError();
            pix.dispose();
        }
    }
    
    // Hàm cập nhật trạng thái chung
    private void setStateError() {
        isLoading = false;
        isError = true;
        // Giữ lại placeholder hoặc texture cuối cùng
    }
    
    private void setStateFinished() {
        isLoading = false;
        // isError được giữ nguyên (false nếu thành công, true nếu 404/lỗi khác)
    }

    // --- Render Logic (Phương thức `draw()` - Tương đương hàm Render trong React) ---

    /**
     * Phương thức này chỉ đơn giản là vẽ những gì đã được thiết lập bởi state/props.
     */
    @Override
    public void draw() {
        // 1. **Vẽ Drawable**: Image widget sẽ tự render currentTexture (đã được setDrawable)
        super.draw();

        // 2. **Vẽ Viền (Dựa trên Props)**: Không phụ thuộc vào state tải
        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();

        // **LƯU Ý**: Toàn bộ logic tải và cập nhật 'drawable' đã được chuyển
        // sang constructor và các hàm 'Effect' (`startLoading`, `handleTexture`).
        // draw() chỉ còn nhiệm vụ "render the current state".
        
        // **Để đảm bảo tính phản ứng (reactivity)**:
        // Nếu muốn tự động thử lại khi hết lỗi, có thể thêm logic
        // để gọi lại `startLoading()` khi `isError` là false.
        // Tuy nhiên, trong Mindustry/Arc, thường logic tải được kích hoạt một lần.
    }
}