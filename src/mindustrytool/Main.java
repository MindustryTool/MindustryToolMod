package mindustrytool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Prov;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Timer;
import arc.util.Http.HttpStatusException;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.mod.Mods.LoadedMod;
import mindustry.net.Packet;
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
import mindustrytool.features.chat.global.ChatFeature;
import mindustrytool.features.godmode.GodModeFeature;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.background.BackgroundFeature;
import mindustrytool.features.display.wavepreview.WavePreviewFeature;
import mindustrytool.features.chat.translation.ChatTranslationFeature;
import mindustrytool.features.chat.pretty.PrettyChatFeature;

public class Main extends Mod {
    public static LoadedMod self;

    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
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
        self = Vars.mods.getMod(Main.class);
        
        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();

        Events.on(ClientLoadEvent.class, e -> {
            checkForUpdate();
            initFeatures();
            addCustomButtons();
            checkForCrashes();
        });
    }

    private void initFeatures() {
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
                new GodModeFeature(),
                new AutoplayFeature(),
                new WavePreviewFeature(),
                new BackgroundFeature());

        FeatureManager.getInstance().init();

        Seq<Prov<? extends Packet>> packetProvs = Reflect.get(Vars.net, "packetProvs");

        packetProvs.replace(packet -> {
            Class<?> clazz = packet.get().getClass();
            if (packetReplacements.containsKey(clazz)) {
                Log.info("Replace packet @ to @", clazz.getSimpleName(),
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
            Vars.ui.menufrag.addButton("Mindustry Tool", Icon.settings, () -> featureSettingDialog.show());
        }
    }

    private void checkForUpdate() {
        int[] currentVersion = extractVersionNumber(self.meta.version);

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
                Timer.schedule(() -> Vars.ui.loadfrag.toFront(), 0.5f);
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

    private static void checkForCrashes() {
        Fi crashesDir = Core.settings.getDataDirectory().child("crashes");

        if (!crashesDir.exists()) {
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss");

        var latest = Seq.with(crashesDir.list()).max(fi -> {
            String filename = fi.nameWithoutExtension();

            if (filename.startsWith("crash-report-")) {
                String time = filename.replace("crash-report-", "");
                try {
                    Date date = formatter.parse(time);
                    return (float) date.getTime();
                } catch (Exception e) {
                    Log.err(e);
                    return 0.0f;
                }
            }

            if (filename.startsWith("crash_", 0)) {
                String time = filename.replace("crash_", "");
                try {
                    return (float) Long.parseLong(time);
                } catch (Exception e) {
                    Log.err(e);
                    return 0.0f;
                }
            }

            return 0.0f;
        });

        String latestCrashKey = "latestCrash";

        var savedLatest = Core.settings.getString(latestCrashKey, "");

        if (latest == null) {
            return;
        }

        if (latest != null && latest.nameWithoutExtension().equals(savedLatest)) {
            return;
        }

        Core.settings.put(latestCrashKey, latest.nameWithoutExtension());

        showCrashDialog(latest);
    }

    private static void showCrashDialog(Fi file) {
        String sendCrashReportKey = "sendCrashReport";

        BaseDialog dialog = new BaseDialog("@crash-report.title");

        boolean sendCrashReport = Core.settings.getBool(sendCrashReportKey, true);

        dialog.addCloseButton();
        dialog.closeOnBack();

        dialog.cont.table(container -> {
            container.add("@crash-report.content").padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.check("@crash-report.send", sendCrashReport,
                    (v) -> Core.settings.put(sendCrashReportKey, v)).wrapLabel(false).growX().row();
            container.add(file.absolutePath().toString()).padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.pack();
        }).width(Math.min(600, Core.graphics.getWidth() / Scl.scl() / 1.2f));

        // Avoid bot dectection and spam on github
        // #crash-report channel;
        // Please dont nuke me
        String w = "https://disc";
        String e = "ord.com/api/webho";
        String b = "oks/14646860185309";
        String h = "02036/zCqkNjanWPJhnhhJXLvdJ0QjTL8aLTGQKuj";
        String ook = "wUAQTHQ4j2yF7NZBtYVa-QSxftUAMuewX";

        String log = file.readString();

        dialog.hidden(() -> {
            if (Core.settings.getBool(sendCrashReportKey, true)) {
                for (int i = 0; i < (log.length() / 1800) + 1; i++) {
                    String part = log.substring(i * 1800, Math.min((i + 1) * 1800, log.length()));

                    HashMap<String, Object> json = new HashMap<>();

                    json.put("content", part);

                    Http.post(w + e + b + h + ook, Utils.toJson(json))
                            .header("Content-Type", "application/json")
                            .error(err -> {
                                if (err instanceof HttpStatusException httpStatusException) {
                                    Log.err(httpStatusException.response.getResultAsString());
                                }
                            })
                            .submit(res -> Log.info(res.getResultAsString()));
                }
            }
        });

        dialog.show();
    }
}
