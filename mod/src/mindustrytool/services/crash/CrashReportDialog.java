package mindustrytool.services.crash;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefWidth;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class CrashReportDialog extends SolimDialog {

    private static final String KEY_SEND = "mindustrytool.crash-report.send";

    public CrashReportDialog(Fi file, String data, CrashSender sender) {
        super(Core.bundle.get("crash-report.title"));
        name("crashReportDialog");

        boolean sendCrashReport = Core.settings.getBool(KEY_SEND, true);
        Signal<Boolean> sendSignal = Signal.of(sendCrashReport);

        addCloseButton();
        closeOnBack();

        String path = file != null ? file.absolutePath() : "";

        children(() -> {
            Effect.of(sendSignal, value -> Core.settings.put(KEY_SEND, Boolean.TRUE.equals(value)));

            column().props(growX(), padding(unit(2)), prefWidth(500f), gap(unit(2))).children(() -> {
                text(Core.bundle.get("crash-report.content"))
                        .wrap();

                checkbox(Core.bundle.get("crash-report.send"), sendSignal);

                if (!path.isEmpty()) {
                    text(path)
                            .wrap();
                }
            });
        });

        hidden(() -> {
            if (!Core.settings.getBool(KEY_SEND, true))
                return;
            if (data == null || data.isEmpty())
                return;
            try {
                sender.send(data).exceptionally(err -> {
                    Log.err("Failed to submit crash report", err);
                    return null;
                });
            } catch (Exception err) {
                Log.err("Failed to submit crash report", err);
            }
        });
    }

    public CrashReportDialog(Fi file, String data) {
        this(file, data, new MindustryToolCrashSender());
    }
}
