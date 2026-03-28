package mindustrytool.services;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Timer;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UpdateService {

    public void checkForUpdate(String currentVersionString) {
        int[] currentVersion = extractVersionNumber(currentVersionString);

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
        });
    }

    private void fetchReleasesAndShowDialog(String currentVer, String latestVer) {
        Http.get(Config.GITHUB_API_URL).error(e -> {
            Log.err("Failed to fetch releases", e);
            Core.app.post(() -> {
                showUpdateDialog(currentVer, latestVer, "Could not fetch release notes.");
            });
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
                        String publishedAt = release.getString("published_at", "");
                        int downloadCount = 0;
                        if (release.has("assets")) {
                            Jval assets = release.get("assets");
                            try {
                                for (Jval asset : assets.asArray()) {
                                    downloadCount += asset.getInt("download_count", 0);
                                }
                            } catch (Exception e) {
                                Log.err("Failed to parse assets", e);
                            }
                        }

                        changelog.append("[accent]").append(tagName).append("[white]\n");

                        try {
                            if (!publishedAt.isEmpty()) {
                                Instant instant = Instant.parse(publishedAt);
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                        .withZone(ZoneId.systemDefault());
                                changelog.append("[lightgray]").append(formatter.format(instant)).append("[] - ");
                            }
                        } catch (Throwable e) {
                            Log.err(e);
                        }

                        changelog.append("[gold]Download count: ").append(downloadCount).append("[white]\n");
                        changelog.append(Utils.renderMarkdown(body)).append("\n\n");
                        count++;
                    }

                    Core.app.post(() -> {
                        showUpdateDialog(currentVer, latestVer, changelog.toString());
                    });
                } else {
                    Core.app.post(() -> {
                        showUpdateDialog(currentVer, latestVer, "Could not fetch release notes.");
                    });
                }
            } catch (Exception e) {
                Log.err("Failed to parse releases", e);
                Core.app.post(() -> {
                    showUpdateDialog(currentVer, latestVer, "Could not parse release notes.");
                });
            }
        });
    }

    private void showUpdateDialog(String currentVer, String latestVer, String changelog) {
        BaseDialog dialog = new BaseDialog("Update Available");

        dialog.name = "updateAvailableDialog";

        Table table = new Table();
        table.defaults().left();

        table.add(Core.bundle.format("message.new-version", "[#" + Color.crimson.toString() + "]" + currentVer,
                "[#" + Color.green.toString() + "]" + latestVer))
                .wrap()
                .width(500f)
                .padBottom(20)
                .row();

        table.add("Discord: " + Config.DISCORD_INVITE_URL).color(Color.royal).padTop(5f).row();

        table.image().height(4f).color(Color.gray).fillX().pad(10f).row();

        Table changelogTable = new Table();
        changelogTable.top().left();
        changelogTable.add(changelog).growX().wrap().width(480f).left();

        ScrollPane pane = new ScrollPane(changelogTable);
        table.add(pane).size(500f, 400f)
                .scrollX(false)
                .row();

        dialog.cont.add(table);

        dialog.buttons.button("Cancel", dialog::remove).size(100f, 50f);
        dialog.buttons.button("Update", () -> {
            try {
                dialog.remove();
                Vars.ui.mods.show();
                Vars.ui.mods.githubImportMod(Config.REPO_URL, true);
                Vars.ui.mods.toFront();
                Timer.schedule(() -> Vars.ui.loadfrag.toFront(), 0.2f);
            } catch (Exception e) {
                Log.err(e);
                Vars.ui.showException(e);
            }
        }).size(100f, 50f);

        Core.app.post(dialog::show);
    }

    private int[] extractVersionNumber(String version) {
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

    private boolean isVersionGreater(int[] v1, int[] v2) {
        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            if (v1[i] > v2[i]) {
                return true;
            } else if (v1[i] < v2[i]) {
                return false;
            }
        }

        return v1.length > v2.length;
    }

    private String versionToString(int[] version) {
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
