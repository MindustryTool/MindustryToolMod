// Khai báo package cho module GUI
package mindustrytool.gui;

// Import Core để truy cập bundle và graphics
import arc.Core;
// Import Color để làm việc với màu sắc
import arc.graphics.Color;
// Import Draw để vẽ
import arc.graphics.g2d.Draw;
// Import Label để hiển thị text
import arc.scene.ui.Label;
// Import ScrollPane để tạo vùng cuộn
import arc.scene.ui.ScrollPane;
// Import TextButton để tạo nút có text
import arc.scene.ui.TextButton;
// Import Cell để cấu hình layout
import arc.scene.ui.layout.Cell;
// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;
// Import Seq là collection của Arc
import arc.struct.Seq;
// Import Align để căn chỉnh text
import arc.util.Align;
// Import Log để ghi log
import arc.util.Log;
// Import Strings để xử lý chuỗi
import arc.util.Strings;
// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import Tex chứa các texture
import mindustry.gen.Tex;
// Import Pal chứa các màu palette
import mindustry.graphics.Pal;
// Import Styles chứa các style UI
import mindustry.ui.Styles;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;
// Import Config để lấy URL API
import mindustrytool.config.Config;
// Import DataServer là data class cho server
import mindustrytool.data.DataServer;

// Dialog hiển thị danh sách server Mindustry online
public class ServerDialog extends BaseDialog {

    // Danh sách dữ liệu server đã load
    private Seq<DataServer> serversData = new Seq<>();
    // Request object để xử lý phân trang
    private ServerPagingRequest<DataServer> request;

    // Constructor khởi tạo dialog
    public ServerDialog() {
        // Gọi constructor cha với tiêu đề
        super("Server Browser");

        // Tạo request với type DataServer và URL từ config
        request = new ServerPagingRequest<>(DataServer.class, Config.API_URL + "servers");

        // Thiết lập số item mỗi trang
        setItemPerPage();

        // Đăng ký listener khi resize cửa sổ
        onResize(() -> {
            // Cập nhật số item mỗi trang
            setItemPerPage();
            // Rebuild giao diện
            ServerBrowser();
        });
        // Load trang đầu tiên
        request.getPage(this::handleServerResult);
        // Đăng ký listener khi dialog hiển thị
        shown(this::ServerBrowser);
    }

    // Phương thức đặt số item mỗi trang
    private void setItemPerPage() {
        // Đặt 20 server mỗi trang
        request.setItemPerPage(20);
    }

    // Phương thức xây dựng giao diện chính
    private void ServerBrowser() {
        // Xóa nội dung cũ
        clear();

        try {
            // Thêm nút đóng
            addCloseButton();
            row();
            // Thêm thanh search/back
            table(searchBar -> {
                // Nút back để đóng dialog
                searchBar.button("@back", Icon.leftSmall, this::hide)//
                        .width(150).padLeft(2).padRight(2).left();
            }).left();
            row();
            // Thêm container chứa danh sách server
            ServerContainer();
            row();
            // Thêm footer với các nút phân trang
            Footer();
        } catch (Exception ex) {
            // Nếu có lỗi thì hiển thị thông báo lỗi
            clear();
            addCloseButton();
            table(container -> Error(container, Core.bundle.format("message.error") + "\n Error: " + ex.getMessage()));
            Log.err(ex);
        }
    }

    // Phương thức wrapper để đảm bảo không gọi khi đang loading
    public void loadingWrapper(Runnable action) {
        // Chạy trên UI thread
        Core.app.post(() -> {
            // Nếu đang loading thì hiển thị thông báo
            if (request.isLoading())
                Vars.ui.showInfoFade("Loading");
            else
                // Nếu không thì thực hiện action
                action.run();
        });
    }

    // Phương thức tạo cell hiển thị lỗi
    private Cell<TextButton> Error(Table parent, String message) {
        // Tạo button với message, click để retry
        Cell<TextButton> error = parent.button(message, Styles.nonet, () -> request.getPage(this::handleServerResult));

        // Cấu hình layout
        return error.center().labelAlign(0).expand().fill();
    }

    // Phương thức tạo cell hiển thị loading
    private Cell<Label> Loading(Table parent) {
        // Hiển thị text loading
        return parent.labelWrap(Core.bundle.format("message.loading")).center().labelAlign(0).expand().fill();
    }

    // Phương thức tạo container cuộn chứa danh sách server
    private Cell<ScrollPane> ServerScrollContainer(Table parent) {
        // Nếu không có server nào thì hiển thị thông báo
        if (serversData.size == 0)
            return parent.pane(container -> container.add("message.no-result"));

        // Tạo pane cuộn với danh sách server
        return parent.pane(container -> {
            // Tính số cột dựa trên chiều rộng màn hình
            var cols = (int) Math.max(1, Core.graphics.getWidth() / 800);

            // Biến đếm để xuống dòng
            int i = 0;
            // Duyệt qua từng server
            for (DataServer serverData : serversData) {
                try {
                    // Tạo card cho server
                    ServerCard(container, serverData);

                    // Nếu đủ số cột thì xuống dòng
                    if (++i % cols == 0) {
                        container.row();
                    }
                } catch (Exception e) {
                    // In lỗi nếu có
                    e.printStackTrace();
                }
            }
            // Căn về phía trên
            container.top();
        }).scrollY(true).expand().fill();

    }

