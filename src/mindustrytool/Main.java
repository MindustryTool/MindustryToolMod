package mindustrytool;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.mod.*;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.config.Config;
import mindustrytool.gui.CreateRoomDialog;
import mindustrytool.gui.JoinRoomDialog;
import mindustrytool.gui.MapDialog;
import mindustrytool.gui.PlayerConnectRoomsDialog;
import mindustrytool.gui.SchematicDialog;

public class Main extends Mod {
    public static SchematicDialog schematicDialog;
    public static MapDialog mapDialog;
    public static PlayerConnectRoomsDialog playerConnectRoomsDialog;
    public static CreateRoomDialog createRoomDialog;
    public static JoinRoomDialog joinRoomDialog;

    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

    public Main() {
        Vars.maxSchematicSize = 10000;
        MapResizeDialog.maxSize = 100000;
    }

    @Override
    public void init() {
        checkForUpdate();

        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();

        addCustomButtons();
    }

    private void addCustomButtons() {
        schematicDialog = new SchematicDialog();
        mapDialog = new MapDialog();
        playerConnectRoomsDialog = new PlayerConnectRoomsDialog();
        createRoomDialog = new CreateRoomDialog();
        joinRoomDialog = new JoinRoomDialog();

        Events.on(ClientLoadEvent.class, (event) -> {
            Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
                Vars.ui.schematics.hide();
                schematicDialog.show();
            });

            if (Vars.mobile) {
                Vars.ui.menufrag.addButton(Core.bundle.format("message.map-browser.title"), Icon.map, () -> {
                    mapDialog.show();
                });
                Vars.ui.menufrag.addButton(Core.bundle.format("message.player-connect.title"), Icon.menu, () -> {
                    playerConnectRoomsDialog.show();
                });
            } else {
                Vars.ui.menufrag.addButton(new MenuButton("Tools", Icon.wrench, () -> {
                }, //
                        new MenuButton(Core.bundle.format("message.map-browser.title"), Icon.map, () -> {
                            mapDialog.show();
                        }), //
                        new MenuButton(Core.bundle.format("message.player-connect.title"), Icon.menu, () -> {
                            playerConnectRoomsDialog.show();
                        })//
                ));
            }
        });
    }

    private void checkForUpdate() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        String currentVersion = mod.meta.version;

        Http.get(Config.API_REPO_URL, (res) -> {
            Jval json = Jval.read(res.getResultAsString());

            String latestVersion = json.getString("version");

            if (!latestVersion.equals(currentVersion)) {
                Log.info("Mod require update, current version: @, latest version: @", currentVersion, latestVersion);

                Vars.ui.showConfirm(Core.bundle.format("message.new-version", currentVersion, latestVersion)
                        + "\nDiscord: https://discord.gg/72324gpuCd",
                        () -> {
                            if (currentVersion.endsWith("v8")) {
                                return;
                            }

                            Core.app.post(() -> {
                                Vars.ui.mods.githubImportMod(Config.REPO_URL, true, null);
                            });
                        });
            } else {
                Log.info("Mod up to date");
            }
        });

        Http.get(Config.API_URL + "ping?client=mod").submit(result -> {
            Log.info("Ping");
        });

    }
}
