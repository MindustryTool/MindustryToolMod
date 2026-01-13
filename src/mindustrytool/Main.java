package mindustrytool;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Mod;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.browser.map.MapBrowserFeature;
import mindustrytool.features.browser.schematic.SchematicBrowserFeature;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.display.healthbar.HealthBarVisualizer;
import mindustrytool.features.display.pathfinding.PathfindingDisplay;
import mindustrytool.features.display.teamresource.TeamResourceFeature;
import mindustrytool.features.display.range.RangeDisplay;
import mindustrytool.features.display.quickaccess.QuickAccessHud;
import mindustrytool.features.auth.AuthFeature;
import mindustrytool.features.settings.FeatureSettingDialog;
import mindustrytool.features.chat.ChatFeature;
import mindustrytool.features.godmode.GodModeFeature;

public class Main extends Mod {
    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");

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
                new GodModeFeature());

        FeatureManager.getInstance().init();
    }

    private void addCustomButtons() {
        featureSettingDialog = new FeatureSettingDialog();

        Events.on(ClientLoadEvent.class, (event) -> {
            try {
                var mod = Vars.mods.getMod(this.getClass());

                for (var child : mod.root.child("icons").list()) {
                    Log.info(child.absolutePath());
                }

                var texture = new TextureRegion(new Texture(mod.root.child("icons").child("mindustry-tool.png")));
                TextureRegionDrawable drawable = new TextureRegionDrawable(texture);

                Vars.ui.menufrag.addButton("Mindustry Tool", drawable, () -> featureSettingDialog.show());
            } catch (Exception e) {
                Log.err(e);
                Vars.ui.menufrag.addButton("Mindustry Tool", Icon.settings, () -> featureSettingDialog.show());
            }
        });

        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;
            @SuppressWarnings("rawtypes")
            arc.struct.Seq<arc.scene.ui.layout.Cell> buttons = root.getCells();

            var span = !Vars.mobile && arc.util.Reflect.<Integer>get(buttons.get(buttons.size - 2), "colspan") == 2;

            var btn = root.row()
                    .button("MindustryTool", Icon.settings, featureSettingDialog::show);

            if (span) {
                btn.colspan(2).width(450f);
            } else {
                btn.row();
            }

            buttons.swap(buttons.size - 1, buttons.size - 2);
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
