package mindustrytool.plugins.playerconnect;

import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.Vars;

/** Injects Player Connect section into the Join Game dialog. */
public class JoinDialogInjector {
    private static Table roomsContainer;
    private static final State<Seq<PlayerConnectRoom>> rooms = new State<>(new Seq<>());
    private static final State<Boolean> isLoading = new State<>(false);
    private static final PlayerConnectUI.Filter filter = new PlayerConnectUI.Filter();
    private static boolean needsRefresh;

    public static void inject() {
        Runnable hook = () -> { needsRefresh = true; Core.app.post(JoinDialogInjector::injectSection); };
        Vars.ui.join.shown(hook); Vars.ui.join.resized(hook);
        Runnable render = () -> RoomCard.renderList(roomsContainer, rooms.get(), isLoading.get(), filter.text);
        rooms.bind(render); isLoading.bind(render);
    }

    private static void injectSection() {
        ScrollPane pane = Vars.ui.join.cont.find(e -> e instanceof ScrollPane);
        if (pane == null) return;
        Table hosts = (Table)pane.getWidget();
        if (hosts == null || hosts.getChildren().isEmpty()) return;
        if (hosts.getChildren().first() instanceof Table first && "playerconnect".equals(first.name)) { if (needsRefresh) { needsRefresh = false; refreshRooms(); } return; }
        Table pc = new Table(); pc.name = "playerconnect"; build(pc);
        hosts.add(pc).growX(); hosts.row();
        if (hosts.getCells().size > 1) { hosts.getCells().insert(0, hosts.getCells().pop()); hosts.getChildren().insert(0, hosts.getChildren().pop()); }
        hosts.invalidateHierarchy(); needsRefresh = false; refreshRooms();
    }

    private static void build(Table parent) {
        Collapser coll = new Collapser(new Table(), Core.settings.getBool("collapsed-playerconnect", false)); coll.setDuration(0.1f);
        PlayerConnectUI.buildHeader(parent, coll, () -> { filter.showHidden = !filter.showHidden; refreshRooms(); });
        Table section = new Table(); PlayerConnectUI.buildSearch(section, filter, JoinDialogInjector::refreshRooms);
        roomsContainer = section.table().growX().left().top().get(); coll.setTable(section);
        parent.add(coll).width((targetWidth() + 5f) * columns()).row();
    }

    private static void refreshRooms() { isLoading.set(true); Api.findPlayerConnectRooms(filter.text, f -> { rooms.set(f == null ? new Seq<>() : f); isLoading.set(false); }); }
    public static float targetWidth() { return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f); }
    public static int columns() { return Mathf.clamp((int)((Core.graphics.getWidth() / Scl.scl() * 0.9f) / targetWidth()), 1, 4); }
}
