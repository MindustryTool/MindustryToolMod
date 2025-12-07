package mindustrytool.presentation.dialog;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Iconc;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.util.Debouncer;
import mindustrytool.core.model.PlayerConnectRoom;
import mindustrytool.data.api.Api;
import mindustrytool.presentation.builder.*;
import java.util.concurrent.TimeUnit;

public class PlayerConnectRoomsDialog extends BaseDialog {
    private final Table roomList = new Table();
    private final Debouncer debouncer = new Debouncer(250, TimeUnit.MILLISECONDS);
    private final State<String> search = new State<>("");
    private final State<Seq<PlayerConnectRoom>> rooms = new State<>(new Seq<>());
    private final State<Boolean> isLoading = new State<>(false);

    public PlayerConnectRoomsDialog() {
        super("@message.room-list.title");
        addCloseButton();
        cont.table(root -> {
            root.table(bar -> bar.field("", t -> { search.set(t); debouncer.debounce(this::fetchRooms); })
                .growX().get().setMessageText(Core.bundle.get("map.search"))).growX().left().top().padBottom(10f);
            root.row();
            root.add(roomList).grow().left().top();
        }).grow().left().top();
        buttons.button("" + Iconc.refresh, this::fetchRooms).size(64).padRight(8);
        rooms.bind(this::renderUI);
        isLoading.bind(this::renderUI);
        shown(this::fetchRooms);
    }

    private void fetchRooms() {
        isLoading.set(true);
        Api.findPlayerConnectRooms(search.get(), found -> { rooms.set(found == null ? new Seq<>() : found); isLoading.set(false); });
    }

    private void renderUI() {
        roomList.clear();
        if (isLoading.get()) { roomList.add(Core.bundle.get("message.loading")).center().pad(20f); roomList.invalidateHierarchy(); return; }
        Seq<PlayerConnectRoom> rs = rooms.get();
        if (rs.isEmpty()) { roomList.add(Core.bundle.get("message.no-rooms-found")).center().pad(20f); roomList.invalidateHierarchy(); return; }
        roomList.pane(list -> rs.forEach(room -> list.add(RoomCard.render(room, () -> join(room))).growX().left().top().pad(6).row())).grow().scrollY(true).scrollX(false);
        roomList.invalidateHierarchy();
    }

    private void join(PlayerConnectRoom room) { PasswordDialog.show(room, this::hide); }
}