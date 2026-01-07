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
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.browser.map.MapDialog;
import mindustrytool.features.browser.schematic.SchematicDialog;
import mindustrytool.features.playerconnect.CreateRoomDialog;
import mindustrytool.features.playerconnect.JoinRoomDialog;
import mindustrytool.features.playerconnect.PlayerConnectRoomsDialog;
import mindustrytool.features.browser.map.MapBrowserFeature;
import mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.settings.FeatureSettingDialog;

public class Main extends Mod {
    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

    private MapBrowserFeature mapBrowserFeature;
    private SchematicBrowserFeature schematicBrowserFeature;
    private PlayerConnectFeature playerConnectFeature;
    private FeatureSettingDialog featureSettingDialog;

    public Main() {
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
    }

    @Override
    public void init() {
        checkForUpdate();

        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();

        initFeatures();
        addCustomButtons();
    }

    private void initFeatures() {
        mapBrowserFeature = new MapBrowserFeature();
        schematicBrowserFeature = new SchematicBrowserFeature();
        playerConnectFeature = new PlayerConnectFeature();

        FeatureManager.getInstance().register(mapBrowserFeature);
        FeatureManager.getInstance().register(schematicBrowserFeature);
        FeatureManager.getInstance().register(playerConnectFeature);

        FeatureManager.getInstance().init();
    }

    private void addCustomButtons() {
        featureSettingDialog = new FeatureSettingDialog();

        Events.on(ClientLoadEvent.class, (event) -> {
            // Schematic button is handled by SchematicBrowserFeature

            if (Vars.mobile) {
                // Mobile buttons handled by Features directly
                // We just need the Settings button
                Vars.ui.menufrag.addButton("Settings", Icon.settings, () -> featureSettingDialog.show());
            } else {
                // Desktop Tools Menu
                Vars.ui.menufrag.addButton("Mindustry Tool", Icon.wrench, () -> {
                    BaseDialog toolsMenu = new BaseDialog("Mindustry Tools");
                    toolsMenu.addCloseButton();
                    toolsMenu.cont.pane(t -> {
                        t.defaults().size(220, 60).pad(5);

                        if (FeatureManager.getInstance().isEnabled(mapBrowserFeature)) {
                            t.button(Core.bundle.format("message.map-browser.title"), Icon.map, () -> {
                                toolsMenu.hide();
                                mapBrowserFeature.getDialog().show();
                            }).row();
                        }

                        if (FeatureManager.getInstance().isEnabled(playerConnectFeature)) {
                            t.button(Core.bundle.format("message.player-connect.title"), Icon.host, () -> {
                                toolsMenu.hide();
                                playerConnectFeature.getDialog().show();
                            }).row();
                        }

                        t.button("Settings", Icon.settings, () -> {
                            toolsMenu.hide(); // Optional
                            featureSettingDialog.show();
                        }).row();
                    }).grow();
                    toolsMenu.show();
                });
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

        Http.get(Config.API_URL + "ping?client=mod-v8").submit(result -> {
            Log.info("Ping");
        });
    }
}
