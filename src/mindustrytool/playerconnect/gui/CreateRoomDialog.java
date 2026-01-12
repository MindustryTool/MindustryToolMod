// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

// Import Core để truy cập settings và app
import arc.Core;
// Import Color để làm việc với màu sắc
import arc.graphics.Color;
// Import Draw để vẽ
import arc.graphics.g2d.Draw;
// Import Button để tạo nút bấm
import arc.scene.ui.Button;
// Import Dialog để tạo popup dialog
import arc.scene.ui.Dialog;
// Import TextField để tạo ô nhập text
import arc.scene.ui.TextField;
// Import Stack để xếp chồng các widget
import arc.scene.ui.layout.Stack;
// Import Table để tạo layout dạng bảng
import arc.scene.ui.layout.Table;
// Import ArrayMap là ordered map của Arc
import arc.struct.ArrayMap;
// Import Strings để xử lý chuỗi
import arc.util.Strings;
// Import Time để lấy thời gian
import arc.util.Time;
// Import Timer để lên lịch chạy task
import arc.util.Timer;

// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Icon chứa các icon
import mindustry.gen.Icon;
// Import Pal chứa các màu palette
import mindustry.graphics.Pal;
// Import Styles chứa các style UI
import mindustry.ui.Styles;
// Import BaseDialog là lớp cơ sở cho dialog
import mindustry.ui.dialogs.BaseDialog;
// Import PlayerConnect để quản lý kết nối phòng
import mindustrytool.playerconnect.PlayerConnect;
// Import PlayerConnectLink để xử lý link kết nối
import mindustrytool.playerconnect.PlayerConnectLink;
// Import PlayerConnectProviders để quản lý danh sách providers
import mindustrytool.playerconnect.PlayerConnectProviders;

// Dialog để tạo và quản lý phòng Player Connect
public class CreateRoomDialog extends BaseDialog {
    // Link của phòng đã tạo
    PlayerConnectLink link;
    // Server được chọn và server đang rename
    Server selected, renaming;
    // Key cũ của server đang rename
    int renamingOldKey;
    // Button của server được chọn
    Button selectedButton;
    // Dialog thêm server và tạo phòng
    Dialog add, create;
    // Table chứa danh sách custom và online servers
    Table custom, online;
    // Cờ đánh dấu section nào đang hiển thị
    boolean customShown = true, onlineShown = true, refreshingOnline;

