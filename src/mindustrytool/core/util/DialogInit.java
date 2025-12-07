package mindustrytool.core.util;

import arc.Events;
import mindustry.game.EventType.ClientLoadEvent;
import mindustrytool.Main;
import mindustrytool.presentation.dialog.*;

public class DialogInit {
    public static void init() {
        Main.schematicDialog = ModDialogs.schematicDialog;
        Main.mapDialog = ModDialogs.mapDialog;
        Main.playerConnectRoomsDialog = new PlayerConnectRoomsDialog();
        Main.createRoomDialog = new CreateRoomDialog();
        Main.joinRoomDialog = new JoinRoomDialog();
        Events.on(ClientLoadEvent.class, e -> ButtonInit.add());
    }
}
