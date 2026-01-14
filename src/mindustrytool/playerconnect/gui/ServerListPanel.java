// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;


// Import Button để tạo nút bấm
import arc.scene.ui.Button;
// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;
// Import ArrayMap là ordered map của Arc
import arc.struct.ArrayMap;

/**
 * Panel hiển thị danh sách servers.
 * Xử lý việc render và ping từng server.
 */
public class ServerListPanel {
    // Server được chọn
    private Server selected;
    // Button của server được chọn
    private Button selectedButton;

    /**
     * Lấy server đang được chọn.
     * @return server được chọn hoặc null
     */
    public Server getSelected() { return selected; }

    public Button getSelectedButton(){ return selectedButton; }

    public void setSelection(Server s, Button b){ this.selected = s; this.selectedButton = b; }

    /**
     * Reset selection về null.
     */
    public void resetSelection() {
        selected = null;
        selectedButton = null;
    }

    /**
     * Setup giao diện danh sách servers.
     * @param servers map chứa danh sách servers (key=name, value=address)
     * @param table table để render vào
     * @param editable cho phép edit hay không (unused, kept for compatibility)
     * @param onDelete callback khi xóa (unused, kept for compatibility)
     */
    public void setupServers(ArrayMap<String, String> servers, Table table, boolean editable, Runnable onDelete) {
        // Reset selection
        selected = null;
        selectedButton = null;
        // Xóa nội dung cũ
        table.clear();
        // Duyệt qua từng server
        for (int i = 0; i < servers.size; i++) {
            Server server = new Server();
            server.name = servers.getKeyAt(i);
            server.set(servers.getValueAt(i));
            ServerRow.addTo(table, server, this);
        }
    }
}
