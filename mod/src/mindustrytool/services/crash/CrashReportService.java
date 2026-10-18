package mindustrytool.services.crash;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import mindustrytool.features.FeatureManager;

public final class CrashReportService {

    private static final String KEY_LATEST = "mindustrytool.crash-report.latest";

    private static volatile CrashReportService instance;

    private final CrashLocator locator;
    private final CrashSender sender;
    private final CrashLogEnricher enricher;

    public static CrashReportService getInstance() {
        if (instance == null) {
            synchronized (CrashReportService.class) {
                if (instance == null) {
                    CrashLocator locator = new CrashLocator(
                            () -> Core.settings.getDataDirectory().child("crashes"));
                    CrashSender sender = new MindustryToolCrashSender();
                    CrashLogEnricher enricher = new CrashLogEnricher(() -> FeatureManager.getEnableds()
                            .map(f -> f.getMetadata().getId()));
                    instance = new CrashReportService(locator, sender, enricher);
                }
            }
        }
        return instance;
    }

    public CrashReportService(CrashLocator locator, CrashSender sender, CrashLogEnricher enricher) {
        this.locator = locator;
        this.sender = sender;
        this.enricher = enricher;
    }

    public CrashReportService() {
        this(
                new CrashLocator(() -> Core.settings.getDataDirectory().child("crashes")),
                new MindustryToolCrashSender(),
                new CrashLogEnricher(() -> FeatureManager.getEnableds().map(f -> f.getMetadata().getId())));
    }

    public boolean checkForCrashes() {
        try {
            Fi latest = locator.findLatest();
            if (latest == null)
                return false;

            long latestTime = CrashTimestampParser.parse(latest);
            if (latestTime == 0)
                return false;

            long savedLatest = 0L;
            try {
                savedLatest = Core.settings.getLong(KEY_LATEST, 0);
            } catch (Exception e) {
                Log.err("Failed to read " + KEY_LATEST, e);
            }

            if (latestTime == savedLatest) {
                return false;
            }

            Core.settings.put(KEY_LATEST, latestTime);

            showCrashDialog(latest);
            return true;
        } catch (Throwable t) {
            Log.err("Unexpected failure during crash check", t);
            return false;
        }
    }

    private void showCrashDialog(Fi file) {
        String log;
        try {
            log = file.readString();
        } catch (Throwable e) {
            Log.err("Failed to read crash file: " + file.absolutePath(), e);
            return;
        }

        if (!log.contains("mindustry-tool")) {
            return;
        }

        try {
            String enriched = enricher.enrich(log);
            Core.app.post(() -> new CrashReportDialog(file, enriched, sender).show());
        } catch (Throwable e) {
            Log.err("Failed to display crash report dialog", e);
        }
    }
}
