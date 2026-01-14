// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Strings để xử lý chuỗi
import arc.util.Strings;

/**
 * Lớp lưu thông tin server cho Player Connect.
 * Parse và validate địa chỉ IP:port.
 */
public class Server {
    // Địa chỉ IP của server
    public String ip, name, error, last;
    // Port của server
    public int port;
    // Cờ đánh dấu địa chỉ hợp lệ
    public boolean wasValid;

    // Phương thức parse địa chỉ từ string
    public synchronized boolean set(String ip) {
        // Nếu giống lần trước thì trả về kết quả cũ
        if (ip.equals(last))
            return wasValid;
        // Reset các giá trị
        this.ip = this.error = null;
        this.port = 0;
        // Lưu giá trị để so sánh lần sau
        last = ip;

        // Nếu rỗng thì lỗi
        if (ip.isEmpty()) {
            this.error = "@message.room.missing-host";
            return wasValid = false;
        }
        try {
            // Kiểm tra có phải IPv6 không (có nhiều hơn 1 dấu :)
            boolean isIpv6 = Strings.count(ip, ':') > 1;
            // Nếu là IPv6 và có format [ip]:port
            if (isIpv6 && ip.lastIndexOf("]:") != -1 && ip.lastIndexOf("]:") != ip.length() - 1) {
                // Tìm vị trí ]:
                int idx = ip.indexOf("]:");
                // Parse IP (bỏ dấu [ ])
                this.ip = ip.substring(1, idx);
                // Parse port
                this.port = Integer.parseInt(ip.substring(idx + 2));
                // Kiểm tra port hợp lệ
                if (port < 0 || port > 0xFFFF)
                    throw new Exception();
            // Nếu là IPv4 và có port
            } else if (!isIpv6 && ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1) {
                // Tìm vị trí dấu :
                int idx = ip.lastIndexOf(':');
                // Parse IP
                this.ip = ip.substring(0, idx);
                // Parse port
                this.port = Integer.parseInt(ip.substring(idx + 1));
                // Kiểm tra port hợp lệ
                if (port < 0 || port > 0xFFFF)
                    throw new Exception();
            } else {
                // Thiếu port
                this.error = "@message.room.missing-port";
                return wasValid = false;
            }
            // Thành công
            return wasValid = true;
        } catch (Exception e) {
            // Port không hợp lệ
            this.error = "@message.room.invalid-port";
            return wasValid = false;
        }
    }

    // Phương thức chuyển về string
    public String get() {
        // Nếu không hợp lệ thì trả về rỗng
        if (!wasValid) {
            return "";
        // Nếu là IPv6 thì thêm dấu []
        } else if (Strings.count(ip, ':') > 1) {
            return "[" + ip + "]:" + port;
        } else {
            // IPv4 format bình thường
            return ip + ":" + port;
        }
    }
}
