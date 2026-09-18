package mindustrytool;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.components.FileIcon;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.background.BackgroundFeature;
import mindustrytool.features.camerazoom.CameraZoomFeature;
import mindustrytool.features.chat.ChatFeature;
import mindustrytool.features.emoji.EmojiFeature;
import mindustrytool.features.freecamera.FreeCameraFeature;
import mindustrytool.features.godmode.GodModeFeature;
import mindustrytool.features.healthbar.HealthBarFeature;
import mindustrytool.features.joystick.JoystickFeature;
import mindustrytool.features.music.MusicFeature;
import mindustrytool.features.pathfinding.PathfindingFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.prettychat.PrettyChatFeature;
import mindustrytool.features.progressdisplay.ProgressDisplayFeature;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import mindustrytool.features.savesync.SaveSyncFeature;
import mindustrytool.features.screenshot.ScreenshotFeature;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.features.smartupgrade.SmartUpgradeFeature;
import mindustrytool.features.teamresource.TeamResourceFeature;
import mindustrytool.features.timecontrol.TimeControlFeature;
import mindustrytool.features.togglerendering.ToggleRenderingFeature;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.wavepreview.WavePreviewFeature;
import mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import mindustrytool.features.browser.map.MapBrowserFeature;
import mindustrytool.features.bridgevisualizer.BridgeVisualizerFeature;
import mindustrytool.features.settings.FeatureSettingDialog;
import mindustrytool.input.ModInputManager;
import mindustrytool.services.Github;
import mindustrytool.services.PacketReplacer;
import mindustrytool.services.ServerService;
import mindustrytool.services.auth.AuthOverlay;
import mindustrytool.services.auth.MindustryAuthProvider;
import mindustrytool.services.crash.CrashReportService;
import mindustrytool.services.update.UpdateService;

public class Main extends Mod {
    public static LoadedMod self;
    private FeatureSettingDialog featureSettingDialog;

    public Main() {
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
    }

    @Override
    public void init() {
        // SolimMcpServer.start(McpConfig.fromProperties());

        self = Vars.mods.getMod(Main.class);

        if (self == null) {
            Vars.ui.showErrorMessage(Core.bundle.get("error.mod-not-found",
                    "Mod cannot find itself, please contact admin on Discord to fix the problem."));
            return;
        }

        ModInputManager.init();

        FeatureManager.register(
                new BackgroundFeature(),
                new QuickAccessFeature(),
                new ChatFeature(),
                new TeamResourceFeature(),
                new TranslationFeature(),
                new SchematicBrowserFeature(),
                new MapBrowserFeature(),
                new BridgeVisualizerFeature(),
                new PlayerConnectFeature(),
                new HealthBarFeature(),
                new CameraZoomFeature(),
                new FreeCameraFeature(),
                new PathfindingFeature(),
                new RangeDisplayFeature(),
                new PrettyChatFeature(),
                new AutoplayFeature(),
                new WavePreviewFeature(),
                new SaveSyncFeature(),
                new GodModeFeature(),
                new SmartDrillFeature(),
                new SmartUpgradeFeature(),
                new MusicFeature(),
                new ProgressDisplayFeature(),
                new ToggleRenderingFeature(),
                new TimeControlFeature(),
                new JoystickFeature(),
                new ScreenshotFeature(),
                new EmojiFeature());

        Events.on(ClientLoadEvent.class, event -> {
            registerMindustryToolButton();

            Github.prefetchAll();
            UpdateService.getInstance().checkForUpdate(() -> {
                Core.app.post(() -> {
                    boolean hasCrashed = new CrashReportService().checkForCrashes();
                    if (hasCrashed) {
                        // Try to disable all feature — mirrors old Main.setup() crash handling
                        FeatureManager.disableAll();
                    }
                    FeatureManager.init();
                    AuthOverlay.getInstance().init();
                    MindustryAuthProvider.getInstance().init();
                    ServerService.getInstance().init();
                    PacketReplacer.replace();
                });
            });
        });
    }

    private void registerMindustryToolButton() {
        Core.app.post(() -> {
            try {
                Vars.ui.menufrag.addButton("Mindustry Tool", FileIcon.of("mindustrytool.png"), () -> {
                    if (featureSettingDialog == null) {
                        featureSettingDialog = new FeatureSettingDialog();
                    }
                    featureSettingDialog.show();
                });
            } catch (Exception err) {
                Vars.ui.showException(err);
            }
        });
    }
}
