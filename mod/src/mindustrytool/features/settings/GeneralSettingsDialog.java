package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.scene.ui.Tooltip;
import mindustry.Vars;
import solim.layout.Row;
import solim.overlay.SolimDialog;

/**
 * Dialog hosting mod-wide preferences as labeled toggle rows.
 * Each toggle binds directly to its {@code ConfigValue} signal for live persistence.
 */
public class GeneralSettingsDialog extends SolimDialog {

    public GeneralSettingsDialog() {
        super(Core.bundle.get("dialog.general-settings.title", "Settings"));

        name("generalSettingsDialog");
        addCloseButton();
        closeOnBack();

        maxWidth(500f);

        content(() -> {
            column().growX().gap(unit(2)).padding(unit(2)).width(500f).children(() -> {
                scroll().grow().children(() -> {
                    column().growX().gap(unit(2)).children(() -> {
                        Row betaRow = row().growX().gap(unit(2)).children(() -> {
                            checkbox(
                                    Core.bundle.get("setting.beta.participate",
                                            "Participate in beta updates"),
                                    ModSettings.betaParticipate.signal()).growX();
                        });
                        // Solim has no tooltip API for rows; attach Arc Tooltip directly.
                        betaRow.table().addListener(new Tooltip(t -> t.add(Core.bundle.get(
                                "setting.beta.participate.tooltip",
                                "Include prerelease versions when checking for updates. Prereleases may be unstable."))));

                        Row sharePresenceRow = row().growX().gap(unit(2)).children(() -> {
                            checkbox(
                                    Core.bundle.get("setting.share-presence",
                                            "Share Game Status"),
                                    ModSettings.sharePresence.signal()).growX();
                        });
                        sharePresenceRow.table().addListener(new Tooltip(t -> t.add(Core.bundle.get(
                                "setting.share-presence.description",
                                "Show your current game activity to other chat members."))));

                        Row freeCameraRow = row().growX().gap(unit(2)).children(() -> {
                            checkbox(
                                    Core.bundle.get("setting.free-camera",
                                            "Free Camera"),
                                    ModSettings.freeCamera.signal()).growX();
                        });
                        freeCameraRow.table().addListener(new Tooltip(t -> t.add(Core.bundle.get(
                                "setting.free-camera.description",
                                "When enabled, the camera stays detached and allows free panning. When disabled, the camera automatically follows your unit."))));
                    });
                    Vars.dataDirectory.child("last_log.txt");
                });
            });
        });
    }
}
