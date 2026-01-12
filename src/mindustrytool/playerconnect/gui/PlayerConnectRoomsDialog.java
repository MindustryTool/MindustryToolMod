// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import TimeUnit để làm việc với đơn vị thời gian
import java.util.concurrent.TimeUnit;
// Import Matcher để match pattern
import java.util.regex.Matcher;
// Import Pattern để tạo regex pattern
import java.util.regex.Pattern;

// Import Core để truy cập bundle và các service
import arc.Core;
// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;
// Import Align để căn chỉnh text
import arc.util.Align;
// Import Log để ghi log
import arc.util.Log;
// Import Strings để xử lý chuỗi
import arc.util.Strings;
// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Version để lấy thông tin phiên bản
import mindustry.core.Version;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import Iconc chứa các ký tự icon
import mindustry.gen.Iconc;
// Import Styles chứa các style UI
import mindustry.ui.Styles;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;
// Import Debouncer để debounce tìm kiếm
import mindustrytool.config.Debouncer;
// Import PlayerConnect để quản lý kết nối phòng
import mindustrytool.playerconnect.PlayerConnect;
// Import PlayerConnectApi để gọi API
import mindustrytool.playerconnect.PlayerConnectApi;
// Import PlayerConnectLink để xử lý link kết nối
import mindustrytool.playerconnect.PlayerConnectLink;
// Import PlayerConnectRoom là data class cho phòng
import mindustrytool.playerconnect.data.PlayerConnectRoom;

// Dialog hiển thị danh sách các phòng Player Connect online
public class PlayerConnectRoomsDialog extends BaseDialog {
    // Table chứa danh sách phòng
    private final Table roomList = new Table();
    // Debouncer để delay tìm kiếm khi gõ liên tục
    private final Debouncer debouncer = new Debouncer(250, TimeUnit.MILLISECONDS);
    // Từ khóa tìm kiếm
    private String searchTerm = "";

    // Constructor khởi tạo dialog
    public PlayerConnectRoomsDialog() {
        // Gọi constructor cha với tiêu đề
        super("@message.room-list.title");
        // Thêm nút đóng dialog
        addCloseButton();

        try {
            // Thêm content vào dialog
            cont.table(container -> {
                // Thêm thanh tìm kiếm
                container.table(topBar -> {
                    // Thêm text field tìm kiếm
                    topBar.field(searchTerm, (result) -> {
                        // Lưu từ khóa
                        searchTerm = result;
                        // Debounce rồi mới tìm kiếm
                        debouncer.debounce(this::setupPlayerConnect);
                    })//
                            .left()
                            .growX()
                            .get()
                            // Đặt placeholder text
                            .setMessageText(Core.bundle.format("@map.search"));

                })
                        .top()
                        .left()
                        .growX();

                container.row();
                // Thêm table danh sách phòng
                container.add(roomList)
                        .grow()
                        .top()
                        .left();

                container.row();
            })
                    .top()
                    .left()
                    .grow();
            // Căn content về góc trên trái
            cont
                    .top()
                    .left();

            // Thêm nút refresh
            buttons
                    .button(Icon.refresh, Styles.squarei, () -> setupPlayerConnect())
                    .size(64)
                    .padRight(8);

            // Đăng ký listener khi dialog hiển thị
            shown(() -> {
                // Load danh sách phòng
                setupPlayerConnect();
            });
        } catch (Throwable e) {
            // Log lỗi nếu có
            Log.err(e);
        }
    }

