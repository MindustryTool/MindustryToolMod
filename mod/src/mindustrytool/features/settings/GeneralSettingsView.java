package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.services.crash.CrashTimestampParser;
import solim.core.BaseComponent;

/**
 * Settings view for mod-wide preferences and diagnostics.
 * Includes toggles for beta updates, game presence, and free camera,
 * as well as one-tap buttons to copy recent logs and crash reports.
 */
public class GeneralSettingsView extends BaseComponent {

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().growY().scrollX(false).children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    checkbox(
                            Core.bundle.get("setting.beta.participate"),
                            ModSettings.betaParticipate.signal())
                            .growX()
                            .tooltip(Core.bundle.get("setting.beta.participate.tooltip"));

                    checkbox(
                            Core.bundle.get("setting.share-presence"),
                            ModSettings.sharePresence.signal())
                            .growX()
                            .tooltip(Core.bundle.get("setting.share-presence.description"));

                    checkbox(
                            Core.bundle.get("setting.free-camera"),
                            ModSettings.freeCamera.signal())
                            .growX()
                            .tooltip(Core.bundle.get("setting.free-camera.description"));

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        button(this::copyLastLog)
                                .style(WebStyles.outline())
                                .growX()
                                .height(unit(10))
                                .tooltip(Core.bundle.get("setting.button.copy-log.tooltip"))
                                .gap(unit(2))
                                .children(() -> {
                                    icon(Icon.copy);
                                    text(Core.bundle.get("setting.button.copy-log"));
                                });

                        button(this::copyLastCrash)
                                .style(WebStyles.outline())
                                .growX()
                                .height(unit(10))
                                .tooltip(Core.bundle.get("setting.button.copy-crash.tooltip"))
                                .gap(unit(2))
                                .children(() -> {
                                    icon(Icon.warning);
                                    text(Core.bundle.get("setting.button.copy-crash"));
                                });
                    });
                });
            });
        }).element();
    }

    private void copyLastLog() {
        try {
            Fi logFile = Vars.dataDirectory.child("last_log.txt");
            if (!logFile.exists()) {
                Vars.ui.showInfoFade(Core.bundle.get("setting.log.not-found"));
                return;
            }
            String content = logFile.readString();
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle.get("setting.log.copied"));
        } catch (Exception e) {
            Log.err("Failed to read last_log.txt", e);
            Vars.ui.showException(e);
        }
    }

    private void copyLastCrash() {
        try {
            Fi crashesDir = Vars.dataDirectory.child("crashes");
            if (!crashesDir.exists() || crashesDir.list() == null) {
                Vars.ui.showInfoFade(Core.bundle.get("setting.crash.not-found"));
                return;
            }

            Fi latest = findLatestCrash(crashesDir);
            if (latest == null) {
                Vars.ui.showInfoFade(Core.bundle.get("setting.crash.not-found"));
                return;
            }

            String content = latest.readString();
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle.format("setting.crash.copied", latest.name()));
        } catch (Exception e) {
            Log.err("Failed to read latest crash report", e);
            Vars.ui.showException(e);
        }
    }

    private @Nullable Fi findLatestCrash(Fi crashesDir) {
        Fi[] files = crashesDir.list();
        if (files == null || files.length == 0) return null;

        Fi latest = null;
        long latestTime = -1;
        for (Fi f : files) {
            if (f == null || f.isDirectory() || !"txt".equalsIgnoreCase(f.extension())) {
                continue;
            }
            long parsed = CrashTimestampParser.parse(f);
            long time = parsed > 0 ? parsed : f.lastModified();
            if (time > latestTime) {
                latestTime = time;
                latest = f;
            }
        }
        return latest;
    }
}

