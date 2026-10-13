package mindustrytool.services.crash;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

public class CrashReportDialog extends SolimDialog {

    private static final String KEY_SEND = "mindustrytool.crash-report.send";

    public CrashReportDialog(Fi file, String data, CrashSender sender) {
        super(Core.bundle.get("crash-report.title"));
        name("crashReportDialog");

        boolean sendCrashReport = Core.settings.getBool(KEY_SEND, true);
        Signal<Boolean> sendSignal = Signal.of(sendCrashReport);
        sendSignal.subscribe(value -> Core.settings.put(KEY_SEND, Boolean.TRUE.equals(value)));

        addCloseButton();
        closeOnBack();

        String path = file != null ? file.absolutePath() : "";

        children(() -> {
            column().growX().gap(unit(2)).padding(unit(2)).width(500f).children(() -> {
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
