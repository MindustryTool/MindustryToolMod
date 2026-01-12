package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import arc.Core; // Import Core để truy cập clipboard
import arc.func.Cons; // Import interface callback function
import arc.scene.Element; // Import Element - base class cho UI widgets
import arc.scene.ui.layout.Table; // Import Table - container layout
import arc.util.serialization.Base64Coder; // Import Base64Coder để encode bytes

import mindustry.Vars; // Import Vars để truy cập game variables
import mindustry.game.Schematic; // Import Schematic class
import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.ui.Styles; // Import Styles cho button styles

import mindustrytool.browser.BrowserApi; // Import BrowserApi để gọi API
import mindustrytool.browser.data.DataSchematic; // Import DataSchematic data class
import mindustrytool.browser.gui.BrowserInfoDialog; // Import BrowserInfoDialog
import mindustrytool.browser.gui.SchematicImage; // Import SchematicImage widget
import mindustrytool.config.Utils; // Import Utils để đọc schematic

import static mindustry.Vars.*; // Static import các Vars thường dùng

/**
 * Handler xử lý các hành động và UI cho Schematic items.
 * Implement BrowserItemHandler để cung cấp logic cụ thể cho Schematics.
 */
public class SchematicItemHandler implements BrowserItemHandler<DataSchematic> { // Implement interface với DataSchematic

    // Dialog hiển thị thông tin chi tiết schematic
    private final BrowserInfoDialog infoDialog = new BrowserInfoDialog();

    /**
     * Tạo image widget cho schematic item.
     * @param item DataSchematic cần hiển thị
     * @return SchematicImage element
     */
    @Override
    public Element createImage(DataSchematic item) {
        return new SchematicImage(item.id()); // Tạo SchematicImage với ID của schematic
    }

    /**
     * Tạo các button actions cho schematic card.
     * Bao gồm: copy, download, info buttons.
     * @param buttons Table chứa các buttons
     * @param item DataSchematic để xử lý
     */
    @Override
    public void buildActionButtons(Table buttons, DataSchematic item) {
        // Button copy - copy schematic vào clipboard
        buttons.button(Icon.copy, Styles.emptyi, () -> handleCopy(item))
                .padLeft(2).padRight(2); // Padding trái phải 2px
        // Button download - lưu schematic vào game
        buttons.button(Icon.download, Styles.emptyi, () -> handleDownload(item))
                .padLeft(2).padRight(2); // Padding trái phải 2px
        // Button info - mở BrowserInfoDialog với data từ API
        buttons.button(Icon.info, Styles.emptyi, () -> BrowserApi.findSchematicById(item.id(), data -> infoDialog.show(data, BrowserType.SCHEMATIC)))
                .tooltip("@info.title"); // Tooltip localized
    }

    /**
     * Xử lý khi user click vào schematic item.
     * Nếu trong game: sử dụng schematic để đặt.
     * Nếu ở menu: mở info dialog.
     * @param item DataSchematic được click
     * @param hideDialog Runnable để đóng dialog
     */
    @Override
    public void handleItemClick(DataSchematic item, Runnable hideDialog) {
        // Nếu đang ở menu (không trong game)
        if (state.isMenu()) {
            // Mở info dialog
            BrowserApi.findSchematicById(item.id(), data -> infoDialog.show(data, BrowserType.SCHEMATIC));
        } else {
            // Đang trong game
            // Kiểm tra có cho phép schematics không
            if (!state.rules.schematicsAllowed) {
                ui.showInfo("@schematic.disabled"); // Thông báo bị disable
            } else {
                // Download và sử dụng schematic để đặt
                downloadSchematicData(item, d -> control.input.useSchematic(Utils.readSchematic(d)));
                hideDialog.run(); // Đóng dialog
            }
        }
    }

    /**
     * Xử lý download schematic.
     * Lưu schematic vào thư viện của game.
     * @param item DataSchematic cần download
     */
    @Override
    public void handleDownload(DataSchematic item) {
        // Download binary data của schematic
        downloadSchematicData(item, d -> {
            // Parse thành Schematic object
            Schematic s = Utils.readSchematic(d);
            // Lấy thông tin chi tiết để add tags
            BrowserApi.findSchematicById(item.id(), detail -> {
                s.labels.add(detail.tags().map(i -> i.name())); // Add các tag names
                s.removeSteamID(); // Xóa Steam ID nếu có
                Vars.schematics.add(s); // Thêm vào thư viện game
                ui.showInfoFade("@schematic.saved"); // Thông báo thành công
            });
        });
    }

    /**
     * Xử lý copy schematic vào clipboard.
     * @param item DataSchematic cần copy
     */
    private void handleCopy(DataSchematic item) {
        // Download binary data của schematic
        downloadSchematicData(item, d -> {
            // Parse thành Schematic object
            Schematic s = Utils.readSchematic(d);
            // Encode thành base64 và copy vào clipboard
            Core.app.setClipboardText(schematics.writeBase64(s));
            ui.showInfoFade("@copied"); // Thông báo đã copy
        });
    }

    /**
     * Helper method để download schematic data và encode base64.
     * @param item DataSchematic cần download
     * @param cons Callback nhận base64 string
     */
    private void downloadSchematicData(DataSchematic item, Cons<String> cons) {
        // Gọi API để download binary data
        BrowserApi.downloadSchematic(item.id(), result -> {
            // Encode bytes thành base64 string và gọi callback
            cons.get(new String(Base64Coder.encode(result)));
        });
    }
}
