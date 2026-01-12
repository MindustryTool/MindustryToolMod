// Khai báo package cho module kết nối người chơi
package mindustrytool.playerconnect;

// Lớp chứa các hằng số cấu hình cho module Player Connect
public class PlayerConnectConfig {
    // URL gốc của API backend cho các request liên quan đến Player Connect
    public static final String API_URL = "https://api.mindustry-tool.com/api/v4/";
    // Khóa dùng để lưu trữ danh sách provider vào bộ nhớ persistent (Core.settings)
    public static final String PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY = "player-connect-providers";
}
