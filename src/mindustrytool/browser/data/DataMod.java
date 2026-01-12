package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

/**
 * Data class chứa metadata tối thiểu của mod.
 * Dùng bởi mod selector trong filter dialog.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: mod.id() thay vì mod.getId()
public class DataMod { // Class chứa thông tin mod

    // ID của mod trên server
    private String id;
    // Tên hiển thị của mod
    private String name;
    // Icon emoji hoặc URL của mod
    private String icon;
    // Vị trí sắp xếp trong danh sách, mặc định 0
    private Integer position = 0;
}
