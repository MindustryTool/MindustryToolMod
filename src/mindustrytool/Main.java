package mindustrytool;

import arc.files.Fi;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.service.content.UpdateChecker;
import mindustrytool.ui.dialog.*;
import mindustrytool.core.util.*;

public class Main extends Mod {
    public static BaseDialog schematicDialog, mapDialog;
    public static PlayerConnectRoomsDialog playerConnectRoomsDialog;
    public static CreateRoomDialog createRoomDialog;
    public static JoinRoomDialog joinRoomDialog;
    public static final Fi imageDir = DirInit.imageDir;
    public static final Fi mapsDir = DirInit.mapsDir;
    public static final Fi schematicDir = DirInit.schematicDir;

    public Main() { DirInit.init(); }

    @Override
    public void init() {
        UpdateChecker.check();
        DialogInit.init();
    }
}
