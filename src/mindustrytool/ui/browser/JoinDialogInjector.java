package mindustrytool.ui.browser;

import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.Vars;
import mindustrytool.core.model.PlayerConnectRoom;
import mindustrytool.data.api.Api;
import mindustrytool.ui.common.State;
import mindustrytool.ui.room.*;

/** Injects Player Connect section at TOP of Mindustry's JoinDialog. */
public class JoinDialogInjector {
    private static Table roomsContainer;
    private static final State<Seq<PlayerConnectRoom>> rooms = new State<>(new Seq<>());
    private static final State<Boolean> isLoading = new State<>(false);
    private static final RoomFilter filter = new RoomFilter();
    private static boolean needsRefresh = false;

    public static void inject() {
        Vars.ui.join.shown(() -> { needsRefresh = true; Core.app.post(() -> injectSection()); });
        Vars.ui.join.resized(() -> { needsRefresh = true; Core.app.post(() -> injectSection()); });
        rooms.bind(() -> RoomRenderer.render(roomsContainer, rooms.get(), isLoading.get(), filter.text));
        isLoading.bind(() -> RoomRenderer.render(roomsContainer, rooms.get(), isLoading.get(), filter.text));
    }

    private static void injectSection() {
        ScrollPane pane = Vars.ui.join.cont.find(e -> e instanceof ScrollPane);
        if (pane == null) return;
        Table hosts = (Table) pane.getWidget();
        if (hosts == null || hosts.getChildren().isEmpty()) return;

        // Check if PC section already exists at top
        if (hosts.getChildren().first() instanceof Table) {
            Table first = (Table) hosts.getChildren().first();
            if ("playerconnect".equals(first.name)) {
                if (needsRefresh) { needsRefresh = false; refreshRooms(); }
                return;
            }
        }

        // Build PC section as a wrapper table
        Table pcWrapper = new Table();
        pcWrapper.name = "playerconnect";
        buildPCSection(pcWrapper);

        // Use add() to properly add with Cell, then reorder
        hosts.add(pcWrapper).growX();
        hosts.row();
        
        // Move to first position by swapping cells
        if (hosts.getCells().size > 1) {
            hosts.getCells().insert(0, hosts.getCells().pop());
            hosts.getChildren().insert(0, hosts.getChildren().pop());
        }
        hosts.invalidateHierarchy();
        needsRefresh = false;
        refreshRooms();
    }

    private static void buildPCSection(Table parent) {
        Collapser coll = new Collapser(new Table(), Core.settings.getBool("collapsed-playerconnect", false));
        coll.setDuration(0.1f);
        PlayerConnectHeader.build(parent, coll, () -> { filter.showHidden = !filter.showHidden; refreshRooms(); });

        Table section = new Table();
        PlayerConnectSearch.build(section, filter, JoinDialogInjector::refreshRooms);
        roomsContainer = section.table().growX().left().top().get();
        coll.setTable(section);
        parent.add(coll).width((targetWidth() + 5f) * columns()).row();
    }

    private static void refreshRooms() {
        isLoading.set(true);
        Api.findPlayerConnectRooms(filter.text, found -> {
            rooms.set(found == null ? new Seq<>() : found);
            isLoading.set(false);
        });
    }

    public static float targetWidth() { return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f); }
    public static int columns() { return Mathf.clamp((int)((Core.graphics.getWidth() / Scl.scl() * 0.9f) / targetWidth()), 1, 4); }
}
