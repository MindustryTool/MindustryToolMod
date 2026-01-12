package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import java.net.URI; // Import URI để xử lý đường dẫn

import org.apache.http.client.utils.URIBuilder; // Import URIBuilder để build URL với params

import arc.Core; // Import Core để post lên main thread
import arc.func.Cons; // Import interface callback function
import arc.struct.ObjectMap; // Import ObjectMap - HashMap của Arc
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.struct.ObjectMap.Entry; // Import Entry để duyệt ObjectMap
import arc.util.Http; // Import Http để gọi HTTP requests
import arc.util.Http.HttpResponse; // Import HttpResponse để xử lý kết quả
import arc.util.Http.HttpStatus; // Import HTTP status codes
import arc.util.Log; // Import Log để ghi log
import mindustry.io.JsonIO; // Import JsonIO để parse JSON

/**
 * Helper đơn giản để thực hiện paged GET requests tới server API.
 * Theo dõi trạng thái loading / error / hasMore và thực hiện request theo yêu cầu.
 */
public class PagingRequest<T> {

    // Flag volatile để thread-safe, đánh dấu đang loading
    private volatile boolean isLoading = false;
    // Flag có thêm data ở trang tiếp theo không
    private boolean hasMore = true;
    // Flag có lỗi không
    private boolean isError = false;
    // Thông báo lỗi
    private String error = "";

    // Số items mỗi trang, mặc định 20
    private int size = 20;

    // Trang hiện tại, bắt đầu từ 0
    private int page = 0;

    // Base URL của API endpoint
    private final String url;
    // Class type để parse JSON
    private final Class<T> clazz;

    // Map chứa các query parameters tùy chọn
    private ObjectMap<String, String> options = new ObjectMap<>();

    /**
     * Constructor tạo PagingRequest.
     * @param clazz Class type của item (để parse JSON)
     * @param url Base URL của API endpoint
     */
    public PagingRequest(Class<T> clazz, String url) {
        this.url = url; // Lưu URL
        this.clazz = clazz; // Lưu class type
    }

    /**
     * Request trang hiện tại từ server và gọi listener với kết quả đã parse.
     * Listener nhận null khi bắt đầu loading hoặc có lỗi.
     * @param listener Callback nhận kết quả
     */
    public synchronized void getPage(Cons<Seq<T>> listener) {
        // Nếu đang loading, không request lại
        if (isLoading)
            return;

        // Reset trạng thái lỗi
        isError = false;
        // Đánh dấu đang loading
        isLoading = true;

        try {
            // Build URL với query parameters
            URIBuilder builder = new URIBuilder(url)
                    .setParameter("page", String.valueOf(page)) // Thêm param page
                    .setParameter("size", String.valueOf(Math.min(size, 100))); // Thêm param size (max 100)

            // Thêm các options khác vào URL
            for (Entry<String, String> entry : options.entries())
                if (entry.value != null && !entry.value.isEmpty()) {
                    builder.setParameter(entry.key, entry.value);
                }

            // Build URI hoàn chỉnh
            URI uri = builder.build();
            // Thông báo listener bắt đầu loading (null = loading)
            listener.get(null);

            // Log URL để debug
            Log.info(uri);

            // Thực hiện HTTP GET request
            Http.get(uri.toString())
                    .timeout(1000 * 5) // Timeout 5 giây
                    .error(e -> handleError(listener, e, uri.toString())) // Handler lỗi
                    .submit(response -> handleResult(response, size, listener)); // Handler kết quả

        } catch (Exception e) {
            // Xử lý lỗi khi build URL
            handleError(listener, e, url);
        }
    }

    /**
     * Xử lý lỗi HTTP request.
     * @param listener Callback để thông báo
     * @param e Exception đã xảy ra
     * @param url URL gây lỗi (để log)
     */
    public synchronized void handleError(Cons<Seq<T>> listener, Throwable e, String url) {
        Log.err(url, e); // Log lỗi với URL
        error = e.getMessage(); // Lưu thông báo lỗi

        isLoading = false; // Không còn loading
        isError = true; // Đánh dấu có lỗi

        listener.get(null); // Thông báo listener (null = lỗi)
    }

    /**
     * Đặt trang hiện tại.
     * @param page Số trang (0-indexed)
     */
    public synchronized void setPage(int page) {
        this.page = page;
    }

    /**
     * Đặt các query parameters tùy chọn.
     * @param options Map chứa key-value của params
     */
    public synchronized void setOptions(ObjectMap<String, String> options) {
        this.options = options;
    }

    /**
     * Lấy số items mỗi trang.
     * @return Số items/page
     */
    public synchronized int getItemPerPage() {
        return size;
    }

    /**
     * Đặt số items mỗi trang.
     * @param size Số items/page
     */
    public synchronized void setItemPerPage(int size) {
        this.size = size;
    }

    /**
     * Kiểm tra còn trang tiếp theo không.
     * @return true nếu còn data
     */
    public synchronized boolean hasMore() {
        return hasMore;
    }

    /**
     * Kiểm tra đang loading không.
     * @return true nếu đang request
     */
    public synchronized boolean isLoading() {
        return isLoading;
    }

    /**
     * Kiểm tra có lỗi không.
     * @return true nếu có lỗi
     */
    public synchronized boolean isError() {
        return isError;
    }

    /**
     * Lấy thông báo lỗi.
     * @return Thông báo lỗi hoặc rỗng
     */
    public synchronized String getError() {
        return error;
    }

    /**
     * Lấy số trang hiện tại.
     * @return Số trang (0-indexed)
     */
    public synchronized int getPage() {
        return page;
    }

    /**
     * Chuyển sang trang tiếp theo và request.
     * @param listener Callback nhận kết quả
     */
    public synchronized void nextPage(Cons<Seq<T>> listener) {
        // Nếu đang loading, bỏ qua
        if (isLoading)
            return;

        // Nếu còn data, tăng page
        if (hasMore) {
            page++;
        }

        // Request trang mới
        getPage(listener);
    }

    /**
     * Chuyển về trang trước và request.
     * @param listener Callback nhận kết quả
     */
    public synchronized void previousPage(Cons<Seq<T>> listener) {
        // Nếu đang loading, bỏ qua
        if (isLoading)
            return;

        // Nếu không phải trang đầu, giảm page
        if (page > 0) {
            page--;
        }

        // Request trang mới
        getPage(listener);
    }

    /**
     * Xử lý kết quả HTTP response.
     * Parse JSON và thông báo listener.
     * @param response HTTP response
     * @param size Số items mong đợi
     * @param listener Callback nhận kết quả
     */
    @SuppressWarnings("unchecked") // Suppress warning cho cast generic
    private synchronized void handleResult(HttpResponse response, int size, Cons<Seq<T>> listener) {
        // Không còn loading
        isLoading = false;
        // Reset lỗi
        isError = false;

        // Nếu response không OK, đánh dấu lỗi
        if (response.getStatus() != HttpStatus.OK) {
            isError = true;
            error = response.getResultAsString(); // Lấy body làm thông báo lỗi
            listener.get(new Seq<>()); // Trả về list rỗng
            return;
        }

        // Lấy body dạng String
        String data = response.getResultAsString();
        // Post lên main thread để parse và thông báo
        Core.app.post(() -> {
            // Parse JSON thành Seq<T>
            var items = JsonIO.json.fromJson(Seq.class, clazz, data);

            // Nếu null, tạo Seq rỗng
            if (items == null) {
                items = new Seq<>();
            }

            // Nếu trả về rỗng, không còn data
            hasMore = items.size != 0;

            // Thông báo listener với kết quả
            listener.get(items);
        });
    }
}
