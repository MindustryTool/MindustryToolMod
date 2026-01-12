package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import java.util.HashMap; // Import HashMap để cache dữ liệu

import arc.Core; // Import Core để post lên main thread
import arc.func.Cons; // Import interface callback function
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.util.Http; // Import Http để gọi HTTP requests
import arc.util.Http.HttpResponse; // Import HttpResponse để xử lý kết quả
import arc.util.Log; // Import Log để ghi log lỗi
import mindustry.io.JsonIO; // Import JsonIO để parse JSON
import mindustrytool.browser.BrowserConfig; // Import BrowserConfig để lấy API URL

/**
 * Service cache và load tag categories từ API.
 * Cung cấp callback mechanism đơn giản và update hook.
 */
public class TagService { // Class quản lý việc load và cache tags

    /**
     * Enum định nghĩa các loại tag category.
     */
    public enum TagCategoryEnum {
        schematics, // Tag cho schematics
        maps // Tag cho maps
    }

    // Callback được gọi khi có update
    private Runnable onUpdate = () -> {};
    // Cache static để lưu categories đã load
    private static HashMap<String, Seq<TagCategory>> categories = new HashMap<>();

    /**
     * Lấy tag categories cho loại category cụ thể.
     * Sử dụng cache nếu có, không thì fetch từ API.
     * @param category Loại category (schematics/maps)
     * @param listener Callback nhận kết quả
     */
    public void getTag(TagCategoryEnum category, Cons<Seq<TagCategory>> listener) {
        // Kiểm tra cache
        var item = categories.get(category.name());

        // Nếu có trong cache, trả về ngay
        if (item != null) {
            Core.app.post(() -> listener.get(item)); // Post về main thread
            return;
        }

        // Không có trong cache, fetch từ API
        getTagData(category, (tags) -> {
            categories.put(category.name(), tags); // Lưu vào cache
            Core.app.post(() -> listener.get(tags)); // Post về main thread
        });
    }

    /**
     * Fetch tag data từ API.
     * @param category Loại category
     * @param listener Callback nhận kết quả
     */
    private void getTagData(TagCategoryEnum category, Cons<Seq<TagCategory>> listener) {
        // Gọi HTTP GET tới endpoint /tags?group=category
        Http.get(BrowserConfig.API_URL + "tags" + "?group=" + category)
                .error(error -> handleError(listener, error, BrowserConfig.API_URL + "tags")) // Handler lỗi
                .submit(response -> handleResult(response, listener)); // Handler kết quả
    }

    /**
     * Xử lý lỗi HTTP request.
     * @param listener Callback để thông báo
     * @param error Exception đã xảy ra
     * @param url URL gây lỗi (để log)
     */
    public void handleError(Cons<Seq<TagCategory>> listener, Throwable error, String url) {
        Log.err(url, error); // Log lỗi
        Core.app.post(() -> listener.get(new Seq<>())); // Trả về list rỗng
    }

    /**
     * Xử lý kết quả HTTP response.
     * Parse JSON và thông báo listener.
     * @param response HTTP response
     * @param listener Callback nhận kết quả
     */
    @SuppressWarnings("unchecked") // Suppress warning cho cast generic
    private void handleResult(HttpResponse response, Cons<Seq<TagCategory>> listener) {
        String data = response.getResultAsString(); // Lấy body dạng String
        // Parse JSON thành Seq<TagCategory>
        Seq<TagCategory> tags = JsonIO.json.fromJson(Seq.class, TagCategory.class, data);
        // Post về main thread
        Core.app.post(() -> {
            listener.get(tags); // Gọi callback với kết quả
            onUpdate.run(); // Gọi update hook
        });
    }

    /**
     * Đăng ký callback khi có update.
     * @param callback Runnable được gọi sau khi load xong
     */
    public void onUpdate(Runnable callback) {
        onUpdate = callback; // Gán callback
    }
}
