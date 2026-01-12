package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.struct.Seq; // Import Seq - mảng động của Arc
import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

import mindustrytool.browser.gui.browser.BrowserItem; // Import interface BrowserItem

/**
 * Data class đại diện cho một Schematic entry từ API.
 * Đã gộp nội dung chi tiết (DetailDataSchematic) vào đây để tránh file trùng lặp.
 * Chứa metadata cơ bản và chi tiết dùng cho SchematicInfoDialog.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: schematic.id() thay vì schematic.getId()
public class DataSchematic implements BrowserItem { // Implement BrowserItem để dùng trong BrowserDialog

    // ID của schematic trên server (UUID hoặc unique string)
    String id;
    // ID ngắn gọn của item (dùng cho URL hoặc display)
    String itemId;
    // ID của người tạo schematic
    String createdBy;
    // Tên của schematic
    String name;
    // Mô tả chi tiết của schematic
    String description;
    // Chiều rộng của schematic (tiles)
    long width;
    // Chiều cao của schematic (tiles)
    long height;
    // Số lượt like
    Long likes;
    // Số lượt download, mặc định 0
    Long downloads = 0l;
    // Số bình luận, mặc định 0
    Long comments = 0l;
    // Danh sách các tag được gán
    Seq<DataTag> tags;
    // Metadata chứa requirements
    SchematicMetadata meta;

    /**
     * Nested class chứa metadata của schematic.
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class SchematicMetadata {
        // Danh sách các requirement (tài nguyên cần thiết)
        Seq<SchematicRequirement> requirements;
    }

    /**
     * Nested class đại diện cho một requirement của schematic.
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class SchematicRequirement {
        // Tên của tài nguyên (vd: "copper", "lead")
        String name;
        // Mã màu hex của tài nguyên (vd: "#d99d73")
        String color;
        // Số lượng cần thiết
        int amount;
    }
}
