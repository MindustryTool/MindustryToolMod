package mindustrytool;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
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
import arc.scene.ui.ScrollPane;
import arc.graphics.Color;
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
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.display.wavepreview.WavePreviewFeature;

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
                new GodModeFeature(),
                new AutoplayFeature(),
                new WavePreviewFeature());

        FeatureManager.getInstance().init();
    }

    private void addCustomButtons() {
        featureSettingDialog = new FeatureSettingDialog();

        Events.on(ClientLoadEvent.class, (event) -> {
            try {
                Vars.ui.menufrag.addButton("Mindustry Tool", Utils.icons("mod.png"), () -> featureSettingDialog.show());
            } catch (Exception e) {
                Log.err(e);
                Vars.ui.menufrag.addButton("Mindustry Tool", Icon.settings, () -> featureSettingDialog.show());
            }
        });

        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;

            @SuppressWarnings("rawtypes")
            Seq<Cell> buttons = root.getCells();

            var buttonTitle = "MindustryTool";

            if (Vars.mobile) {
                root.row()
                        .buttonRow(buttonTitle, Icon.settings, featureSettingDialog::show)
                        .row();
            } else if (arc.util.Reflect.<Integer>get(buttons.get(buttons.size - 2), "colspan") == 2) {
                root.row()
                        .button(buttonTitle, Icon.settings, featureSettingDialog::show)
                        .colspan(2)
                        .width(450f)
                        .row();

            } else {
                root.row()
                        .button(buttonTitle, Icon.settings, featureSettingDialog::show)
                        .row();
            }
            buttons.swap(buttons.size - 1, buttons.size - 2);
        });

    }

    private void checkForUpdate() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        int[] currentVersion = extractVersionNumber(mod.meta.version);

        Http.get(Config.API_REPO_URL, (res) -> {
            try {
                Jval json = Jval.read(res.getResultAsString());

                String latestVersionStr = json.getString("version");
                int[] latestVersion = extractVersionNumber(latestVersionStr);

                if (isVersionGreater(latestVersion, currentVersion)) {
                    Log.info("Mod require update, current version: @, latest version: @",
                            versionToString(currentVersion),
                            versionToString(latestVersion));

                    fetchReleasesAndShowDialog(versionToString(currentVersion), versionToString(latestVersion));
                } else {
                    Log.info("Mod up to date");
                }
            } catch (Exception e) {
                Log.err("Failed to check update", e);
            }
        });

        Http.get(Config.API_URL + "ping?client=mod-v8").submit(result -> {
            Log.info("Ping");
        });
    }

    private void fetchReleasesAndShowDialog(String currentVer, String latestVer) {
        Http.get(Config.GITHUB_API_URL).error(e -> {
            Log.err("Failed to fetch releases", e);
            showUpdateDialog(currentVer, latestVer, "Could not fetch release notes.");
        }).submit(res -> {
            try {
                Jval json = Jval.read(res.getResultAsString());
                if (json.isArray()) {
                    Seq<Jval> releases = json.asArray();
                    StringBuilder changelog = new StringBuilder();

                    int count = 0;
                    for (Jval release : releases) {
                        if (count >= 20)
                            break;

                        String tagName = release.getString("tag_name", "");
                        String body = release.getString("body", "No description provided.");

                        changelog.append("[accent]").append(tagName).append("[]\n");
                        changelog.append(Utils.renderMarkdown(body)).append("\n\n");
                        count++;
                    }

                    showUpdateDialog(currentVer, latestVer, changelog.toString());
                } else {
                    showUpdateDialog(currentVer, latestVer, "Could not fetch release notes.");
                }
            } catch (Exception e) {
                Log.err("Failed to parse releases", e);
                showUpdateDialog(currentVer, latestVer, "Could not parse release notes.");
            }
        });
    }

    private void showUpdateDialog(String currentVer, String latestVer, String changelog) {
        Core.app.post(() -> {
            BaseDialog dialog = new BaseDialog("Update Available");

            Table table = new Table();
            table.defaults().left();

            table.add(Core.bundle.format("message.new-version", currentVer, latestVer)).wrap().width(500f).row();
            table.add("Discord: " + Config.DISCORD_INVITE_URL).color(Color.royal).padTop(5f).row();

            table.image().height(4f).color(Color.gray).fillX().pad(10f).row();

            Table changelogTable = new Table();
            changelogTable.top().left();
            changelogTable.add(changelog).wrap().width(480f).left();

            ScrollPane pane = new ScrollPane(changelogTable);
            table.add(pane).size(500f, 400f).row();

            dialog.cont.add(table);

            dialog.buttons.button("Cancel", dialog::hide).size(100f, 50f);
            dialog.buttons.button("Update", () -> {
                dialog.hide();
                Vars.ui.mods.githubImportMod(Config.REPO_URL, true, null);
                Vars.ui.mods.show();
            }).size(100f, 50f);

            dialog.show();
        });
    }

    private static int[] extractVersionNumber(String version) {
        try {
            if (version.contains("v")) {
                version = version.substring(version.indexOf("v"), version.indexOf("-"));
            }
            String clean = version.replaceAll("[^0-9.]", "");
            String[] parts = clean.split("\\.");
            int[] numbers = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
            return numbers;
        } catch (Exception e) {
            Log.err(e);
            return new int[0];
        }
    }

    private static boolean isVersionGreater(int[] v1, int[] v2) {
        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            if (v1[i] > v2[i]) {
                return true;
            } else if (v1[i] < v2[i]) {
                return false;
            }
        }

        return v1.length > v2.length;
    }

    private static String versionToString(int[] version) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < version.length; i++) {
            sb.append(version[i]);
            if (i < version.length - 1) {
                sb.append(".");
            }
        }
        return sb.toString();
    }
}
