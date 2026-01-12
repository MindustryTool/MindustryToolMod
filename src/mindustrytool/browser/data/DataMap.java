package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.struct.Seq; // Import Seq - mảng động của Arc
import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode/toString
import lombok.experimental.Accessors; // Import annotation @Accessors để cấu hình accessors

import mindustrytool.browser.gui.browser.BrowserItem; // Import interface BrowserItem

/**
 * Data class đại diện cho một Map entry từ API.
 * Chứa metadata cơ bản: id, author, stats, tags, dimensions, description.
 */
@Data // Lombok: tự sinh getter, setter, equals, hashCode, toString
@Accessors(chain = true, fluent = true) // Fluent accessors: map.id() thay vì map.getId(), chain cho setter trả về this
public class DataMap implements BrowserItem { // Implement BrowserItem để dùng trong BrowserDialog

    // ID của map trên server (UUID hoặc unique string)
    String id;
    // ID ngắn gọn của item (dùng cho URL hoặc display)
    String itemId;
    // Tên của map
    String name;
    // Số lượt like của map
    Long likes;
    // Số lượt download, mặc định 0
    Long downloads = 0l;
    // Số bình luận, mặc định 0
    Long comments = 0l;
    // ID của người tạo map
    String createdBy;
    // Mô tả chi tiết của map
    String description;
    // Chiều rộng của map (tiles)
    long width;
    // Chiều cao của map (tiles)
    long height;
    // Danh sách các tag được gán cho map
    Seq<DataTag> tags;
}
