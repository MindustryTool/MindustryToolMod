package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import arc.files.Fi; // Import Fi để xử lý file operations
import arc.scene.Element; // Import Element - base class cho UI widgets
import arc.scene.ui.layout.Table; // Import Table - container layout

import mindustry.Vars; // Import Vars để truy cập game variables
import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.ui.Styles; // Import Styles cho button styles

import mindustrytool.browser.BrowserApi; // Import BrowserApi để gọi API
import mindustrytool.browser.data.DataMap; // Import DataMap data class
import mindustrytool.browser.gui.BrowserInfoDialog; // Import BrowserInfoDialog
import mindustrytool.browser.gui.MapImage; // Import MapImage widget

import static mindustry.Vars.*; // Static import các Vars thường dùng

/**
 * Handler xử lý các hành động và UI cho Map items.
 * Implement BrowserItemHandler để cung cấp logic cụ thể cho Maps.
 */
public class MapItemHandler implements BrowserItemHandler<DataMap> { // Implement interface với DataMap

    // Dialog hiển thị thông tin chi tiết map
    private final BrowserInfoDialog infoDialog = new BrowserInfoDialog();

    /**
     * Tạo image widget cho map item.
     * @param item DataMap cần hiển thị
     * @return MapImage element
     */
    @Override
    public Element createImage(DataMap item) {
        return new MapImage(item.id()); // Tạo MapImage với ID của map
    }

    /**
     * Tạo các button actions cho map card.
     * Bao gồm: download, info buttons.
     * @param buttons Table chứa các buttons
     * @param item DataMap để xử lý
     */
    @Override
    public void buildActionButtons(Table buttons, DataMap item) {
        // Button download - gọi handleDownload khi click
        buttons.button(Icon.download, Styles.emptyi, () -> handleDownload(item))
                .padLeft(2).padRight(2); // Padding trái phải 2px
        // Button info - mở BrowserInfoDialog với data từ API
        buttons.button(Icon.info, Styles.emptyi, () -> BrowserApi.findMapById(item.id(), data -> infoDialog.show(data, BrowserType.MAP)))
                .tooltip("@info.title"); // Tooltip localized
    }

    /**
     * Xử lý khi user click vào map item.
     * Map không có hành động mặc định khi click.
     * @param item DataMap được click
     * @param hideDialog Runnable để đóng dialog (không dùng)
     */
    @Override
    public void handleItemClick(DataMap item, Runnable hideDialog) {
        // Map không có hành động khi click - để trống
    }

    /**
     * Xử lý download map.
     * Lưu map vào customMapDirectory và import vào game.
     * @param item DataMap cần download
     */
    @Override
    public void handleDownload(DataMap item) {
        // Gọi API để download binary data của map
        BrowserApi.downloadMap(item.id(), result -> {
            // Tạo file trong thư mục custom maps
            Fi mapFile = Vars.customMapDirectory.child(item.id());
            mapFile.writeBytes(result); // Ghi bytes vào file
            Vars.maps.importMap(mapFile); // Import map vào game
            ui.showInfoFade("@map.saved"); // Hiển thị thông báo thành công
        });
    }
}
