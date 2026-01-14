// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Core để truy cập settings
import arc.Core;
// Import TextField để tạo ô nhập text
import arc.scene.ui.TextField;
// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;

/**
 * Dialog form để nhập tên phòng và mật khẩu khi tạo phòng.
 */
public class CreateRoomFormDialog extends BaseDialog {
    // Mảng lưu config phòng (tên và mật khẩu)
    private final String[] roomConfig = { "", "" };
    // Mảng lưu reference đến TextField
    private final TextField[] fieldCreate = { null, null };
    // Callback khi người dùng bấm OK
    private Runnable onConfirm;

    // Constructor khởi tạo dialog
    public CreateRoomFormDialog() {
        super("@message.create-room.title");

        // Đọc tên phòng đã lưu từ settings
        roomConfig[0] = Core.settings.getString("playerConnectRoomName", Vars.player.name());
        // Đọc mật khẩu phòng đã lưu từ settings
        roomConfig[1] = Core.settings.getString("playerConnectRoomPassword", "");

        // Cấu hình buttons mặc định của dialog
        buttons.defaults().size(140f, 60f).pad(4f);

        // Thêm content vào dialog
        cont.table(table -> {
            // Thêm label "Tên server"
            table.add("@message.create-room.server-name")
                    .padRight(5f)
                    .right();

            // Thêm text field nhập tên server
            fieldCreate[0] = table.field(roomConfig[0], text -> {
                // Lưu giá trị khi thay đổi
                roomConfig[0] = text;
                // Lưu vào settings
                Core.settings.put("playerConnectRoomName", text);
            })
                    .size(320f, 54f)
                    // Validate độ dài từ 1-100 ký tự
                    .valid(t -> t.length() > 0 && t.length() <= 100)
                    .maxTextLength(100)
                    .left()
                    .get();

            // Xuống dòng và thêm label "Mật khẩu"
            table.row()
                    .add("@message.password")
                    .padRight(5f)
                    .right();

            // Thêm text field nhập mật khẩu
            fieldCreate[1] = table.field(roomConfig[1], text -> {
                // Lưu giá trị khi thay đổi
                roomConfig[1] = text;
                // Lưu vào settings
                Core.settings.put("playerConnectRoomPassword", text);
            })
                    .size(320f, 54f)
                    .maxTextLength(100)
                    .left()
                    .get();

            // Xuống dòng và thêm ô trống
            table.row().add();
        }).row();

        // Thêm nút Cancel
        buttons.button("@cancel", this::hide);

        // Thêm nút OK
        buttons.button("@ok", () -> {
            // Gọi callback nếu có
            if (onConfirm != null) {
                onConfirm.run();
            }
            // Ẩn dialog
            hide();
        })
                // Disable nếu tên rỗng hoặc quá dài
                .disabled(b -> roomConfig[0].isEmpty()
                        || roomConfig[0].length() > 100
                        || roomConfig[1].length() > 100);
    }

    /**
     * Hiển thị dialog với callback khi confirm.
     * @param onConfirm callback khi người dùng bấm OK
     */
    public void show(Runnable onConfirm) {
        this.onConfirm = onConfirm;
        show();
    }

    /**
     * Lấy mật khẩu phòng đã nhập.
     * @return mật khẩu
     */
    public String getPassword() {
        return roomConfig[1];
    }

    /**
     * Lấy tên phòng đã nhập.
     * @return tên phòng
     */
    public String getRoomName() {
        return roomConfig[0];
    }
}
