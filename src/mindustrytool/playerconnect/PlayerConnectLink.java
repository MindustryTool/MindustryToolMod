// Khai báo package cho module kết nối người chơi
package mindustrytool.playerconnect;

// Import URI để làm việc với URI
import java.net.URI;
// Import URISyntaxException để xử lý lỗi cú pháp URI
import java.net.URISyntaxException;

// Lớp đại diện cho link kết nối phòng Player Connect
public class PlayerConnectLink {
    // Scheme của URI cho player connect
    public static final String UriScheme = "player-connect";

    // URI đầy đủ của link
    public final URI uri;
    // Host của server
    public final String host;
    // Port của server
    public final int port;
    // ID của phòng
    public final String roomId;

    // Constructor nhận host, port và roomId
    public PlayerConnectLink(String host, int port, String roomId) {
        // Kiểm tra host không null hoặc rỗng
        if (host == null || host.isEmpty())
            throw new IllegalArgumentException("Missing host");

        // Kiểm tra port không phải -1
        if (port == -1)
            throw new IllegalArgumentException("Missing port");

        // Nếu roomId bắt đầu bằng "/" thì bỏ dấu "/" đi
        if (roomId != null && roomId.startsWith("/"))
            roomId = roomId.substring(1);

        // Kiểm tra roomId không null hoặc rỗng
        if (roomId == null || roomId.isEmpty())
            throw new IllegalArgumentException("Missing room id");

        // Gán các giá trị
        this.host = host;
        this.port = port;
        this.roomId = roomId;

        try {
            // Tạo URI từ các thành phần
            uri = new URI(UriScheme, null, host, port, '/' + roomId, null, null);
        }
        // Lỗi này chỉ xảy ra khi host không hợp lệ
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid host");
        }
    }

    // Chuyển đổi thành string (trả về URI)
    @Override
    public String toString() {
        return uri.toString();
    }

    /** @throws IllegalArgumentException nếu link không hợp lệ */
    // Phương thức static parse link từ string
    public static PlayerConnectLink fromString(String link) {
        // Kiểm tra link bắt đầu bằng scheme nhưng thiếu host
        if (link.startsWith(UriScheme) &&
                (!link.startsWith(UriScheme + "://") || link.length() == (UriScheme + "://").length()))
            throw new IllegalArgumentException("Missing host");

        // Biến lưu URI đã parse
        URI uri;
        try {
            // Parse link thành URI
            uri = URI.create(link);
        } catch (IllegalArgumentException e) {
            // Lấy thông báo lỗi
            String cause = e.getLocalizedMessage();
            // Tìm vị trí dấu hai chấm
            int semicolon = cause.indexOf(':');
            // Nếu không có thì throw lại exception gốc
            if (semicolon == -1)
                throw e;
            else
                // Nếu có thì throw exception mới với message ngắn hơn
                throw new IllegalArgumentException(cause.substring(0, semicolon), e);
        }

        // Kiểm tra scheme có đúng là player-connect không
        if (uri.isAbsolute() && !uri.getScheme().equals(UriScheme))
            throw new IllegalArgumentException("Not a player-connect link: " + link);

        // Tạo và trả về PlayerConnectLink từ các thành phần URI
        return new PlayerConnectLink(uri.getHost(), uri.getPort(), uri.getPath());
    }
}
