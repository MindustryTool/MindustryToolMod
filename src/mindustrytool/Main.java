package mindustrytool;

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
import mindustry.mod.Mod;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.browser.map.MapBrowserFeature;
import mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.display.healthbar.HealthBarVisualizer;
import mindustrytool.features.display.pathfinding.PathfindingDisplay;
import mindustrytool.features.display.teamresource.TeamResourceFeature;
import mindustrytool.features.display.range.RangeDisplay;
import mindustrytool.features.display.progress.ProgressDisplay;
import mindustrytool.features.display.quickaccess.QuickAccessHud;
import mindustrytool.features.auth.AuthFeature;
import mindustrytool.features.settings.FeatureSettingDialog;
import mindustrytool.features.smartupgrade.SmartUpgradeFeature;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.services.ServerService;
import mindustrytool.services.CrashReportService;
import mindustrytool.services.UpdateService;
import mindustrytool.features.chat.global.ChatFeature;
import mindustrytool.features.godmode.GodModeFeature;
import mindustrytool.features.godmode.TapListener;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.background.BackgroundFeature;
import mindustrytool.features.music.MusicFeature;
import mindustrytool.features.music.dto.MusicRegisterEvent;
import mindustrytool.features.display.wavepreview.WavePreviewFeature;
import mindustrytool.features.chat.translation.ChatTranslationFeature;
import mindustrytool.features.chat.pretty.PrettyChatFeature;
import mindustrytool.features.savesync.SaveSyncFeature;

public class Main extends Mod {
    public static LoadedMod self;

    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi backgroundsDir = Vars.dataDirectory.child("mindustry-tool-backgrounds");
    public static Fi musicsDir = Vars.dataDirectory.child("mindustry-tool-musics");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

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
        try {
            self = Vars.mods.getMod(Main.class);

            imageDir.mkdirs();
            mapsDir.mkdirs();
            backgroundsDir.mkdirs();
            musicsDir.mkdirs();
            schematicDir.mkdirs();

            TapListener.init();
            ServerService.init();

            Events.on(ClientLoadEvent.class, e -> {
                try {
                    featureSettingDialog = new FeatureSettingDialog();

                    FeatureManager.getInstance().register(//
                            new MapBrowserFeature(), //
                            new SchematicBrowserFeature(), //
                            new PlayerConnectFeature(), //
                            new HealthBarVisualizer(), //
                            new TeamResourceFeature(),
                            new PathfindingDisplay(), //
                            new RangeDisplay(), //
                            new QuickAccessHud(), //
                            new AuthFeature(), //
                            new ChatFeature(),
                            new ChatTranslationFeature(),
                            new PrettyChatFeature(),
                            new AutoplayFeature(),
                            new WavePreviewFeature(),
                            new SaveSyncFeature(),
                            // new ItemVisualizerFeature(),
                            new GodModeFeature(),
                            new SmartDrillFeature(),
                            new SmartUpgradeFeature(),
                            new BackgroundFeature(),
                            new MusicFeature(),
                            new ProgressDisplay());

                    boolean hasCrashed = new CrashReportService().checkForCrashes();
                    if (hasCrashed) {
                        // Try to disable all feature
                        FeatureManager.getInstance().disableAll();
                    }
                    initFeatures();

                    Events.fire(new MusicRegisterEvent());

                    new UpdateService().checkForUpdate(self.meta.version);
                    addCustomButtons();

                } catch (Exception err) {
                    Log.err(err);
                }
            });
        } catch (Exception e) {
            Log.err(e);
        }
    }

    private void initFeatures() {
        FeatureManager.getInstance().init();

        Seq<Prov<? extends Packet>> packetProvs = Reflect.get(Vars.net, "packetProvs");

        packetProvs.replace(packet -> {
            Class<?> clazz = packet.get().getClass();
            if (packetReplacements.containsKey(clazz)) {
                Log.debug("Replace packet @ to @", clazz.getSimpleName(),
                        packetReplacements.get(clazz).get().getClass().getSimpleName());
                return packetReplacements.get(clazz);
            }

            return packet;
        });
    }

    private void addCustomButtons() {
        try {
            Vars.ui.menufrag.addButton("Mindustry Tool", Utils.icons("mod.png"), () -> featureSettingDialog.show());
        } catch (Exception e) {
            Log.err(e);
        }
    }
}
