package mindustrytool.services;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Http.HttpStatusException;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.FeatureManager;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;

public class CrashReportService {

    private final SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss");

    public boolean checkForCrashes() {
        Fi crashesDir = Core.settings.getDataDirectory().child("crashes");

        if (!crashesDir.exists()) {
            return false;
        }

        var latest = Seq.with(crashesDir.list()).max(this::parseCrashTime);

        long epoch = LocalDate.of(2026, 1, 25)
                .atStartOfDay(ZoneOffset.UTC)
                .toEpochSecond() * 1000;

        if (parseCrashTime(latest) < epoch) {
            return false;
        }

        String latestCrashKey = "latestCrash";

        var savedLatest = 0L;

        try {
            savedLatest = Core.settings.getLong(latestCrashKey, 0);
        } catch (Exception e) {
            Log.err(e);
        }

        if (latest == null) {
            return false;
        }

        if (parseCrashTime(latest) == savedLatest) {
            return false;
        }

        Core.settings.put(latestCrashKey, parseCrashTime(latest));

        showCrashDialog(latest);

        return true;
    }

    private long parseCrashTime(Fi file) {
        if (file == null) {
            return 0;
        }

        String filename = file.nameWithoutExtension();

        if (filename.startsWith("crash-report-")) {
            String time = filename.replace("crash-report-", "");
            try {
                Date date = formatter.parse(time);
                return date.getTime();
            } catch (Exception e) {
                Log.err(e);
                return 0;
            }
        }

        if (filename.startsWith("crash_", 0)) {
            String time = filename.replace("crash_", "");
            try {
                return Long.parseLong(time);
            } catch (Exception e) {
                Log.err(e);
                return 0;
            }
        }

        return 0;
    }

    private void showCrashDialog(Fi file) {
        String log = file.readString();

        boolean hasMindustryTool = log.contains("mindustry-tool");

        if (!hasMindustryTool) {
            return;
        }

        int separatorIndex = log.indexOf("\n\n");

        if (separatorIndex != -1) {
            var firstPart = log.substring(0, separatorIndex);
            var secondPart = log.substring(separatorIndex);
            var enabledFeatures = FeatureManager.getInstance().getEnableds().map(f -> f.getMetadata().name());
            var enabledFeatureString = Strings.join(",", enabledFeatures);

            log = firstPart + "\n" + "Enabled features: " + enabledFeatureString + secondPart;
        }

        var data = log;

        String sendCrashReportKey = "sendCrashReport";

        BaseDialog dialog = new BaseDialog("@crash-report.title");

        dialog.name = "crashReportDialog";

        boolean sendCrashReport = Core.settings.getBool(sendCrashReportKey, true);

        dialog.addCloseButton();
        dialog.closeOnBack();

        dialog.cont.table(container -> {
            container.add("@crash-report.content").padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.check("@crash-report.send", sendCrashReport,
                    (v) -> Core.settings.put(sendCrashReportKey, v)).wrapLabel(false).growX().row();
            container.add(file.absolutePath()).padBottom(10f).wrapLabel(true).wrap().growX().row();
            container.pack();
        }).width(Math.min(600, Core.graphics.getWidth() / Scl.scl() / 1.2f));

        dialog.hidden(() -> {
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
                                Log.info(res.getResultAsString());
                            });
                } catch (Exception err) {
                    Log.err(err);
                }
            }
        });

        Core.app.post(dialog::show);
    }
}