    // Phương thức load và hiển thị danh sách phòng
    public void setupPlayerConnect() {
        // Xóa nội dung cũ
        roomList.clear();
        // Hiển thị loading text
        roomList.labelWrap(Core.bundle.format("message.loading"))
                .center()
                .labelAlign(0)
                .expand()
                .fill();

        // Gọi API lấy danh sách phòng
        PlayerConnectApi.findPlayerConnectRooms(searchTerm, rooms -> {
            // Xóa loading text
            roomList.clear();

            // Tạo pane cuộn
            roomList.pane(list -> {
                // Nếu không có phòng nào
                if (rooms.isEmpty()) {
                    // Hiển thị thông báo không tìm thấy
                    list.labelWrap(Core.bundle.format("message.no-rooms-found"))
                            .center()
                            .labelAlign(0)
                            .expand()
                            .fill();
                    return;
                }

                // Duyệt qua từng phòng
                for (PlayerConnectRoom room : rooms) {
                    // Tạo card cho phòng
                    list.table(Styles.black5, card -> {
                        // Phần bên trái chứa thông tin
                        card.table(left -> {
                            // Hiển thị tên phòng, locale, khóa và phiên bản
                            left.add(
                                    room.data().name() + "(" + room.data().locale() + ") [white]"
                                            + (room.data().isSecured() ? Iconc.lock : "")
                                            + " " + getVersionString(room.data().version()))
                                    .fontScale(1.5f)
                                    .align(Align.left)
                                    .left();

                            left.row();
                            // Hiển thị tên map và gamemode
                            left.add(Iconc.map + " " + Core.bundle.format("save.map", room.data().mapName())
                                    + "[lightgray] / " + room.data().gamemode())
                                    .align(Align.left).left();

                            left.row();
                            // Hiển thị số người chơi
                            left.add(
                                    Iconc.players + " " + Core.bundle.format("players", room.data().players().size))
                                    .align(Align.left)
                                    .left();

                            left.row();
                            // Hiển thị phiên bản
                            left.add(Core.bundle.format("version") + ": " + room.data().version())
                                    .align(Align.left)
                                    .left();

                            // Nếu có mod thì hiển thị
                            if (room.data().mods().size > 0) {
                                left.row();
                                // Hiển thị danh sách mod
                                left.add(Iconc.book + " " + Strings.join(",", room.data().mods())).align(Align.left)
                                        .left();
                            }
                        })
                                .top()
                                .left();

                        // Khoảng trống giữa
                        card.add().growX().width(-1);

                        // Phần bên phải chứa nút Join
                        card.table(right -> {
                            // Nút Join
                            right.button(Iconc.play + " " + Core.bundle.format("join"), () -> {
                                // Nếu phòng không có mật khẩu
                                if (!room.data().isSecured()) {
                                    try {
                                        // Join trực tiếp
                                        PlayerConnect.joinRoom(
                                                PlayerConnectLink.fromString(room.link()), "",
                                                () -> hide());
                                    } catch (Throwable e) {
                                        // Xử lý lỗi
                                        hide();
                                        setupPlayerConnect();
                                        Vars.ui.showException("@message.connect.fail", e);
                                    }

                                    return;
                                }

                                // Nếu phòng có mật khẩu thì hiển thị dialog nhập password
                                BaseDialog connect = new BaseDialog("@message.type-password.title");
                                // Mảng lưu password
                                String[] password = { "" };

                                // Thêm content
                                connect.cont.table(table -> {
                                    // Label "Mật khẩu"
                                    table.add("@message.password")
                                            .padRight(5f)
                                            .right();

                                    // Text field nhập password
                                    table.field(password[0], text -> password[0] = text)
                                            .size(320f, 54f)
                                            // Validate độ dài
                                            .valid(t -> t.length() > 0 && t.length() <= 100)
                                            .maxTextLength(100)
                                            .left()
                                            .get();
                                    table.row().add();
                                }).row();

                                // Nút Cancel
                                connect.buttons.button("@cancel", () -> {
                                    connect.hide();
                                }).minWidth(210);

                                // Nút OK
                                connect.buttons.button("@ok", () -> {
                                    try {
                                        // Join với password
                                        PlayerConnect.joinRoom(
                                                PlayerConnectLink.fromString(room.link()),
                                                password[0],
                                                () -> {
                                                    // Ẩn cả 2 dialog khi thành công
                                                    hide();
                                                    connect.hide();
                                                });
                                    } catch (Throwable e) {
                                        // Xử lý lỗi
                                        hide();
                                        connect.hide();
                                        setupPlayerConnect();
                                        Vars.ui.showException("@message.connect.fail", e);
                                    }
                                }).minWidth(210);

                                // Hiển thị dialog password
                                connect.show();
                            })
                                    .minWidth(150);
                        });
                    })
                            .growX()
                            .left()
                            .top()
                            .margin(8)
                            .pad(8);

                    // Xuống dòng cho phòng tiếp theo
                    list.row();
                }
            })
                    .top()
                    .left()
                    .fill()
                    .expandX()
                    // Chỉ scroll dọc
                    .scrollX(false)
                    .scrollY(true);

            // Cấu hình margin cho roomList
            roomList
                    .top()
                    .left()
                    .marginTop(8)
                    .marginBottom(8);

        });
    }

