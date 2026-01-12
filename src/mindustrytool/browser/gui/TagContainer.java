package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import arc.scene.ui.layout.Scl; // Import Scl để scale theo DPI
import arc.scene.ui.layout.Table; // Import Table layout container
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.util.Align; // Import các hằng số căn chỉnh
import mindustry.gen.Tex; // Import các texture có sẵn
import mindustrytool.browser.data.DataTag; // Import data class cho Tag

/**
 * Helper nhỏ để render danh sách tag dạng nút pill có thể cuộn.
 */
public class TagContainer {
    /**
     * Vẽ container chứa các tag vào table.
     * @param container Table cha để chứa tags
     * @param tags Danh sách các tag cần hiển thị
     */
    public static void draw(Table container, Seq<DataTag> tags) {
        // Xóa tất cả children hiện tại của container
        container.clearChildren();
        // Căn trái container
        container.left();

        // Nếu không có tag nào thì thoát
        if (tags == null) {
            return;
        }

        // Thêm label "Tags:" với padding phải 4px
        container.add("@schematic.tags").padRight(4);

        // Tạo scroll pane để chứa các tag (có thể cuộn ngang)
        container.pane(scrollPane -> {
            // Căn trái nội dung trong scroll pane
            scrollPane.left();
            // Thiết lập mặc định: padding 4px, chiều cao 42px cho mỗi item
            scrollPane.defaults().pad(4).height(Scl.scl(36f));
            // Biến đếm để xuống dòng mỗi 4 tag
            int i = 0;
            // Duyệt qua từng tag
            for (var tag : tags) {
                // Tạo table con với background button, chứa tên tag
                scrollPane.table(Tex.button, item -> item.add(tag.name())
                        .height(Scl.scl(36f)) // Chiều cao scaled
                        .fillX() // Fill theo chiều ngang
                        .growX() // Mở rộng theo chiều ngang
                        .labelAlign(Align.center) // Căn giữa text
                ).fillX(); // Table fill theo chiều ngang

                // Mỗi 4 tag thì xuống dòng mới
                if (++i % 4 == 0) {
                    scrollPane.row();
                }
            }

        })
                .fillX() // Fill chiều ngang
                .margin(20) // Margin 20px
                .left() // Căn trái
                .scrollX(true); // Cho phép cuộn ngang
    }
}
