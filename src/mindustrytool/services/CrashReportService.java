package mindustrytool.services;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustrytool.features.FeatureManager;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
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

        String latestCrashKey = "mindustrytool.crash-report.latest";

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

        Core.app.post(() -> new CrashReportDialog(file, data).show());
    }
}
