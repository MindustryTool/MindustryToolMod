// Khai báo package cho data classes của module Player Connect
package mindustrytool.playerconnect.data;

// Import Seq là collection của Arc framework
import arc.struct.Seq;
// Import annotation Data từ Lombok để tự động tạo getter/setter/equals/hashCode/toString
import lombok.Data;
// Import Accessors để cấu hình cách tạo accessor methods
import lombok.experimental.Accessors;

// Annotation tự động tạo getter/setter/equals/hashCode/toString
@Data
// Cấu hình accessor methods: chain = true cho phép gọi liên tiếp, fluent = true bỏ prefix get/set
@Accessors(chain = true, fluent = true)
// Lớp đại diện cho một phòng Player Connect
public class PlayerConnectRoom {
    // ID duy nhất của phòng
    private String roomId;
    // Link kết nối đến phòng (player-connect://host:port/roomId)
    private String link;
    // Dữ liệu chi tiết của phòng
    private PlayerConnectRoomData data;

    // Annotation tự động tạo getter/setter/equals/hashCode/toString
    @Data
    // Cấu hình accessor methods
    @Accessors(chain = true, fluent = true)
    // Lớp chứa dữ liệu chi tiết của phòng
    public static class PlayerConnectRoomData {
        // Tên phòng/host
        private String name;
        // Trạng thái phòng
        private String status;
        // Cờ đánh dấu phòng riêng tư
        private boolean isPrivate;
        // Cờ đánh dấu phòng có mật khẩu
        private boolean isSecured;
        // Danh sách người chơi trong phòng
        private Seq<PlayerConnectRoomPlayer> players;
        // Tên map đang chơi
        private String mapName;
        // Chế độ chơi (survival, sandbox, attack, pvp, editor)
        private String gamemode;
        // Danh sách mod đang sử dụng
        private Seq<String> mods;
        // Phiên bản game của host
        private String version;
        // Ngôn ngữ của host
        private String locale;
    }

    // Annotation tự động tạo getter/setter/equals/hashCode/toString
    @Data
    // Cấu hình accessor methods
    @Accessors(chain = true, fluent = true)
    // Lớp chứa thông tin người chơi trong phòng
    public static class PlayerConnectRoomPlayer {
        // Tên người chơi
        private String name;
        // Ngôn ngữ của người chơi
        private String locale;
    }
}
