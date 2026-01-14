// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;
// Import Timer để lên lịch chạy task
import arc.util.Timer;

// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;
// Import PlayerConnect từ net package
import mindustrytool.playerconnect.net.PlayerConnect;
// Import PlayerConnectLink từ data package
import mindustrytool.playerconnect.data.PlayerConnectLink;

/**
 * Dialog chính để tạo và quản lý phòng Player Connect.
 * Sử dụng các component UI đã tách rời.
 */
public class CreateRoomDialog extends BaseDialog {
    // Link của phòng đã tạo
    private PlayerConnectLink link;
    // Panel xử lý danh sách servers
    private final ServerListPanel serverListPanel;
    // Section online servers
    private final OnlineServersSection onlineSection;
    // Dialog form tạo phòng
    private final CreateRoomFormDialog createFormDialog;

    // Constructor khởi tạo dialog
    public CreateRoomDialog() {
        // Gọi constructor cha với tiêu đề
        super("@message.manage-room.title");

        // Khởi tạo các component UI TRƯỚC khi sử dụng trong lambda
        serverListPanel = new ServerListPanel();
        onlineSection = new OnlineServersSection(serverListPanel);
        createFormDialog = new CreateRoomFormDialog();

        // Đăng ký event để đóng phòng khi host game mới
        arc.Events.run(mindustry.game.EventType.HostEvent.class, this::closeRoom);

        // Tạo overlay cho các button
        makeButtonOverlay();
        // Thêm nút đóng dialog
        addCloseButton();

        // Đăng ký listener khi dialog hiển thị
        shown(() -> {
            // Delay 7 frame rồi refresh danh sách
            arc.util.Time.run(7f, () -> {
                onlineSection.refresh();
            });
        });

        // Thêm nút tạo phòng vào dialog chính
        buttons.button("@message.manage-room.create-room", Icon.add, () -> {
            createFormDialog.show(() -> createRoom(createFormDialog.getPassword()));
        })
                // Disable nếu phòng chưa đóng hoặc chưa chọn server
                .disabled(b -> !PlayerConnect.isRoomClosed() || serverListPanel.getSelected() == null);

        // Nếu mobile thì xuống dòng
        if (Vars.mobile)
            buttons.row();

        // Thêm nút đóng phòng
        buttons.button("@message.manage-room.close-room", Icon.cancel, this::closeRoom)
                // Disable nếu phòng đã đóng
                .disabled(b -> PlayerConnect.isRoomClosed());

        // Thêm nút copy link
        buttons.button("@message.manage-room.copy-link", Icon.copy, this::copyLink)
                // Disable nếu chưa có link
                .disabled(b -> link == null);

        // Tạo pane chứa danh sách servers
        cont.pane(hosts -> {
            // Build section online servers
            onlineSection.build(hosts);
            // Thêm margin dưới để không bị che bởi buttons
            hosts.marginBottom(Vars.mobile ? 140f : 70f);
        })
                .get()
                // Chỉ scroll dọc
                .setScrollingDisabled(true, false);

        // Thêm nút vào pause menu
        setupPauseMenuButton();
    }

    /**
     * Setup nút trong pause menu.
     */
    private void setupPauseMenuButton() {
        Vars.ui.paused.shown(() -> {
            // Lấy root table của pause dialog
            Table root = Vars.ui.paused.cont;
            // Lấy danh sách cells
            @SuppressWarnings("rawtypes")
            arc.struct.Seq<arc.scene.ui.layout.Cell> buttons = root.getCells();

            // Nếu là mobile
            if (Vars.mobile) {
                // Thêm button với style khác
                root.row()
                        .buttonRow("@message.manage-room.title", Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server()).row();
                return;

            // Nếu button cuối cùng có colspan = 2
            } else if (arc.util.Reflect.<Integer>get(buttons.get(buttons.size - 2), "colspan") == 2) {
                // Thêm button với colspan = 2
                root.row()
                        .button("@message.manage-room.title", Icon.planet, this::show)
                        .colspan(2)
                        .width(450f)
                        .disabled(button -> !Vars.net.server())
                        .row();

            } else {
                // Thêm button bình thường
                root.row()
                        .button("@message.manage-room.title", Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server())
                        .row();
            }
            // Swap vị trí 2 button cuối để button mới lên trước
            buttons.swap(buttons.size - 1, buttons.size - 2);
        });
    }

    /**
     * Tạo phòng với mật khẩu.
     * @param password mật khẩu phòng
     */
    public void createRoom(String password) {
        // Lấy server được chọn
        Server selected = serverListPanel.getSelected();
        // Nếu chưa chọn server thì return
        if (selected == null)
            return;

        // Hiển thị loading dialog
        Vars.ui.loadfrag.show("@message.manage-room.create-room");
        // Reset link cũ
        link = null;
        // Đặt timeout 10 giây để đóng phòng nếu không thành công
        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        // Gọi API tạo phòng
        PlayerConnect.createRoom(selected.ip, selected.port, password, l -> {
            // Callback thành công: ẩn loading
            Vars.ui.loadfrag.hide();
            // Hủy timeout
            t.cancel();
            // Lưu link
            link = l;
        }, e -> {
            // Callback lỗi: xử lý exception
            Vars.net.handleException(e);
            // Hủy timeout
            t.cancel();
        }, r -> {
            // Callback phòng bị đóng: ẩn loading
            Vars.ui.loadfrag.hide();
            // Hủy timeout
            t.cancel();
            // Nếu có lý do thì hiển thị thông báo
            if (r != null) {
                Vars.ui.showText("", "@message.room." + arc.util.Strings.camelToKebab(r.name()));
            // Nếu không có lý do và chưa có link thì hiển thị lỗi
            } else if (link == null) {
                Vars.ui.showErrorMessage("@message.manage-room.create-room.failed");
            }
            // Reset link
            link = null;
        });
    }

    /**
     * Đóng phòng hiện tại.
     */
    public void closeRoom() {
        // Gọi hàm đóng phòng
        PlayerConnect.closeRoom();
        // Reset link
        link = null;
    }

    /**
     * Copy link phòng vào clipboard.
     */
    public void copyLink() {
        // Nếu chưa có link thì return
        if (link == null) {
            return;
        }

        // Copy link vào clipboard
        arc.Core.app.setClipboardText(link.toString());
        // Hiển thị thông báo đã copy
        Vars.ui.showInfoFade("@copied");
    }
}
