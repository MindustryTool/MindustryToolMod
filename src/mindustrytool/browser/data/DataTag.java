package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.struct.Seq; // Import Seq - mảng động của Arc
import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

/**
 * Data class đại diện cho một tag.
 * Tag được gán cho maps hoặc schematics để phân loại.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: tag.id() thay vì tag.getId()
public class DataTag { // Class chứa thông tin tag

    // ID của tag trên server
    private String id;
    // Tên hiển thị của tag
    private String name;
    // Vị trí sắp xếp trong danh sách, mặc định 0
    private Integer position = 0;
    // ID của category chứa tag này
    private String categoryId;
    // Icon emoji hoặc ký tự đại diện
    private String icon;
    // Tag đầy đủ (icon + name)
    private String fullTag;
    // Mã màu hex của tag
    private String color;
    // Số lượng items có tag này, mặc định 0
    private Integer count = 0;
    // Danh sách ID các planet mà tag áp dụng
    private Seq<String> planetIds;
}
