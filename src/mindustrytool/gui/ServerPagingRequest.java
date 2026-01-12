// Khai báo package cho module GUI
package mindustrytool.gui;

// Import Cons là functional interface nhận 1 tham số
import arc.func.Cons;
// Import Seq là collection của Arc framework
import arc.struct.Seq;
// Import Core để chạy code trên UI thread
import arc.Core;
// Import Http để thực hiện HTTP request
import arc.util.Http;
// Import HttpResponse để xử lý response
import arc.util.Http.HttpResponse;
// Import Log để ghi log
import arc.util.Log;
// Import JsonIO để parse JSON
import mindustry.io.JsonIO;

/**
 * PagingRequest dành riêng cho ServerDialog (giữ tách biệt khỏi browser module)
 */
// Lớp generic để xử lý request phân trang
public class ServerPagingRequest<T> {
    // URL gốc của API
    private final String url;
    // Class type của item để parse JSON
    private final Class<T> type;
    // Danh sách items đã load
    private Seq<T> items = new Seq<>();
    // Cờ đánh dấu còn trang tiếp theo không
    private boolean hasMore = true;
    // Cờ đánh dấu đang loading
    private boolean loading = false;
    // Cờ đánh dấu có lỗi
    private boolean error = false;
    // Thông báo lỗi
    private String errorMessage = "";
    // Số trang hiện tại (0-indexed)
    private int page = 0;
    // Số item mỗi trang
    private int size = 20;

    // Constructor nhận type và URL
    public ServerPagingRequest(Class<T> type, String url) {
        // Lưu URL
        this.url = url;
        // Lưu type
        this.type = type;
    }

    // Phương thức reset về trạng thái ban đầu
    public void clear() {
        // Xóa danh sách items
        items.clear();
        // Reset về trang đầu
        page = 0;
        // Đánh dấu còn dữ liệu
        hasMore = true;
        // Xóa trạng thái lỗi
        error = false;
    }

    // Getter lấy số trang hiện tại
    public int getPage() {
        return page;
    }

    // Setter đặt số trang và reset dữ liệu
    public void setPage(int page) {
        // Đặt trang mới
        this.page = page;
        // Xóa items cũ
        items.clear();
        // Đánh dấu còn dữ liệu
        hasMore = true;
        // Xóa trạng thái lỗi
        error = false;
    }

    // Setter đặt số item mỗi trang
    public void setItemPerPage(int size) {
        this.size = size;
    }

    // Kiểm tra có đang loading không
    public boolean isLoading() {
        return loading;
    }

    // Kiểm tra có lỗi không
    public boolean isError() {
        return error;
    }

    // Lấy thông báo lỗi
    public String getError() {
        return errorMessage;
    }

    // Kiểm tra còn trang tiếp theo không
    public boolean hasMore() {
        return hasMore;
    }

    // Lấy dữ liệu trang hiện tại
    public void getPage(Cons<Seq<T>> listener) {
        // Xóa items cũ
        items.clear();
        // Gọi API lấy dữ liệu
        getData(listener);
    }

    // Chuyển đến trang tiếp theo
    public void nextPage(Cons<Seq<T>> listener) {
        // Nếu còn trang tiếp
        if (hasMore) {
            // Tăng số trang
            page++;
            // Xóa items cũ
            items.clear();
            // Gọi API lấy dữ liệu
            getData(listener);
        } else {
            // Nếu hết thì trả về items hiện tại trên UI thread
            Core.app.post(() -> listener.get(items));
        }
    }

    // Chuyển về trang trước
    public void previousPage(Cons<Seq<T>> listener) {
        // Nếu không phải trang đầu
        if (page > 0) {
            // Giảm số trang
            page--;
            // Xóa items cũ
            items.clear();
            // Gọi API lấy dữ liệu
            getData(listener);
        } else {
            // Nếu ở trang đầu thì trả về items hiện tại trên UI thread
            Core.app.post(() -> listener.get(items));
        }
    }

    // Phương thức private thực hiện HTTP request
    private void getData(Cons<Seq<T>> listener) {
        // Đánh dấu đang loading
        loading = true;
        // Xóa trạng thái lỗi
        error = false;
        // Xây dựng URL với tham số phân trang
        String requestUrl = url + (url.contains("?") ? "&" : "?") + "page=" + page + "&size=" + size;
        // Thực hiện HTTP GET request
        Http.get(requestUrl)
                // Xử lý lỗi
                .error(err -> handleError(listener, err, requestUrl))
                // Xử lý response thành công
                .submit(response -> handleResult(response, listener));
    }

    // Phương thức xử lý lỗi
    private void handleError(Cons<Seq<T>> listener, Throwable err, String url) {
        // Log lỗi với URL
        Log.err(url, err);
        // Tắt cờ loading
        loading = false;
        // Bật cờ error
        error = true;
        // Lưu thông báo lỗi
        errorMessage = err.getMessage();
        // Trả về danh sách rỗng trên UI thread
        Core.app.post(() -> listener.get(new Seq<>()));
    }

    // Suppress warning do generic type erasure khi parse JSON
    @SuppressWarnings("unchecked")
    // Phương thức xử lý response thành công
    private void handleResult(HttpResponse response, Cons<Seq<T>> listener) {
        // Lấy nội dung response
        String data = response.getResultAsString();
        // Parse JSON thành danh sách items
        Seq<T> newItems = JsonIO.json.fromJson(Seq.class, type, data);

        // Nếu số items nhỏ hơn size thì không còn trang tiếp
        if (newItems.size < size) {
            hasMore = false;
        }
        // Xóa items cũ
        items.clear();
        // Thêm items mới
        items.addAll(newItems);
        // Tắt cờ loading
        loading = false;

        // Trả kết quả về listener trên UI thread
        Core.app.post(() -> listener.get(items));
    }
}
