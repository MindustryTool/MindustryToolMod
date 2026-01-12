package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.struct.Seq; // Import Seq - mảng động của Arc
import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

/**
 * Data class đại diện cho một category chứa nhiều tags.
 * Là nhóm các tag liên quan được trả về từ API.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: category.id() thay vì category.getId()
public class TagCategory { // Class chứa thông tin tag category

    // ID của category trên server
    public String id;
    // Tên hiển thị của category
    public String name;
    // Mã màu hex của category
    public String color;
    // Vị trí sắp xếp trong danh sách
    public int position;
    // Cho phép duplicate (chọn nhiều tag trong category)
    public boolean duplicate;
    // ID người tạo category
    public String createdBy;
    // ID người cập nhật cuối
    public String updatedBy;
    // Danh sách các tag trong category này
    public Seq<DataTag> tags;
}
