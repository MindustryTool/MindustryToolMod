// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import PlayerConnect để quản lý kết nối phòng
import mindustrytool.playerconnect.PlayerConnect;
// Import PlayerConnectLink để xử lý link kết nối
import mindustrytool.playerconnect.PlayerConnectLink;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;

// Dialog để join vào phòng Player Connect bằng link
public class JoinRoomDialog extends BaseDialog {
    // Link cuối cùng được nhập, khởi tạo với scheme mặc định
    String lastLink = "player-connect://";
    // Mật khẩu phòng
    String password = "";
    // Cờ đánh dấu link có hợp lệ không
    boolean isValid;
    // Thông báo kết quả validate
    String output;
    
    // Reference đến dialog danh sách phòng
    private PlayerConnectRoomsDialog roomsDialog;

    // Constructor nhận reference đến PlayerConnectRoomsDialog
    public JoinRoomDialog(PlayerConnectRoomsDialog roomsDialog) {
        // Gọi constructor cha với tiêu đề
        super("@message.join-room.title");
        // Lưu reference
        this.roomsDialog = roomsDialog;

        // Cấu hình chiều rộng mặc định cho content
        cont.defaults().width(Vars.mobile ? 350f : 550f);

        // Thêm content vào dialog
        cont.table(table -> {
            // Thêm label "Link"
            table.add("@message.join-room.link")
                    .padRight(5f)
                    .left();

            // Thêm text field nhập link
            table.field(lastLink, this::setLink)
                    .maxTextLength(100)
                    // Validate link khi thay đổi
                    .valid(this::setLink)
                    .height(54f)
                    .growX()
                    .row();

            // Thêm label "Mật khẩu"
            table.add("@message.password")
                    .padRight(5f)
                    .left();

            // Thêm text field nhập mật khẩu
            table.field(password, text -> password = text)
                    .maxTextLength(100)
                    .height(54f)
                    .growX()
                    .row();

            // Thêm ô trống và label hiển thị kết quả validate
            table.add();
            table.labelWrap(() -> output)
                    .left()
                    .growX()
                    .row();
        }).row();

        // Cấu hình buttons mặc định
        buttons.defaults()
                .size(140f, 60f)
                .pad(4f);

        // Thêm nút Cancel
        buttons.button("@cancel", this::hide);

        // Thêm nút OK để join phòng
        buttons.button("@ok", this::joinRoom)
                // Disable nếu link không hợp lệ, rỗng, hoặc đang có kết nối khác
                .disabled(button -> !isValid || lastLink.isEmpty() || Vars.net.active());

        // Thêm nút vào dialog danh sách phòng để mở dialog này
        roomsDialog.buttons
                .button("@message.join-room.title", Icon.play, this::show);
    }

    // Phương thức join vào phòng
    public void joinRoom() {
        // Kiểm tra tên người chơi không rỗng
        if (Vars.player.name.trim().isEmpty()) {
            Vars.ui.showInfo("@noname");
            return;
        }

        // Biến lưu link đã parse
        PlayerConnectLink link;
        try {
            // Parse link từ string
            link = PlayerConnectLink.fromString(lastLink);
        } catch (Exception e) {
            // Nếu lỗi thì đánh dấu không hợp lệ và hiển thị lỗi
            isValid = false;
            Vars.ui.showErrorMessage(arc.Core.bundle.get("message.join-room.invalid") + ' ' + e.getLocalizedMessage());
            return;
        }

        // Hiển thị loading dialog
        Vars.ui.loadfrag.show("@connecting");
        // Đặt nút để hủy kết nối
        Vars.ui.loadfrag.setButton(() -> {
            // Ẩn loading
            Vars.ui.loadfrag.hide();
            // Ngắt kết nối
            Vars.netClient.disconnectQuietly();
        });

        // Delay 2 frame rồi join phòng
        arc.util.Time.runTask(2f, () -> PlayerConnect.joinRoom(link, password, () -> {
            // Callback thành công: ẩn cả 2 dialog
            roomsDialog.hide();
            hide();
        }));
    }

    // Phương thức validate và set link
    public boolean setLink(String link) {
        // Nếu giống link trước thì trả về kết quả cũ
        if (lastLink.equals(link)) {
            return isValid;
        }

        // Lưu link mới
        lastLink = link;
        try {
            // Thử parse link
            PlayerConnectLink.fromString(lastLink);
            // Nếu thành công thì hiển thị thông báo hợp lệ
            output = "@message.join-room.valid";
            return isValid = true;

        } catch (Exception e) {
            // Nếu lỗi thì hiển thị thông báo lỗi
            output = arc.Core.bundle.get("message.join-room.invalid") + ' ' + e.getLocalizedMessage();
            return isValid = false;
        }
    }
}