    // Constructor khởi tạo dialog
    public CreateRoomDialog() {
        // Gọi constructor cha với tiêu đề
        super("@message.manage-room.title");
        // Đăng ký event để đóng phòng khi host game mới
        arc.Events.run(mindustry.game.EventType.HostEvent.class, this::closeRoom);

        // Cấu hình chiều rộng mặc định cho content
        cont.defaults().width(Vars.mobile ? 480f : 850f);

        // Tạo overlay cho các button
        makeButtonOverlay();
        // Thêm nút đóng dialog
        addCloseButton();

        // Đăng ký listener khi dialog hiển thị
        shown(() -> {
            // Delay 7 frame rồi refresh danh sách
            arc.util.Time.run(7f, () -> {
                refreshCustom();
                refreshOnline();
            });
        });

        // Object tạm để validate địa chỉ server
        Server tmp = new Server();
        // Mảng lưu giá trị đang edit (tên và địa chỉ)
        String[] lastEdit = { "", "" };
        // Mảng lưu reference đến TextField
        TextField[] fieldEdit = { null, null };

        // Mảng lưu config phòng (tên và mật khẩu)
        String[] roomConfig = { "", "" };
        // Mảng lưu reference đến TextField
        TextField[] fieldCreate = { null, null };

        // Đọc tên phòng đã lưu từ settings
        roomConfig[0] = Core.settings.getString("playerConnectRoomName", Vars.player.name());
        // Đọc mật khẩu phòng đã lưu từ settings
        roomConfig[1] = Core.settings.getString("playerConnectRoomPassword", "");

        // Tạo dialog tạo phòng
        create = new BaseDialog("@message.create-room.title");

        // Cấu hình buttons mặc định của dialog
        create.buttons.defaults().size(140f, 60f).pad(4f);
        // Thêm content vào dialog
        create.cont.table(table -> {
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
        create.buttons.button("@cancel", () -> {
            // Ẩn dialog
            create.hide();
        });

        // Thêm nút OK
        create.buttons.button("@ok", () -> {
            // Tạo phòng với mật khẩu
            createRoom(roomConfig[1]);
            // Ẩn dialog
            create.hide();
        })
                // Disable nếu tên rỗng hoặc quá dài
                .disabled(b -> roomConfig[0].isEmpty()
                        || roomConfig[0].length() > 100
                        || roomConfig[1].length() > 100);

        // Thêm nút tạo phòng vào dialog chính
        buttons.button("@message.manage-room.create-room", Icon.add, create::show)
                // Disable nếu phòng chưa đóng hoặc chưa chọn server
                .disabled(b -> !PlayerConnect.isRoomClosed() || selected == null);
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

        // Tạo dialog thêm/sửa server
        add = new BaseDialog("@joingame.title");

        // Cấu hình buttons mặc định
        add.buttons.defaults().size(140f, 60f).pad(4f);
        // Thêm content
        add.cont.table(table -> {
            // Thêm label "Tên server"
            table.add("@message.manage-room.server-name")
                    .padRight(5f)
                    .right();

            // Thêm text field nhập tên
            fieldEdit[0] = table.field(lastEdit[0], text -> lastEdit[0] = text)
                    .size(320f, 54f)
                    .maxTextLength(100)
                    .left()
                    .get();

            // Xuống dòng và thêm label "IP"
            table.row()
                    .add("@joingame.ip")
                    .padRight(5f)
                    .right();

            // Thêm text field nhập địa chỉ IP:port
            fieldEdit[1] = table.field(lastEdit[1], tmp::set)
                    .size(320f, 54f)
                    // Validate địa chỉ
                    .valid(t -> tmp.set(lastEdit[1] = t))
                    .maxTextLength(100)
                    .left()
                    .get();

            // Xuống dòng và thêm ô trống
            table.row().add();
            // Thêm label hiển thị lỗi
            table.label(() -> tmp.error)
                    .width(320f)
                    .left()
                    .row();
        }).row();

        // Thêm nút Cancel vào dialog add/edit server
        add.buttons.button("@cancel", () -> {
            // Nếu đang rename thì reset
            if (renaming != null) {
                renaming = null;
                lastEdit[0] = lastEdit[1] = "";
            }
            // Ẩn dialog
            add.hide();
        });

        // Thêm nút OK
        add.buttons.button("@ok", () -> {
            // Nếu đang rename
            if (renaming != null) {
                // Xóa entry cũ
                PlayerConnectProviders.custom.removeIndex(renamingOldKey);
                // Thêm entry mới tại vị trí cũ
                PlayerConnectProviders.custom.insert(renamingOldKey, lastEdit[0], lastEdit[1]);
                // Reset state
                renaming = null;
                renamingOldKey = -1;
            } else
                // Nếu thêm mới thì put vào map
                PlayerConnectProviders.custom.put(lastEdit[0], lastEdit[1]);
            // Lưu danh sách vào settings
            PlayerConnectProviders.saveCustom();
            // Refresh giao diện
            refreshCustom();
            // Ẩn dialog
            add.hide();
            // Reset giá trị
            lastEdit[0] = lastEdit[1] = "";
        // Disable nếu địa chỉ không hợp lệ hoặc thiếu tên/địa chỉ
        }).disabled(b -> !tmp.wasValid || lastEdit[0].isEmpty() || lastEdit[1].isEmpty());

        // Đăng ký listener khi dialog add hiển thị
        add.shown(() -> {
            // Đổi tiêu đề tùy theo đang edit hay add
            add.title.setText(renaming != null ? "@server.edit" : "@server.add");
            // Nếu đang rename thì điền sẵn giá trị cũ
            if (renaming != null) {
                fieldEdit[0].setText(renaming.name);
                fieldEdit[1].setText(renaming.get());
                lastEdit[0] = renaming.name;
                lastEdit[1] = renaming.get();
            } else {
                // Nếu thêm mới thì xóa text
                fieldEdit[0].clearText();
                fieldEdit[1].clearText();
            }
        });

        // Tạo pane chứa danh sách servers
        cont.pane(hosts -> {
            // Section custom servers
            hosts.table(table -> {
                // Tiêu đề section
                table.add("@message.manage-room.custom-servers")
                        .pad(10)
                        .padLeft(0)
                        .color(Pal.accent)
                        .growX()
                        .left();

                // Nút thêm server
                table.button(Icon.add, Styles.emptyi, add::show)
                        .size(40f)
                        .right()
                        .padRight(3);

                // Nút refresh danh sách custom
                table.button(Icon.refresh, Styles.emptyi, this::refreshCustom)
                        .size(40f)
                        .right()
                        .padRight(3);

                // Nút toggle hiển thị section
                table.button(Icon.downOpen, Styles.emptyi, () -> customShown = !customShown)
                        // Cập nhật icon tùy theo trạng thái
                        .update(i -> i.getStyle().imageUp = !customShown ? Icon.upOpen : Icon.downOpen)
                        .size(40f)
                        .right();

            })
                    .pad(0, 5, 0, 5)
                    .growX()
                    .row();

            // Đường kẻ ngang
            hosts.image()
                    .pad(5)
                    .height(3)
                    .color(Pal.accent)
                    .growX()
                    .row();

            // Collapser chứa danh sách custom servers
            hosts.collapser(table -> custom = table, false, () -> customShown)
                    .pad(0, 5, 10, 5)
                    .growX();

            hosts.row();

            // Section public servers
            hosts.table(table -> {
                // Tiêu đề section
                table.add("@message.manage-room.public-servers")
                        .pad(10)
                        .padLeft(0)
                        .color(Pal.accent)
                        .growX()
                        .left();

                // Nút refresh danh sách online
                table.button(Icon.refresh, Styles.emptyi, this::refreshOnline)
                        .size(40f)
                        .right()
                        .padRight(3);

                // Nút toggle hiển thị section
                table.button(Icon.downOpen, Styles.emptyi, () -> onlineShown = !onlineShown)
                        // Cập nhật icon tùy theo trạng thái
                        .update(i -> i.getStyle().imageUp = !onlineShown ? Icon.upOpen : Icon.downOpen)
                        .size(40f)
                        .right();

            })
                    .pad(0, 5, 0, 5)
                    .growX()
                    .row();

            // Đường kẻ ngang
            hosts.image()
                    .pad(5)
                    .height(3)
                    .color(Pal.accent)
                    .growX()
                    .row();

            // Collapser chứa danh sách online servers
            hosts.collapser(table -> online = table, false, () -> onlineShown)
                    .pad(0, 5, 10, 5)
                    .growX();

            hosts.row();

            // Thêm margin dưới để không bị che bởi buttons
            hosts.marginBottom(Vars.mobile ? 140f : 70f);
        })
                .get()
                // Chỉ scroll dọc
                .setScrollingDisabled(true, false);

        // Thêm nút vào pause menu
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

    // Phương thức refresh danh sách custom servers
    void refreshCustom() {
        // Load từ settings
        // Load từ settings
        PlayerConnectProviders.loadCustom();
        // Setup giao diện cho custom servers, cho phép edit
        setupServers(PlayerConnectProviders.custom, custom, true, () -> {
            // Callback khi xóa server: lưu và refresh
            PlayerConnectProviders.saveCustom();
            refreshCustom();
        });
    }

    // Phương thức refresh danh sách online servers
    void refreshOnline() {
        // Nếu đang refresh thì bỏ qua
        if (refreshingOnline) {
            return;
        }

        // Đánh dấu đang refresh
        refreshingOnline = true;
        // Gọi API lấy danh sách providers
        PlayerConnectProviders.refreshOnline(() -> {
            // Callback thành công: reset cờ và setup giao diện
            refreshingOnline = false;
            // Không cho phép edit online servers
            setupServers(PlayerConnectProviders.online, online, false, null);
        }, e -> {
            // Callback lỗi: reset cờ và hiển thị lỗi
            refreshingOnline = false;
            Vars.ui.showException("@message.room.fetch-failed", e);
        });
    }

    // Phương thức setup giao diện danh sách servers
    void setupServers(ArrayMap<String, String> servers, Table table, boolean editable, Runnable onDelete) {
        // Reset selection
        selected = null;
        // Xóa nội dung cũ
        table.clear();
        // Duyệt qua từng server
        for (int i = 0; i < servers.size; i++) {
            // Tạo object Server để lưu thông tin
            Server server = new Server();
            // Lấy tên từ key
            server.name = servers.getKeyAt(i);
            // Parse địa chỉ từ value
            server.set(servers.getValueAt(i));

            // Tạo button cho server này
            Button button = new Button();
            // Cấu hình style checked giống như over
            button.getStyle().checkedOver = button.getStyle().checked = button.getStyle().over;
            // Cho phép programmatic change events
            button.setProgrammaticChangeEvents(true);
            // Đăng ký click handler
            button.clicked(() -> {
                // Set server được chọn
                selected = server;
                // Set button được chọn
                selectedButton = button;
            });
            // Thêm button vào table
            table.add(button).checked(b -> selectedButton == b).growX().padTop(5).padBottom(5).row();

            // Tạo Stack để xếp chồng các widget
            Stack stack = new Stack();
            // Tạo table inner
            Table inner = new Table();
            // Đặt màu
            inner.setColor(Pal.gray);
            // Reset draw
            Draw.reset();

            // Xóa children mặc định của button
            button.clearChildren();
            // Thêm stack vào button
            button.add(stack).growX().row();

            // Tạo table chứa thông tin ping
            Table ping = inner.table(t -> {
            })
                    .margin(0)
                    .pad(0)
                    .left()
                    .fillX()
                    .get();

            // Thêm khoảng trống để đẩy các phần tử về phải
            inner.add().expandX();
            // Tạo table chứa label
            Table label = new Table().center();
            // Nếu mobile hoặc tên quá dài thì hiển thị 2 dòng
            if (Vars.mobile || (servers.getKeyAt(i) + " (" + servers.getValueAt(i) + ')').length() > 54) {
                // Dòng 1: tên server
                label.add(servers.getKeyAt(i))
                        .pad(5, 5, 0, 5)
                        .expandX()
                        .row();

                // Dòng 2: địa chỉ
                label.add(" [lightgray](" + servers.getValueAt(i) + ')').pad(5, 0, 5, 5).expandX();
            } else
                // Nếu ngắn thì hiển thị 1 dòng
                label.add(servers.getKeyAt(i) + " [lightgray](" + servers.getValueAt(i) + ')').pad(5).expandX();

            // Thêm label vào stack
            stack.add(label);
            // Thêm inner vào stack
            stack.add(inner);

            // Nếu cho phép edit
            if (editable) {
                // Lưu index cho lambda
                final int i0 = i;
                // Nếu mobile
                if (Vars.mobile) {
                    // Thêm nút edit
                    inner.button(Icon.pencil, Styles.emptyi, () -> {
                        // Set server đang rename
                        renaming = server;
                        // Lưu key cũ
                        renamingOldKey = i0;
                        // Hiển thị dialog
                        add.show();
                    })
                            .size(30f)
                            .pad(2, 5, 2, 5)
                            .right();

                    // Thêm nút xóa
                    inner.button(Icon.trash, Styles.emptyi, () -> {
                        // Hiển thị confirm dialog
                        Vars.ui.showConfirm("@confirm", "@server.delete", () -> {
                            // Xóa server
                            servers.removeKey(server.name);
                            // Gọi callback
                            if (onDelete != null) {
                                onDelete.run();
                            }
                        });
                    })
                            .size(30f)
                            .pad(2, 5, 2, 5)
                            .right();
                } else {
                    // Desktop: nút edit nhỏ hơn
                    inner.button(Icon.pencilSmall, Styles.emptyi, () -> {
                        renaming = server;
                        renamingOldKey = i0;
                        add.show();
                    })
                            .pad(4)
                            .right();

                    // Desktop: nút xóa nhỏ hơn
                    inner.button(Icon.trashSmall, Styles.emptyi, () -> {
                        Vars.ui.showConfirm("@confirm", "@server.delete", () -> {
                            servers.removeKey(server.name);
                            if (onDelete != null)
                                onDelete.run();
                        });
                    }).pad(2).right();
                }
            }

            // Hiển thị animation đang ping
            ping.label(() -> Strings.animated(Time.time, 4, 11, "."))
                    .pad(2)
                    .color(Pal.accent)
                    .left();

            // Thực hiện ping đến server
            PlayerConnect.pingHost(server.ip, server.port, ms -> {
                // Callback thành công: xóa nội dung cũ
                ping.clear();
                // Hiển thị icon OK màu xanh
                ping.image(Icon.ok)
                        .color(Color.green)
                        .padLeft(5)
                        .padRight(5)
                        .left();

                // Nếu mobile thì hiển thị ping ở dòng mới
                if (Vars.mobile) {
                    ping.row()
                            .add(ms + "ms")
                            .color(Color.lightGray)
                            .padLeft(5)
                            .padRight(5)
                            .left();
                } else {
                    // Desktop: hiển thị ping cùng dòng
                    ping.add(ms + "ms")
                            .color(Color.lightGray)
                            .padRight(5)
                            .left();
                }

            }, e -> {
                // Callback lỗi: xóa nội dung cũ
                ping.clear();
                // Hiển thị icon X màu đỏ
                ping.image(Icon.cancel)
                        .color(Color.red)
                        .padLeft(5)
                        .padRight(5)
                        .left();
            });
        }
    }

    // Phương thức tạo phòng với mật khẩu
    // Phương thức tạo phòng với mật khẩu
    public void createRoom(String password) {
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

    // Phương thức đóng phòng
    public void closeRoom() {
        // Gọi hàm đóng phòng
        PlayerConnect.closeRoom();
        // Reset link
        link = null;
    }

    // Phương thức copy link vào clipboard
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

    // Lớp nội bộ để lưu thông tin server
    static class Server {
        // Địa chỉ IP của server
        public String ip, name, error, last;
        // Port của server
        public int port;
        // Cờ đánh dấu địa chỉ hợp lệ
        public boolean wasValid;

        // Phương thức parse địa chỉ từ string
        public synchronized boolean set(String ip) {
            // Nếu giống lần trước thì trả về kết quả cũ
            if (ip.equals(last))
                return wasValid;
            // Reset các giá trị
            this.ip = this.error = null;
            this.port = 0;
            // Lưu giá trị để so sánh lần sau
            last = ip;

            // Nếu rỗng thì lỗi
            if (ip.isEmpty()) {
                this.error = "@message.room.missing-host";
                return wasValid = false;
            }
            try {
                // Kiểm tra có phải IPv6 không (có nhiều hơn 1 dấu :)
                boolean isIpv6 = Strings.count(ip, ':') > 1;
                // Nếu là IPv6 và có format [ip]:port
                if (isIpv6 && ip.lastIndexOf("]:") != -1 && ip.lastIndexOf("]:") != ip.length() - 1) {
                    // Tìm vị trí ]:
                    int idx = ip.indexOf("]:");
                    // Parse IP (bỏ dấu [ ])
                    this.ip = ip.substring(1, idx);
                    // Parse port
                    this.port = Integer.parseInt(ip.substring(idx + 2));
                    // Kiểm tra port hợp lệ
                    if (port < 0 || port > 0xFFFF)
                        throw new Exception();
                // Nếu là IPv4 và có port
                } else if (!isIpv6 && ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1) {
                    // Tìm vị trí dấu :
                    int idx = ip.lastIndexOf(':');
                    // Parse IP
                    this.ip = ip.substring(0, idx);
                    // Parse port
                    this.port = Integer.parseInt(ip.substring(idx + 1));
                    // Kiểm tra port hợp lệ
                    if (port < 0 || port > 0xFFFF)
                        throw new Exception();
                } else {
                    // Thiếu port
                    this.error = "@message.room.missing-port";
                    return wasValid = false;
                }
                // Thành công
                return wasValid = true;
            } catch (Exception e) {
                // Port không hợp lệ
                this.error = "@message.room.invalid-port";
                return wasValid = false;
            }
        }

        // Phương thức chuyển về string
        public String get() {
            // Nếu không hợp lệ thì trả về rỗng
            if (!wasValid) {
                return "";
            // Nếu là IPv6 thì thêm dấu []
            } else if (Strings.count(ip, ':') > 1) {
                return "[" + ip + "]:" + port;
            } else {
                // IPv4 format bình thường
                return ip + ":" + port;
            }
        }
    }
}
