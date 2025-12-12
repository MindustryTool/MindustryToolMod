package mindustrytool.ui.room;

import arc.util.*;
import mindustry.Vars;
import mindustrytool.core.model.ServerHost;
import mindustrytool.network.*;

public class RoomCreator {
    public static void create(ServerHost selected, String password, arc.func.Cons<PlayerConnectLink> onSuccess, arc.func.Cons<PlayerConnectLink> setter) {
        if (selected == null) return;
        Vars.ui.loadfrag.show("@message.manage-room.create-room");
        final PlayerConnectLink[] link = {null};
        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        PlayerConnect.createRoom(selected.ip, selected.port, password, 
            l -> { Vars.ui.loadfrag.hide(); t.cancel(); link[0] = l; onSuccess.get(l); },
            e -> { Vars.net.handleException(e); t.cancel(); },
            r -> { 
                Vars.ui.loadfrag.hide(); t.cancel(); 
                if (r != null) Vars.ui.showText("", "@message.room." + Strings.camelToKebab(r.name()));
                else if (link[0] == null) Vars.ui.showErrorMessage("@message.manage-room.create-room.failed");
                setter.get(null);
            });
    }
}
