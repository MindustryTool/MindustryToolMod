package old.mindustrytool;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mods.LoadedMod;
import mindustry.net.Packet;
import old.mindustrytool.features.FeatureManager;
import old.mindustrytool.features.autoplay.AutoplayFeature;
import old.mindustrytool.features.background.BackgroundFeature;
import old.mindustrytool.features.browser.map.MapBrowserFeature;
import old.mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import old.mindustrytool.features.chat.global.ChatFeature;
import old.mindustrytool.features.chat.translation.ChatTranslationFeature;
import old.mindustrytool.features.display.healthbar.HealthBarVisualizer;
import old.mindustrytool.features.display.pathfinding.PathfindingDisplay;
import old.mindustrytool.features.display.progress.ProgressDisplay;
import old.mindustrytool.features.display.quickaccess.QuickAccessFeature;
import old.mindustrytool.features.display.range.RangeDisplay;
import old.mindustrytool.features.display.teamresource.TeamResourceFeature;
import old.mindustrytool.features.display.togglerendering.ToggleRenderingFeature;
import old.mindustrytool.features.display.wavepreview.WavePreviewFeature;
import old.mindustrytool.features.godmode.GodModeFeature;
import old.mindustrytool.features.music.MusicFeature;
import old.mindustrytool.features.music.dto.MusicRegisterEvent;
import old.mindustrytool.features.playerconnect.PlayerConnectFeature;
import old.mindustrytool.features.savesync.SaveSyncFeature;
import old.mindustrytool.features.settings.FeatureSettingDialog;
import old.mindustrytool.features.smartdrill.SmartDrillFeature;
import old.mindustrytool.features.smartupgrade.SmartUpgradeFeature;
import old.mindustrytool.features.time.TimeControlFeature;
import old.mindustrytool.services.CrashReportService;
import old.mindustrytool.services.ServerService;
import old.mindustrytool.services.TapListener;
import old.mindustrytool.services.UpdateService;
import mindustry.mod.Mod;

public class Main extends Mod {
    public static LoadedMod self;

    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");
    public static Fi backgroundsDir = Vars.dataDirectory.child("mindustry-tool-backgrounds");
    public static Fi musicsDir = Vars.dataDirectory.child("mindustry-tool-musics");

    private static ObjectMap<Class<?>, Prov<? extends Packet>> packetReplacements = new ObjectMap<>();

    public static FeatureSettingDialog featureSettingDialog;

    public static void registerPacketPlacement(Class<?> clazz, Prov<? extends Packet> prov) {
        packetReplacements.put(clazz, prov);
    }

    public Main() {
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
    }

    @Override
    public void init() {
        self = Vars.mods.getMod(Main.class);

        if (self == null) {
            Core.app.post(() -> {
                Vars.ui.showErrorMessage("Mod cant find itself, please contact admin on Discord to fix the problem\n"
                        + Config.DISCORD_INVITE_URL);
            });
            return;
        }

        Events.on(ClientLoadEvent.class, e -> {
            try {
                featureSettingDialog = new FeatureSettingDialog();

                addCustomButtons();

                UpdateService.getInstance().checkForUpdate(() -> {
                    Core.app.post(() -> {
                        try {
                            setup();
                        } catch (Exception err) {
                            Core.app.post(() -> {
                                Vars.ui.showException(err);
                            });
                        }
                    });
                });
            } catch (Exception err) {
                Core.app.post(() -> {
                    Vars.ui.showException(err);
                });
            }
        });
    }

    private void setup() {
        imageDir.mkdirs();
        mapsDir.mkdirs();
        backgroundsDir.mkdirs();
        musicsDir.mkdirs();
        schematicDir.mkdirs();

        checkDirVersion(imageDir, 1);
        checkDirVersion(mapsDir, 1);
        checkDirVersion(schematicDir, 1);

        mindustrytool.services.MindustryAuthProvider.getInstance().init();
        mindustrytool.ui.AuthOverlay.getInstance().init();
        ServerService.getInstance().init();
        TapListener.getInstance().init();

        FeatureManager.getInstance().register(//
                new MapBrowserFeature(), //
                new SchematicBrowserFeature(), //
                new PlayerConnectFeature(), //
                new HealthBarVisualizer(), //
                new TeamResourceFeature(),
                new PathfindingDisplay(), //
                new RangeDisplay(), //
                new QuickAccessFeature(), //
                new ChatFeature(),
                new ChatTranslationFeature(),
                // new PrettyChatFeature(),
                new AutoplayFeature(),
                new WavePreviewFeature(),
                new SaveSyncFeature(),
                // new ItemVisualizerFeature(),
                new GodModeFeature(),
                new SmartDrillFeature(),
                new SmartUpgradeFeature(),
                new BackgroundFeature(),
                new MusicFeature(),
                new ProgressDisplay(),
                new ToggleRenderingFeature(),
                new TimeControlFeature());

        boolean hasCrashed = new CrashReportService().checkForCrashes();
        if (hasCrashed) {
            // Try to disable all feature
            FeatureManager.getInstance().disableAll();
        }
        initFeatures();

        Events.fire(new MusicRegisterEvent());
        Events.fire(new MdtInitEvent());
    }

    private void initFeatures() {
        FeatureManager.getInstance().init();

        Seq<Prov<? extends Packet>> packetProvs = Reflect.get(Vars.net, "packetProvs");

        packetProvs.replace(packet -> {
            Class<?> clazz = packet.get().getClass();
            if (packetReplacements.containsKey(clazz)) {
                Log.info("Replace packet @ to @", clazz.getSimpleName(),
                        packetReplacements.get(clazz).get().getClass().getSimpleName());
                return packetReplacements.remove(clazz);
            }

            return packet;
        });

        for (Class<?> clazz : packetReplacements.keys()) {
            Log.info("Packet @ not found", clazz.getSimpleName());
        }
    }

    private void addCustomButtons() {
        Core.app.post(() -> {
            try {
                Vars.ui.menufrag.addButton("Mindustry Tool", Utils.icons("mod.png"), () -> featureSettingDialog.show());
            } catch (Exception err) {
                Vars.ui.showException(err);
            }
        });
    }

    private int readDirVersion(Fi dir) {
        try {
            Fi versionFile = dir.child("version.txt");
            if (versionFile.exists()) {
                return Integer.parseInt(versionFile.readString());
            } else {
                return -1;
            }
        } catch (Exception err) {
            return 0;
        }
    }

    private void writeDirVersion(Fi dir, int version) {
        try {
            dir.emptyDirectory(false);
            Fi versionFile = dir.child("version.txt");
            versionFile.writeString(version + "");
        } catch (Exception err) {
            Core.app.post(() -> {
                Vars.ui.showException(err);
            });
        }
    }

    private void checkDirVersion(Fi dir, int expectedVersion) {
        try {
            int version = readDirVersion(dir);
            if (version == -1) {
                dir.mkdirs();
            }

            if (version != expectedVersion) {
                writeDirVersion(dir, expectedVersion);
            }
        } catch (Exception err) {
            Log.err("Check dir version failed", err);
        }
    }
}
