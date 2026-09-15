package mindustrytool.services.crash;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import mindustrytool.features.FeatureManager;

/**
 * Crash detection orchestrator. SOLID:
 *
 * <ul>
 *   <li>Single Responsibility — only orchestrates locate → dedupe → enrich → show dialog
 *   <li>Open/Closed — behaviour extended by replacing {@link CrashLocator}/{@link
 *       CrashSender}/{@link CrashLogEnricher}
 *   <li>Dependency Inversion — depends on abstractions ({@link CrashSender}) and {@link
 *       CrashLocator} facade
 *   <li>Interface Segregation handled via focused collaborators
 * </ul>
 *
 * Not over-engineered: no Dagger/Guice, no extra modules — constructor injection with a default
 * singleton.
 */
public final class CrashReportService {

	private static final String KEY_LATEST = "mindustrytool.crash-report.latest";

	private static volatile CrashReportService instance;

	private final CrashLocator locator;
	private final CrashSender sender;
	private final CrashLogEnricher enricher;

	/** Production singleton. */
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

	/** For tests / custom wiring. */
	public CrashReportService(CrashLocator locator, CrashSender sender, CrashLogEnricher enricher) {
		this.locator = locator;
		this.sender = sender;
		this.enricher = enricher;
	}

	/**
	 * No-arg production constructor — mirrors old {@code new CrashReportService()} usage in {@code
	 * Main}.
	 */
	public CrashReportService() {
		this(
				new CrashLocator(() -> Core.settings.getDataDirectory().child("crashes")),
				new MindustryToolCrashSender(),
				new CrashLogEnricher(() ->
						FeatureManager.getEnableds().map(f -> f.getMetadata().getId())));
	}

	/**
	 * Checks for a new crash file and shows the report dialog if needed.
	 *
	 * @return true if a dialog was shown (caller may disable features)
	 */
	public boolean checkForCrashes() {
		Fi latest = locator.findLatest();
		if (latest == null) return false;

		long latestTime = CrashTimestampParser.parse(latest);
		if (latestTime == 0) return false;

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
	}

	private void showCrashDialog(Fi file) {
		String log;
		try {
			log = file.readString();
		} catch (Exception e) {
			Log.err("Failed to read crash file: " + file.absolutePath(), e);
			return;
		}

		if (!log.contains("mindustry-tool")) {
			return;
		}

		String enriched = enricher.enrich(log);
		Core.app.post(() -> new CrashReportDialog(file, enriched, sender).show());
	}
}