    // Phương thức lấy chuỗi hiển thị phiên bản với thông báo tương thích
    private String getVersionString(String versionString) {
        // Parse thông tin build từ string
        BuildInfo info = extract(versionString);
        // Lấy số build
        int version = info.build;
        // Lấy loại build
        String versionType = info.type;

        // Nếu build là -1 (custom build)
        if (version == -1) {
            return Core.bundle.format("server.version", Core.bundle.get("server.custombuild"), "");
        // Nếu build là 0 (quá cũ)
        } else if (version == 0) {
            return Core.bundle.get("server.outdated");
        // Nếu server cũ hơn client
        } else if (version < Version.build && Version.build != -1) {
            return Core.bundle.get("server.outdated") + "\n" +
                    Core.bundle.format("server.version", version, "");
        // Nếu server mới hơn client
        } else if (version > Version.build && Version.build != -1) {
            return Core.bundle.get("server.outdated.client") + "\n" +
                    Core.bundle.format("server.version", version, "");
        // Nếu cùng phiên bản và cùng loại
        } else if (version == Version.build && Version.type.equals(versionType)) {
            return "";
        } else {
            // Trường hợp khác: hiển thị phiên bản
            return Core.bundle.format("server.version", version, versionType);
        }
    }

    // Lớp nội bộ lưu thông tin build đã parse
    private static class BuildInfo {
        // Loại build (official, custom, bleeding-edge...)
        public String type = "custom";
        // Số build
        public int build = -1;
        // Số revision
        public int revision = -1;
        // Modifier bổ sung
        public String modifier;

        // Chuyển đổi thành string để debug
        @Override
        public String toString() {
            return "type=" + type + ", build=" + build + ", revision=" + revision + ", modifier=" + modifier;
        }
    }

    // Phương thức parse chuỗi phiên bản thành BuildInfo
    private BuildInfo extract(String combined) {
        // Tạo object mới
        BuildInfo info = new BuildInfo();

        // Nếu là "custom build" đặc biệt
        if ("custom build".equals(combined)) {
            info.type = "custom";
            info.build = -1;
            info.revision = 0;
            info.modifier = null;
            return info;
        }

        // Pattern: "type build number.revision"
        Pattern pattern = Pattern.compile("^(.+?) build (\\d+)(?:\\.(\\d+))?$");
        // Match với chuỗi input
        Matcher matcher = pattern.matcher(combined);

        // Nếu match thành công
        if (matcher.matches()) {
            // Lấy phần đầu (type hoặc modifier)
            String first = matcher.group(1);
            // Parse số build
            info.build = Integer.parseInt(matcher.group(2));
            // Parse revision nếu có, không thì 0
            info.revision = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            // Nếu là "official"
            if ("official".equals(first)) {
                info.type = "official";
                info.modifier = first;
            } else {
                // Các loại khác
                info.type = first;
                info.modifier = null;
            }
        } else {
            // Nếu không match được format thì log warning
            Log.warn("Invalid combined() format: " + combined);
        }

        // Trả về thông tin đã parse
        return info;
    }
}
