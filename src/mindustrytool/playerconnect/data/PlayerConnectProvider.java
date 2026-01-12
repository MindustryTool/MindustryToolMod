// Khai báo package cho data classes của module Player Connect
package mindustrytool.playerconnect.data;

// Import annotation Data từ Lombok để tự động tạo getter/setter/equals/hashCode/toString
import lombok.Data;
// Import Accessors để cấu hình cách tạo accessor methods
import lombok.experimental.Accessors;

// Annotation tự động tạo getter/setter/equals/hashCode/toString
@Data
// Cấu hình accessor methods: chain = true cho phép gọi liên tiếp, fluent = true bỏ prefix get/set
@Accessors(chain = true, fluent = true)
// Lớp đại diện cho một provider kết nối (CLaJ server)
public class PlayerConnectProvider {
    // ID duy nhất của provider
    private String id;
    // Tên hiển thị của provider
    private String name;
    // Địa chỉ kết nối của provider (host:port)
    private String address;
}
