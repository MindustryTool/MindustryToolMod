// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;

// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import Pal chứa các màu palette
import mindustry.graphics.Pal;
// Import Styles chứa các style UI
import mindustry.ui.Styles;
// Import PlayerConnectProviders để quản lý danh sách providers
import mindustrytool.playerconnect.PlayerConnectProviders;

/**
 * UI component cho section danh sách online servers.
 * Hiển thị header với nút refresh/toggle và danh sách servers.
 */
public class OnlineServersSection {
    // Table chứa danh sách online servers
    private Table onlineTable;
    // Cờ đánh dấu section đang hiển thị
    private boolean shown = true;
    // Cờ đánh dấu đang refresh
    private boolean refreshing;
    // Panel xử lý server list
    private final ServerListPanel serverListPanel;

    /**
     * Constructor khởi tạo section.
     * @param serverListPanel panel xử lý server list
     */
    public OnlineServersSection(ServerListPanel serverListPanel) {
        this.serverListPanel = serverListPanel;
    }

    /**
     * Build UI vào container.
     * @param container table chứa section
     */
    public void build(Table container) {
        // Section public servers
        container.table(table -> {
            // Tiêu đề section
            table.add("@message.manage-room.public-servers")
                    .pad(10)
                    .padLeft(0)
                    .color(Pal.accent)
                    .growX()
                    .left();

            // Nút refresh danh sách online (section cố định - không thu gọn)
            table.button(Icon.refresh, Styles.emptyi, this::refresh)
                .size(40f)
                .right()
                .padRight(3);

        })
                .pad(0, 5, 0, 5)
                .growX()
                .row();

        // Đường kẻ ngang
        container.image()
                .pad(5)
                .height(3)
                .color(Pal.accent)
                .growX()
                .row();


        // Fixed table chứa danh sách online servers (không thu gọn)
        container.table(table -> onlineTable = table)
            .pad(0, 5, 10, 5)
            .growX()
            .row();

        container.row();
    }

    /**
     * Refresh danh sách online servers.
     */
    public void refresh() {
        // Nếu đang refresh thì bỏ qua
        if (refreshing) {
            return;
        }

        // Đánh dấu đang refresh
        refreshing = true;
        // Gọi API lấy danh sách providers
        PlayerConnectProviders.refreshOnline(() -> {
            // Callback thành công: reset cờ và setup giao diện
            refreshing = false;
            // Không cho phép edit online servers
            serverListPanel.setupServers(PlayerConnectProviders.online, onlineTable, false, null);
        }, e -> {
            // Callback lỗi: reset cờ và hiển thị lỗi
            refreshing = false;
            Vars.ui.showException("@message.room.fetch-failed", e);
        });
    }

    /**
     * Lấy table chứa danh sách online servers.
     * @return online table
     */
    public Table getOnlineTable() {
        return onlineTable;
    }
}
