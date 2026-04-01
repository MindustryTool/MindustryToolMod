package mindustrytool.services;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.layout.Scl;
import arc.util.Http;
import arc.util.Log;
import arc.util.Http.HttpStatusException;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;

import java.util.HashMap;

public class CrashReportDialog extends BaseDialog {
    public CrashReportDialog(Fi file, String data) {
        super("@crash-report.title");
        name = "crashReportDialog";

        String sendCrashReportKey = "mindustrytool.crash-report.send";
        boolean sendCrashReport = Core.settings.getBool(sendCrashReportKey, true);

        addCloseButton();
        closeOnBack();

        cont.table(container -> {
            container.add("@crash-report.content").padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.check("@crash-report.send", sendCrashReport,
                    value -> Core.settings.put(sendCrashReportKey, value)).wrapLabel(false).growX().row();
            container.add(file.absolutePath()).padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.pack();
        }).width(Math.min(600, Core.graphics.getWidth() / Scl.scl() / 1.2f));

        hidden(() -> {
            if (Core.settings.getBool(sendCrashReportKey, true)) {
                try {
                    HashMap<String, Object> json = new HashMap<>();

                    json.put("content", data);

                    Http.post(Config.API_v4_URL + "/crashes", Utils.toJson(json))
                            .header("Content-Type", "application/json")
                            .error(err -> {
                                if (err instanceof HttpStatusException httpStatusException) {
                                    Log.err(httpStatusException.response.getResultAsString());
                                }
                            })
                            .submit(res -> {
                            });
                } catch (Exception err) {
                    Log.err(err);
                }
            }
        });
    }
}
