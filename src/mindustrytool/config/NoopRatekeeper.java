// Khai báo package cho module config
package mindustrytool.config;

// Import Ratekeeper là lớp giới hạn tốc độ gửi packet
import arc.util.Ratekeeper;

// Lớp NoopRatekeeper kế thừa Ratekeeper nhưng không giới hạn gì cả
// Dùng cho Player Connect để tránh bị blacklist bởi hệ thống rate limiting
public class NoopRatekeeper extends Ratekeeper {
    // Override phương thức allow để luôn cho phép
    @Override
    public boolean allow(long spacing, int cap) {
        // Luôn trả về true, không bao giờ từ chối
        return true;
    }
}
