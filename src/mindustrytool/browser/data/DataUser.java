package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

/**
 * Data class đại diện cho thông tin user cơ bản từ API.
 * Dùng để hiển thị thông tin tác giả của map/schematic.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: user.id() thay vì user.getId()
public class DataUser { // Class chứa thông tin user

    // ID của user trên server
    private String id;
    // Tên hiển thị của user
    private String name;
    // URL avatar của user
    private String imageUrl;
}
