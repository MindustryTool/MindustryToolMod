// Khai báo package cho module data
package mindustrytool.data;

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
// Lớp đại diện cho thông tin một server Mindustry
public class DataServer {
    // ID duy nhất của server
    private String id;
    // ID của manager quản lý server
    private String managerId;
    // ID của user sở hữu server
    private String userId;
    // Port mà server đang chạy
    private int port;
    // Dung lượng RAM đang sử dụng (bytes)
    private long ramUsage;
    // Tổng dung lượng RAM được cấp phát (bytes)
    private long totalRam;
    // Số lượng người chơi đang online
    private long players;
    // Địa chỉ IP/domain của server
    private String address;
    // Tên server
    private String name;
    // Mô tả server
    private String description;
    // Chế độ hoạt động (survival, sandbox, attack, pvp...)
    private String mode;
    // Tên map đang chơi
    private String mapName;
    // Gamemode của map
    private String gamemode;
    // Trạng thái server (UP, DOWN, STARTING...)
    private String status = "DOWN";
    // Cờ đánh dấu server chính thức
    private boolean isOfficial;
    // Cờ đánh dấu tự động tắt khi không có người chơi
    private boolean isAutoTurnOff = true;
    // Cờ đánh dấu đây là hub server
    private boolean isHub = false;
    // Ảnh preview của map dưới dạng byte array
    private byte[] mapImage;
    // Danh sách các mod đang sử dụng
    private Seq<String> mods = new Seq<>();

    // Getter tùy chỉnh cho mods, loại bỏ plugin nội bộ
    public Seq<String> mods() {
        // Lọc bỏ plugin "mindustrytoolplugin" khỏi danh sách
        return mods.select(v -> !v.equals("mindustrytoolplugin"));
    }
}
