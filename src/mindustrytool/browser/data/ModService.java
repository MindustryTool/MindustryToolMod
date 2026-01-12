package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.Core; // Import Core để post lên main thread
import arc.func.Cons; // Import interface callback function
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.util.Http; // Import Http để gọi HTTP requests
import arc.util.Http.HttpResponse; // Import HttpResponse để xử lý kết quả
import arc.util.Log; // Import Log để ghi log lỗi
import mindustry.io.JsonIO; // Import JsonIO để parse JSON
import mindustrytool.browser.BrowserConfig; // Import BrowserConfig để lấy API URL

/**
 * Service cache và cung cấp danh sách mods có sẵn.
 * Dùng bởi filter dialog để cho user lọc theo mod nguồn gốc.
 */
public class ModService { // Class quản lý việc load và cache mods

    // Callback được gọi khi có update
    private Runnable onUpdate = () -> {};
    // Cache static để lưu mods đã load
    private static Seq<DataMod> mods = new Seq<>();

    /**
     * Lấy danh sách mods.
     * Sử dụng cache nếu có, không thì fetch từ API.
     * @param listener Callback nhận kết quả
     */
    public void getMod(Cons<Seq<DataMod>> listener) {
        // Nếu cache rỗng, fetch từ API
        if (mods.isEmpty()) {
            getModData((modsData) -> {
                mods = modsData; // Lưu vào cache
                Core.app.post(() -> listener.get(mods)); // Post về main thread
            });
        } else {
            // Có trong cache, trả về ngay
            Core.app.post(() -> listener.get(mods)); // Post về main thread
        }
    }

    /**
     * Fetch mod data từ API.
     * @param listener Callback nhận kết quả
     */
    private void getModData(Cons<Seq<DataMod>> listener) {
        // Gọi HTTP GET tới endpoint /planets (mods = planets trong API)
        Http.get(BrowserConfig.API_URL + "planets")
                .error(error -> handleError(listener, error, BrowserConfig.API_URL + "planets")) // Handler lỗi
                .submit(response -> handleResult(response, listener)); // Handler kết quả
    }

    /**
     * Xử lý lỗi HTTP request.
     * @param listener Callback để thông báo
     * @param error Exception đã xảy ra
     * @param url URL gây lỗi (để log)
     */
    public void handleError(Cons<Seq<DataMod>> listener, Throwable error, String url) {
        Log.err(url, error); // Log lỗi
        Core.app.post(() -> listener.get(new Seq<>())); // Trả về list rỗng
    }

    /**
     * Xử lý kết quả HTTP response.
     * Parse JSON và thông báo listener.
     * @param response HTTP response
     * @param listener Callback nhận kết quả
     */
    private void handleResult(HttpResponse response, Cons<Seq<DataMod>> listener) {
        String data = response.getResultAsString(); // Lấy body dạng String
        // Parse JSON thành Seq<DataMod>
        @SuppressWarnings("unchecked") // Suppress warning cho cast generic
        Seq<DataMod> mods = JsonIO.json.fromJson(Seq.class, DataMod.class, data);

        // Post về main thread
        Core.app.post(() -> {
            listener.get(mods); // Gọi callback với kết quả
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
