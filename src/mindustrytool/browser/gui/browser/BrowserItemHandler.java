package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import arc.scene.Element; // Import Element - base class cho tất cả UI widgets
import arc.scene.ui.layout.Table; // Import Table - container layout linh hoạt

/**
 * Interface xử lý các hành động và UI cho từng loại item.
 * Cho phép BrowserDialog delegate các xử lý cụ thể cho từng loại (Map/Schematic).
 * @param <T> Loại BrowserItem (DataMap, DataSchematic, etc.)
 */
public interface BrowserItemHandler<T extends BrowserItem> { // Interface với generic type

    /**
     * Tạo image widget cho item.
     * @param item Item cần hiển thị
     * @return Element chứa image preview
     */
    Element createImage(T item);

    /**
     * Tạo các button actions trong card item.
     * Ví dụ: copy, download, info buttons.
     * @param buttons Table chứa các buttons
     * @param item Item để xử lý actions
     */
    void buildActionButtons(Table buttons, T item);

    /**
     * Xử lý khi user click vào item.
     * @param item Item được click
     * @param hideDialog Runnable để đóng dialog nếu cần
     */
    void handleItemClick(T item, Runnable hideDialog);

    /**
     * Xử lý download item.
     * @param item Item cần download
     */
    void handleDownload(T item);
}
