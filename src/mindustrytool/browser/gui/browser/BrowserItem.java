package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import arc.struct.Seq; // Import Seq cho danh sách tags
import mindustrytool.browser.data.DataTag; // Import DataTag

/**
 * Interface chung cho các item hiển thị trong browser.
 * Được implement bởi DataMap, DataSchematic, etc.
 * Cung cấp các method chung để BrowserDialog và BrowserInfoDialog có thể render.
 */
public interface BrowserItem { // Interface định nghĩa contract cho browser items

    /**
     * Lấy ID của item.
     * @return ID unique của item trên server
     */
    String id();

    /**
     * Lấy tên của item.
     * @return Tên hiển thị của item
     */
    String name();

    /**
     * Lấy mô tả của item.
     * @return Mô tả chi tiết hoặc null
     */
    String description();

    /**
     * Lấy ID người tạo.
     * @return ID của người tạo item
     */
    String createdBy();

    /**
     * Lấy chiều rộng của item.
     * @return Chiều rộng (tiles)
     */
    long width();

    /**
     * Lấy chiều cao của item.
     * @return Chiều cao (tiles)
     */
    long height();

    /**
     * Lấy số lượt like.
     * @return Số likes hoặc null
     */
    Long likes();

    /**
     * Lấy số bình luận.
     * @return Số comments hoặc null
     */
    Long comments();

    /**
     * Lấy số lượt download.
     * @return Số downloads hoặc null
     */
    Long downloads();

    /**
     * Lấy danh sách tags.
     * @return Seq<DataTag> các tag của item
     */
    Seq<DataTag> tags();
}
