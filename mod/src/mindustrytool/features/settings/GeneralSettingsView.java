package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureKeybind;
import mindustrytool.features.FeatureManager;
import mindustrytool.services.crash.CrashTimestampParser;
    import solim.core.Component;

public class GeneralSettingsView extends Component {

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            preferencesCard();
            diagnosticsCard();
            keybindsCard();
        });
    }

    private void keybindsCard() {
        Seq<FeatureKeybind> allBinds = new Seq<>();
        for (Feature f : FeatureManager.getFeatures()) {
            allBinds.addAll(f.getKeybinds());
        }
        SettingKeybindCard.of(Core.bundle.get("setting.section.keybinds", "Keybinds"), allBinds);
    }

    private Component headerCard() {
        return SettingCard.header(
                Icon.settings,
                Core.bundle.get("dialog.general-settings.title"),
                "PREFERENCES",
                Core.bundle.get("dialog.general-settings.description"));
    }

    private Component preferencesCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("setting.section.preferences"), () -> {

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    text(Core.bundle.get("setting.beta.participate"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("setting.beta.participate.tooltip"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true), growX());
                });
                spacer();
                checkbox("", ModSettings.betaParticipate.signal());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    text(Core.bundle.get("setting.share-presence"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("setting.share-presence.description"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true), growX());
                });
                spacer();
                checkbox("", ModSettings.sharePresence.signal());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            SettingSlider.of(
                    Core.bundle.get("setting.universal-scale"),
                    ModSettings.universalScale.signal(),
                    0.5f, 1.5f, 0.05f,
                    ModSettings.universalScale.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));
        });
    }

    private Component diagnosticsCard() {
        return SettingCard.section(Icon.infoCircle, Core.bundle.get("setting.section.diagnostics"), () -> {
            button(this::copyLastLog)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)), tooltip(Core.bundle.get("setting.button.copy-log.tooltip")))
                    .children(() -> {
                        icon(Icon.copy).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("setting.button.copy-log"));
                    });

            button(this::copyLastCrash)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)), tooltip(Core.bundle.get("setting.button.copy-crash.tooltip")))
                    .children(() -> {
                        icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("setting.button.copy-crash"));
                    });

            button(() -> new SolimDemoDialog().show())
                    .props(style(WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)), tooltip("Open a dialog demonstrating Solim UI framework features."))
                    .children(() -> {
                        icon(Icon.box).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text("Solim Demo");
                    });
        });
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
            Vars.ui.showInfoFade(Core.bundle.format("setting.crash.copied", latest.name()) + latest.name()
                    + ") copied to clipboard.");
        } catch (Exception e) {
            Log.err("Failed to read latest crash report", e);
            Vars.ui.showException(e);
        }
    }

    private @Nullable Fi findLatestCrash(Fi crashesDir) {
        Fi[] files = crashesDir.list();
        if (files == null || files.length == 0)
            return null;

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
