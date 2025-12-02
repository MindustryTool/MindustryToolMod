package mindustrytool.gui;

import arc.Core;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.config.Debouncer;
import mindustrytool.data.PlayerConnectRoom;
import mindustrytool.net.Api;
import mindustrytool.playerconnect.PlayerConnect;
import mindustrytool.playerconnect.PlayerConnectLink;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerConnectRoomsDialog extends BaseDialog {

    // Lỗi Thread Safety: Cần Core.app.post(listener) để đảm bảo renderUI chạy trên UI thread.
    private static class State<T> {
        private T value;
        private Runnable listener;
        public State(T initial) { value = initial; }
        public T get() { return value; }
        public void set(T newValue) {
            value = newValue;
            // FIX: Đảm bảo lắng nghe chạy trên luồng App (UI thread)
            if (listener != null) Core.app.post(listener); 
        }
        public void bind(Runnable r) { listener = r; }
    }

    private final Table roomList = new Table();
    private final Debouncer debouncer = new Debouncer(250, TimeUnit.MILLISECONDS);

    // STATE
    private final State<String> search = new State<>("");
    private final State<Seq<PlayerConnectRoom>> rooms = new State<>(new Seq<>());
    private final State<Boolean> isLoading = new State<>(false); // Thêm trạng thái Loading

    public PlayerConnectRoomsDialog() {
        super("@message.room-list.title");
        addCloseButton();

        // 1. SETUP LAYOUT
        cont.table(root -> {
            // Search Bar
            root.table(bar -> {
                var field = bar.field("", t -> {
                    search.set(t);
                    debouncer.debounce(this::fetchRooms);
                }).growX().get();
                field.setMessageText(Core.bundle.get("map.search"));
            }).growX().left().top().padBottom(10f);
            root.row();

            // Room List Container
            root.add(roomList).grow().left().top();
        }).grow().left().top();

        // 2. ACTIONS & EFFECTS
        buttons.button("" + Iconc.refresh, this::fetchRooms).size(64).padRight(8);

        // Binding: Thay đổi trạng thái -> Render lại UI
        rooms.bind(this::renderUI);
        isLoading.bind(this::renderUI); // Bind trạng thái loading

        shown(this::fetchRooms);
    }

    // =================== ACTIONS & EFFECTS ===================

    private void fetchRooms() {
        isLoading.set(true); // Bắt đầu tải

        Api.findPlayerConnectRooms(search.get(), found -> {
            rooms.set(found == null ? new Seq<>() : found);
            isLoading.set(false); // Kết thúc tải
        });
    }
    
    // Tách logic Join chung
    private void tryJoin(PlayerConnectRoom room, String password, BaseDialog dialogToHide) {
        try {
            PlayerConnect.joinRoom(
                PlayerConnectLink.fromString(room.link()), 
                password, 
                () -> { 
                    hide(); 
                    if (dialogToHide != null) dialogToHide.hide(); 
                }
            );
        } catch (Throwable e) {
            hide();
            if (dialogToHide != null) dialogToHide.hide();
            fetchRooms(); // Tải lại danh sách nếu thất bại
            Vars.ui.showException("@message.connect.fail", e);
        }
    }

    private void join(PlayerConnectRoom room) {
        if (!room.data().isSecured()) {
            tryJoin(room, "", null);
            return;
        }

        // Dialog Mật khẩu
        BaseDialog dialog = new BaseDialog("@message.type-password.title");
        String[] pass = {""};

        dialog.cont.table(tt -> {
            tt.add("@message.password").padRight(5);
            
            // Sử dụng setPasswordMode thay vì maxTextLength/valid cho mật khẩu
            TextField passField = tt.field(pass[0], t -> pass[0] = t)
                .size(320, 54).left().get();
            passField.setPasswordMode(true);
            passField.setText("");
        });

        dialog.buttons.button("@cancel", dialog::hide);
        dialog.buttons.button("@ok", () -> {
            // Dùng hàm tryJoin chung
            tryJoin(room, pass[0], dialog);
        }).disabled(b -> pass[0].isEmpty()); // Rút gọn: Dùng disabled

        dialog.show();
    }

    // =================== RENDER UI ===================

    private void renderUI() {
        roomList.clear();

        if (isLoading.get()) {
            roomList.add(Core.bundle.get("message.loading")).center().pad(20f);
            roomList.invalidateHierarchy();
            return;
        }
        
        Seq<PlayerConnectRoom> rs = rooms.get();

        if (rs.isEmpty()) {
            roomList.add(Core.bundle.get("message.no-rooms-found")).center().pad(20f);
            roomList.invalidateHierarchy();
            return;
        }

        roomList.pane(list -> {
            for (PlayerConnectRoom room : rs) {
                list.add(renderRoom(room)).growX().left().top().pad(6);
                list.row();
            }
        }).grow().scrollY(true).scrollX(false);
        
        roomList.invalidateHierarchy();
    }

    // =================== ROOM CARD ===================

    private Table renderRoom(PlayerConnectRoom room) {
        Table t = new Table(Styles.black5);
        t.margin(8);

        // LEFT (Info)
        t.table(left -> {
            // Rút gọn hiển thị Tiêu đề và Phiên bản
            String title = Strings.format("@: @", 
                room.data().name() + " (" + room.data().locale() + ")", 
                getVersionString(room.data().version())) 
                + " [white]" + (room.data().isSecured() ? Iconc.lock : "");

            left.add(title).fontScale(1.35f).left().wrap().growX();
            left.row();

            left.add(Strings.format("@ @[lightgray] / @",
                Iconc.map, room.data().mapName(), room.data().gamemode())).left();
            left.row();

            left.add(Strings.format("@ @",
                Iconc.players, Core.bundle.format("players", room.data().players().size))).left();
            left.row();

            if (room.data().mods().size > 0) {
                left.add(Iconc.book + " " + Strings.join(", ", room.data().mods())).left().wrap().growX();
            }

        }).left().top().growX();

        t.add().growX();

        // RIGHT (Button)
        t.table(right -> {
            right.button(Iconc.play + " " + Core.bundle.get("join"), () -> join(room))
                .size(160f, 60f); // Sử dụng size cố định cho kích thước nút đồng nhất
        }).right().top();

        return t;
    }

    // =================== VERSION PARSER (Giữ nguyên) ===================

    private static class BuildInfo {
        public String type = "custom";
        public int build = -1;
        @SuppressWarnings("unused")
        public int revision = 0;
        @SuppressWarnings("unused")
        public String modifier;
    }

    private BuildInfo extract(String combined) {
        BuildInfo info = new BuildInfo();
        if ("custom build".equals(combined)) return info;
        Matcher m = Pattern.compile("^(.+?) build (\\d+)(?:\\.(\\d+))?$").matcher(combined);
        if (!m.matches()) return info;
        info.type = m.group(1);
        info.build = Integer.parseInt(m.group(2));
        info.revision = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        info.modifier = info.type.equals("official") ? info.type : null;
        return info;
    }

    private String getVersionString(String s) {
        BuildInfo i = extract(s);
        if (i.build == -1) return Core.bundle.format("server.custombuild");
        if (i.build == 0) return Core.bundle.get("server.outdated");

        // Sử dụng biến cục bộ để dễ đọc
        int clientBuild = Version.build;
        String clientType = Version.type;

        if (clientBuild != -1) {
            if (i.build < clientBuild)
                return Core.bundle.get("server.outdated") + "\n" + Core.bundle.format("server.version", i.build, "");
            if (i.build > clientBuild)
                return Core.bundle.get("server.outdated.client") + "\n" + Core.bundle.format("server.version", i.build, "");
        }

        if (i.build == clientBuild && clientType.equals(i.type))
            return "";

        return Core.bundle.format("server.version", i.build, i.type);
    }
}