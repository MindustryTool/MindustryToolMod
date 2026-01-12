package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import arc.graphics.Color; // Import Color để xử lý màu sắc
import arc.graphics.g2d.Draw; // Import Draw để reset draw state
import arc.scene.ui.Button; // Import Button widget
import arc.scene.ui.Label; // Import Label widget
import arc.scene.ui.layout.Scl; // Import Scl để scale theo DPI
import arc.scene.ui.layout.Table; // Import Table - container layout
import arc.util.Align; // Import Align constants

import mindustry.gen.Tex; // Import Tex cho textures
import mindustry.ui.Styles; // Import Styles cho UI styles

/**
 * UI Component để hiển thị một item card trong browser.
 * Đây là card chứa preview image, tên, và stats của item.
 */
public class ItemCard { // Class tạo item cards

    // Kích thước cố định của card (pixels, scaled)
    private static final float CARD_SIZE = Scl.scl(200f);

    /**
     * Tạo một item card button.
     * @param <T> Loại BrowserItem
     * @param container Table chứa card
     * @param item Item data để hiển thị
     * @param handler Handler xử lý actions cho item
     * @param hideDialog Runnable để đóng dialog khi cần
     * @return Button card đã tạo
     */
    public static <T extends BrowserItem> Button create(
            Table container, // Container cha
            T item, // Data của item
            BrowserItemHandler<T> handler, // Handler xử lý logic
            Runnable hideDialog // Callback đóng dialog
    ) {
        // Reference để giữ button (vì dùng trong lambda)
        Button[] buttonRef = { null };

        // Tạo button với nội dung preview
        buttonRef[0] = container.button(preview -> {
            preview.top(); // Căn top
            preview.margin(0f); // Không margin

            // --- Hàng action buttons ở trên cùng ---
            preview.table(buttons -> {
                buttons.center(); // Căn giữa
                buttons.defaults().size(50f); // Size mặc định cho buttons
                handler.buildActionButtons(buttons, item); // Delegate tạo buttons cho handler
            }).growX().height(50f); // Chiếm hết width, height cố định 50

            preview.row(); // Xuống hàng mới

            // --- Stack: Image + Name overlay ---
            preview.stack(
                    handler.createImage(item), // Image từ handler
                    new Table(nameTable -> { // Overlay table cho tên
                        nameTable.top(); // Căn top
                        // Table với background đen mờ
                        nameTable.table(Styles.black3, c -> {
                            // Label hiển thị tên item
                            Label label = c.add(item.name())
                                    .style(Styles.outlineLabel) // Style có outline
                                    .color(Color.white) // Màu trắng
                                    .top() // Căn top
                                    .growX() // Chiếm hết width
                                    .width(CARD_SIZE - 8f).get(); // Width cố định
                            Draw.reset(); // Reset draw state
                            label.setEllipsis(true); // Hiển thị "..." nếu quá dài
                            label.setAlignment(Align.center); // Căn giữa text
                        }).growX().margin(1).pad(4).maxWidth(Scl.scl(CARD_SIZE - 8f)).padBottom(0);
                    })
            ).size(CARD_SIZE); // Size cố định cho stack

            preview.row(); // Xuống hàng mới

            // --- Hàng stats: likes, comments, downloads ---
             // Margin 8px
            ItemStats.renderStatsRow(preview, item, false); // Render stats row
        }, () -> {
            // Click handler cho button
            // Bỏ qua nếu đang press children (action buttons)
            if (buttonRef[0].childrenPressed()) return;
            // Delegate xử lý click cho handler
            handler.handleItemClick(item, hideDialog);

        }).pad(4).style(Styles.flati).get(); // Padding 4px, flat style

        // Set background texture cho button
        buttonRef[0].getStyle().up = Tex.pane;

        return buttonRef[0]; // Trả về button đã tạo
    }
}
