package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.services.crash.CrashTimestampParser;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for mod-wide preferences and diagnostics.
 * Includes toggles for beta updates and game presence sharing,
 * as well as one-tap buttons to copy recent logs and crash reports.
 */
public class GeneralSettingsView extends BaseComponent {

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    preferencesCard();
                    diagnosticsCard();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("dialog.general-settings.title", "Settings")
                        : "Settings")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("PREFERENCES").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("dialog.general-settings.description",
                            "Configure general mod preferences and access diagnostic reports.")
                    : "Configure general mod preferences and access diagnostic reports.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component preferencesCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("setting.section.preferences", "Mod Preferences")
                        : "Mod Preferences")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Beta updates toggle
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("setting.beta.participate", "Participate in beta updates")
                            : "Participate in beta updates")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("setting.beta.participate.tooltip",
                                    "Include prerelease versions when checking for updates. Prereleases may be unstable.")
                            : "Include prerelease versions when checking for updates. Prereleases may be unstable.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", ModSettings.betaParticipate.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Game presence toggle
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("setting.share-presence", "Share Game Status")
                            : "Share Game Status")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("setting.share-presence.description",
                                    "Show your current game activity to other chat members.")
                            : "Show your current game activity to other chat members.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", ModSettings.sharePresence.signal());
            });
        });
    }

    private Component diagnosticsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.infoCircle).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("setting.section.diagnostics", "Diagnostics")
                        : "Diagnostics")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            button(this::copyLastLog)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .tooltip(Core.bundle != null ? Core.bundle.get("setting.button.copy-log.tooltip") : "")
                    .children(() -> {
                        icon(Icon.copy).size(unit(6));
                        text(Core.bundle != null
                                ? Core.bundle.get("setting.button.copy-log", "Copy Last Log")
                                : "Copy Last Log");
                    });

            button(this::copyLastCrash)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .tooltip(Core.bundle != null ? Core.bundle.get("setting.button.copy-crash.tooltip") : "")
                    .children(() -> {
                        icon(Icon.warning).size(unit(6));
                        text(Core.bundle != null
                                ? Core.bundle.get("setting.button.copy-crash", "Copy Last Crash")
                                : "Copy Last Crash");
                    });
        });
    }

    private void copyLastLog() {
        try {
            Fi logFile = Vars.dataDirectory.child("last_log.txt");
            if (!logFile.exists()) {
                Vars.ui.showInfoFade(Core.bundle != null
                        ? Core.bundle.get("setting.log.not-found", "No log file found.")
                        : "No log file found.");
                return;
            }
            String content = logFile.readString();
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle != null
                    ? Core.bundle.get("setting.log.copied", "Log copied to clipboard.")
                    : "Log copied to clipboard.");
        } catch (Exception e) {
            Log.err("Failed to read last_log.txt", e);
            Vars.ui.showException(e);
        }
    }

    private void copyLastCrash() {
        try {
            Fi crashesDir = Vars.dataDirectory.child("crashes");
            if (!crashesDir.exists() || crashesDir.list() == null) {
                Vars.ui.showInfoFade(Core.bundle != null
                        ? Core.bundle.get("setting.crash.not-found", "No crash reports found.")
                        : "No crash reports found.");
                return;
            }

            Fi latest = findLatestCrash(crashesDir);
            if (latest == null) {
                Vars.ui.showInfoFade(Core.bundle != null
                        ? Core.bundle.get("setting.crash.not-found", "No crash reports found.")
                        : "No crash reports found.");
                return;
            }

            String content = latest.readString();
            Core.app.setClipboardText(content);
            Vars.ui.showInfoFade(Core.bundle != null
                    ? Core.bundle.format("setting.crash.copied", latest.name())
                    : "Crash report (" + latest.name() + ") copied to clipboard.");
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
