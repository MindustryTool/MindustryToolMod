package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import arc.func.Cons; // Import interface callback function
import arc.scene.ui.layout.Table; // Import Table layout container
import arc.util.Strings; // Import utility xử lý chuỗi
import mindustry.gen.Icon; // Import các icon có sẵn
import mindustry.gen.Tex; // Import các texture có sẵn
import mindustry.ui.Styles; // Import các style UI
import mindustrytool.browser.data.SearchConfig; // Import cấu hình tìm kiếm

/**
 * Thanh ngang hiển thị các tag đã chọn và cho phép xóa tag.
 * Được sử dụng trong giao diện tìm kiếm của `BrowserDialog`.
 */
public class TagBar {
    /**
     * Vẽ thanh tag vào table cha.
     * @param tagBar Table container để chứa các tag
     * @param searchConfig Cấu hình tìm kiếm chứa danh sách tag đã chọn
     * @param onUpdate Callback được gọi khi có thay đổi tag
     */
    public static void draw(Table tagBar, SearchConfig searchConfig, Cons<SearchConfig> onUpdate) {
        // Duyệt qua từng tag đã được chọn trong searchConfig
        for (var tag : searchConfig.getSelectedTags()) {
            // Tạo một table con với background là button texture
            tagBar.table(Tex.button, table -> {
                // Nếu tag có icon thì hiển thị icon
                if (tag.getIcon() != null) {
                    // Thêm NetworkImage để load icon từ URL, kích thước 24x24
                    table.add(new NetworkImage(tag.getIcon())).size(24).padRight(4);
                }
                // Hiển thị tên tag với format: CategoryName_TagName (viết hoa chữ cái đầu)
                table.add(Strings.capitalize(tag.getCategoryName() + "_" + tag.getName()));
                // Thêm nút X để xóa tag
                table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                    // Khi click: xóa tag khỏi danh sách đã chọn
                    searchConfig.getSelectedTags().remove(tag);
                    // Gọi callback thông báo có thay đổi
                    onUpdate.get(searchConfig);
                }).margin(4); // Thêm margin 4px cho nút
            });
        }
    }
}
