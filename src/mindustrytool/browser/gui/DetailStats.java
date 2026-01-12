package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import arc.scene.ui.layout.Table; // Import Table layout container
import mindustry.gen.Icon; // Import các icon có sẵn trong Mindustry

/**
 * Utility nhỏ để vẽ các icon và số đếm likes/comments/downloads.
 * Xử lý an toàn khi giá trị null.
 */
public class DetailStats {
    /**
     * Vẽ thống kê (likes, comments, downloads) vào table.
     * @param table Table container để chứa các thống kê
     * @param likes Số lượt thích (có thể null)
     * @param comments Số bình luận (có thể null)
     * @param downloads Số lượt tải (có thể null)
     */
    public static void draw(Table table, Long likes, Long comments, Long downloads) {
        // Căn trái nội dung trong table
        table.left();
        // Thêm thống kê likes với icon mũi tên lên
        addStat(table, Icon.upOpenSmall, likes);
        // Thêm thống kê comments với icon chat
        addStat(table, Icon.chatSmall, comments);
        // Thêm thống kê downloads với icon download
        addStat(table, Icon.downloadSmall, downloads);
    }

    /**
     * Thêm một stat đơn lẻ (icon + số) vào table.
     * @param table Table container
     * @param icon Icon drawable để hiển thị
     * @param value Giá trị số (có thể null, sẽ hiển thị 0 nếu null)
     */
    private static void addStat(Table table, arc.scene.style.Drawable icon, Long value) {
        // Thêm icon với padding trái/phải 2px
        table.image(icon).padLeft(2).padRight(2);
        // Thêm giá trị số, nếu null thì hiển thị 0
        table.add(" " + (value != null ? value : 0) + " ").marginLeft(2);
    }
}
