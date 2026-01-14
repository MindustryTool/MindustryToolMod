// Khai báo package cho module networking
package mindustrytool.playerconnect.net;

// Import Core để chạy callback trên UI thread
import arc.Core;
// Import Cons là functional interface nhận 1 tham số
import arc.func.Cons;
// Import Seq là collection của Arc framework
import arc.struct.Seq;
// Import Http để thực hiện các request HTTP
import arc.util.Http;
// Import Log để ghi log lỗi
import arc.util.Log;
// Import JsonIO để parse JSON response
import mindustry.io.JsonIO;
// Import PlayerConnectConfig cho API URL
import mindustrytool.playerconnect.PlayerConnectConfig;
// Import data class đại diện cho provider kết nối
import mindustrytool.playerconnect.data.PlayerConnectProvider;
// Import data class đại diện cho phòng kết nối
import mindustrytool.playerconnect.data.PlayerConnectRoom;

// Lớp chứa các phương thức API cho module Player Connect
public class PlayerConnectApi {

    // Phương thức tìm kiếm danh sách phòng theo từ khóa
    public static void findPlayerConnectRooms(String search, Cons<Seq<PlayerConnectRoom>> listener) {
        // Xây dựng URL tìm kiếm phòng với tham số name
        String url = PlayerConnectConfig.API_URL + "player-connect/rooms?name=" + search;
        
        // Thực hiện HTTP GET request
        Http.get(url)
                // Xử lý lỗi khi request thất bại
                .error(error -> {
                    // Ghi log lỗi với URL và exception
                    Log.err(url, error);
                    // Trả về danh sách rỗng trên UI thread
                    Core.app.post(() -> listener.get(new Seq<>()));
                })
                // Xử lý response thành công
                .submit(response -> {
                    // Lấy nội dung response dưới dạng string
                    String data = response.getResultAsString();
                    // Suppress warning do generic type erasure khi parse JSON
                    @SuppressWarnings("unchecked")
                    // Parse JSON thành danh sách PlayerConnectRoom
                    Seq<PlayerConnectRoom> rooms = JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, data);
                    // Trả kết quả về listener trên UI thread, nếu null thì trả danh sách rỗng
                    Core.app.post(() -> listener.get(rooms != null ? rooms : new Seq<>()));
                });
    }

    // Phương thức lấy danh sách các provider kết nối
    public static void findPlayerConnectProvider(Cons<Seq<PlayerConnectProvider>> onSuccess, Cons<Throwable> onFailed) {
        // Xây dựng URL lấy danh sách providers
        String url = PlayerConnectConfig.API_URL + "player-connect/providers";
        
        // Thực hiện HTTP GET request
        Http.get(url)
                // Xử lý lỗi khi request thất bại
                .error(error -> {
                    // Ghi log lỗi với URL và exception
                    Log.err(url, error);
                    // Gọi callback onFailed với lỗi trên UI thread
                    Core.app.post(() -> onFailed.get(error));
                })
                // Xử lý response thành công
                .submit(response -> {
                    // Lấy nội dung response dưới dạng string
                    String data = response.getResultAsString();
                    // Suppress warning do generic type erasure khi parse JSON
                    @SuppressWarnings("unchecked")
                    // Parse JSON thành danh sách PlayerConnectProvider
                    Seq<PlayerConnectProvider> providers = JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, data);
                    // Gọi callback onSuccess với kết quả trên UI thread, nếu null thì trả danh sách rỗng
                    Core.app.post(() -> onSuccess.get(providers != null ? providers : new Seq<>()));
                });
    }
}
