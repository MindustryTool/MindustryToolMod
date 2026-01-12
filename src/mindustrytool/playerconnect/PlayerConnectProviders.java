// Khai báo package cho module kết nối người chơi
package mindustrytool.playerconnect;

// Import Core để truy cập settings
import arc.Core;
// Import ArrayMap là ordered map của Arc
import arc.struct.ArrayMap;

// Lớp quản lý danh sách các provider cho Player Connect
public class PlayerConnectProviders {
    // URL của provider công khai (rỗng = mặc định)
    public static final String PUBLIC_PROVIDER_URL = "";
    // Khóa dùng để lưu providers tùy chỉnh vào persistent storage
    public static final String PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY = "player-connect-providers";
    // Map lưu các provider online (từ API) và custom (người dùng thêm)
    public static final ArrayMap<String, String> online = new ArrayMap<>(),
            custom = new ArrayMap<>();

    // Phương thức làm mới danh sách provider online từ API
    public static synchronized void refreshOnline(Runnable onCompleted, arc.func.Cons<Throwable> onFailed) {
        // Gọi API lấy danh sách providers
        PlayerConnectApi.findPlayerConnectProvider(providers -> {
            // Xóa danh sách cũ
            online.clear();
            // Thêm từng provider vào map
            for (var provider : providers) {
                // Dùng tên làm key, địa chỉ làm value
                online.put(provider.name(), provider.address());
            }
            // Gọi callback hoàn thành
            onCompleted.run();
        }, onFailed);
    }

    // Suppress warning do generic type erasure khi đọc từ settings
    @SuppressWarnings("unchecked")
    // Phương thức load danh sách provider tùy chỉnh từ settings
    public static void loadCustom() {
        // Xóa danh sách cũ
        custom.clear();
        // Đọc từ settings và thêm vào map
        custom.putAll(Core.settings.getJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, ArrayMap.class, String.class,
                ArrayMap::new));
    }

    // Phương thức lưu danh sách provider tùy chỉnh vào settings
    public static void saveCustom() {
        // Lưu map vào settings dưới dạng JSON
        Core.settings.putJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, String.class, custom);
    }
}