    // Phương thức tạo card hiển thị thông tin server
    private void ServerCard(Table container, DataServer data) {
        // Căn về góc trên trái
        container.top().left();
        // Xóa background
        container.background(null);

        // Màu của card
        Color color = Pal.gray;

        // Kiểm tra có thể connect không (phải có mapName và address)
        var canConnect = data.mapName() != null && data.address() != null;

        // Tạo button cho card
        container.button(t -> {
            // Căn về góc trên trái
            t.top().left();
            // Đặt màu
            t.setColor(color);
            // Hiển thị tên server
            t.add(data.name()).left().labelAlign(Align.left);
            // Reset draw
            Draw.reset();
            t.row();

            // Nếu có description thì hiển thị (tối đa 3 dòng)
            if (!data.description().isEmpty()) {
                // Đếm số dòng
                int count = 0;
                // StringBuilder để xây dựng kết quả
                StringBuilder result = new StringBuilder(data.description().length());
                // Duyệt qua từng ký tự
                for (int i = 0; i < data.description().length(); i++) {
                    char c = data.description().charAt(i);
                    // Nếu là xuống dòng thì đếm
                    if (c == '\n') {
                        count++;
                        // Chỉ giữ lại 3 dòng đầu
                        if (count < 3)
                            result.append(c);
                    } else {
                        result.append(c);
                    }
                }
                // Hiển thị description với màu xám
                t.add("[gray]" + result).left().wrap();
                t.row();
            }

            // Hiển thị số người chơi
            t.add(Core.bundle.format("players", data.players())).left().labelAlign(Align.left);
            t.row();

            // Hiển thị tên map nếu có
            if (data.mapName() != null && !data.mapName().isEmpty()) {
                t.add("Map: " + data.mapName()).left().labelAlign(Align.left);
                t.row();

                // Hiển thị địa chỉ và port nếu có
                if (data.address() != null && !data.address().isEmpty()) {
                    t.add("Address: " + data.address() + ":" + data.port()).left().labelAlign(Align.left);
                    t.row();
                }
            }

            // Hiển thị gamemode nếu có
            if (data.gamemode() != null && !data.gamemode().isEmpty()) {
                t.add("Gamemode: " + data.gamemode()).left().labelAlign(Align.left);
                t.row();
            }

            // Hiển thị mode nếu có
            if (data.mode() != null && !data.mode().isEmpty()) {
                t.add("Mode: " + data.mode()).left().labelAlign(Align.left);
                t.row();
            }

            // Hiển thị danh sách mod nếu có
            if (data.mods() != null && !data.mods().isEmpty()) {
                t.add("Mods: " + Strings.join(", ", data.mods())).left().labelAlign(Align.left);
                t.row();
            }

        // Style button empty, click để connect
        }, Styles.emptyi, () -> {
            // Nếu có thể connect thì gọi join dialog
            if (canConnect) {
                Vars.ui.join.connect(data.address(), data.port());
            } else {
                // Nếu không thì hiển thị thông báo
                Vars.ui.showInfoFade("Cannot connect to this server.");
            }
        })//
                .growY()//
                .growX()//
                .left()//
                // Disable button nếu không thể connect
                .disabled(!canConnect)//
                .bottom()//
                .pad(8);
    }

    // Phương thức tạo footer với các nút phân trang
    private void Footer() {
        table(footer -> {
            // Nút trang trước
            footer.button(Icon.left, () -> request.previousPage(this::handleServerResult)).margin(4).pad(4).width(100)
                    // Disable nếu đang loading, ở trang đầu, hoặc có lỗi
                    .disabled(request.isLoading() || request.getPage() == 0 || request.isError()).height(40);

            // Label hiển thị số trang hiện tại
            footer.table(Tex.buttonDisabled, table -> {
                // Số trang (1-indexed để hiển thị)
                table.labelWrap(String.valueOf(request.getPage() + 1)).width(50).style(Styles.defaultLabel)
                        .labelAlign(0).center().fill();
            }).pad(4).height(40);

            // Nút nhập số trang
            footer.button(Icon.edit, () -> {
                // Hiển thị dialog nhập số trang
                Vars.ui.showTextInput("@select-page", "", "", input -> {
                    try {
                        // Parse và đặt trang mới
                        request.setPage(Integer.parseInt(input));
                        // Rebuild giao diện
                        shown(this::ServerBrowser);
                    } catch (Exception e) {
                        // Hiển thị lỗi nếu input không hợp lệ
                        Vars.ui.showInfo("Invalid input");
                    }
                });
            })//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    // Disable nếu đang loading, hết trang, hoặc có lỗi
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            // Nút trang tiếp theo
            footer.button(Icon.right, () -> request.nextPage(this::handleServerResult))//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    // Disable nếu đang loading, hết trang, hoặc có lỗi
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            // Nút upload schematic (mở URL)
            footer.button("@upload", () -> Core.app.openURI(Config.UPLOAD_SCHEMATIC_URL)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            // Căn về phía dưới
            footer.bottom();
        }).expandX().fillX();
    }

    // Phương thức tạo container chính chứa danh sách server
    private Cell<Table> ServerContainer() {
        return table(container -> {
            // Nếu đang loading thì hiển thị loading
            if (request.isLoading()) {
                Loading(container);
                return;
            }

            // Nếu có lỗi thì hiển thị error
            if (request.isError()) {
                Error(container, String.format("There is an error, reload? (%s)", request.getError()));
                return;
            }

            // Nếu không thì hiển thị danh sách server
            ServerScrollContainer(container);
        }).expand().fill().top();
    }

    // Callback xử lý kết quả từ API
    private void handleServerResult(Seq<DataServer> servers) {
        // Nếu có dữ liệu thì lưu
        if (servers != null)
            this.serversData = servers;
        else
            // Nếu không thì xóa danh sách
            this.serversData.clear();

        // Rebuild giao diện
        ServerBrowser();
    }
}
