package mindustrytool.services.update;

import arc.Core;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustrytool.Main;
import mindustrytool.features.settings.ModSettings;
import mindustrytool.services.Github;
import mindustrytool.services.MindustryTool;

/**
 * Update check orchestrator. Directly delegates to {@link Github} / {@link MindustryTool} and shows
 * {@link UpdateDialog} — no indirection interfaces.
 */
public final class UpdateService {

	private static volatile UpdateService instance;

	private UpdateService() {}

	public static UpdateService getInstance() {
		if (instance == null) {
			synchronized (UpdateService.class) {
				if (instance == null) {
					instance = new UpdateService();
				}
			}
		}
		return instance;
	}

	public void checkForUpdate(Runnable done) {
		if (done == null) done = () -> {};
		final Runnable finalDone = done;

		String currentVersionString = Main.self != null && Main.self.meta != null ? Main.self.meta.version : "0";
		int[] currentVersion = VersionUtils.parseVersion(currentVersionString);
		String currentVerStr = VersionUtils.format(currentVersion);

		// fire-and-forget ping
		try {
			MindustryTool.ping("mod-v8").exceptionally(e -> null);
		} catch (Exception e) {
			Log.err("Ping failed", e);
		}

		if (isBetaParticipate()) {
			checkForUpdateBeta(currentVersion, currentVerStr, finalDone);
			return;
		}

		Github.getModHjson().whenComplete((body, err) -> {
			if (err != null) {
				Log.err(err);
				safeDone(finalDone);
				return;
			}
			try {
				Jval json = Jval.read(body);
				String latestVersionStr = json.getString("version");
				int[] latestVersion = VersionUtils.parseVersion(latestVersionStr);
				String latestVerStr = VersionUtils.format(latestVersion);

				if (VersionUtils.isGreater(latestVersion, currentVersion)) {
					Log.info(Core.bundle.format("update.status.require-update", currentVerStr, latestVerStr));
					fetchReleasesAndShowDialog(currentVerStr, latestVerStr, finalDone);
				} else {
					Log.info(Core.bundle.get("update.status.up-to-date"));
					safeDone(finalDone);
				}
			} catch (Exception e) {
				safeDone(finalDone);
				Log.err("Failed to check update", e);
			}
		});
	}

	/** Reads the beta flag defensively; any failure falls back to the stable channel. */
	private static boolean isBetaParticipate() {
		try {
			return Boolean.TRUE.equals(ModSettings.betaParticipate.get());
		} catch (Exception e) {
			Log.err("Failed to read beta flag, using stable channel", e);
			return false;
		}
	}

	/**
	 * Beta channel: releases-only gate, no {@code mod.hjson} fetch. Every failure
	 * (no network, request error, malformed/empty payload, nothing newer) resolves
	 * to silent — log and finish — so the game can never crash or stall here.
	 */
	private void checkForUpdateBeta(int[] currentVersion, String currentVerStr, Runnable done) {
		final int[] safeCurrent = currentVersion != null ? currentVersion : new int[0];
		final String safeCurrentStr = currentVerStr != null ? currentVerStr : "";
		final Runnable finalDone = done != null ? done : () -> {};

		try {
			Github.getReleases().whenComplete((body, err) -> {
				try {
					if (err != null) {
						Log.err("Beta update check failed", err);
						safeDone(finalDone);
						return;
					}
					if (body == null || body.trim().isEmpty()) {
						Log.info(Core.bundle.get("update.status.up-to-date"));
						safeDone(finalDone);
						return;
					}
					String latestTag = ChangelogFormatter.findLatestTag(body);
					if (latestTag == null) {
						Log.info(Core.bundle.get("update.status.up-to-date"));
						safeDone(finalDone);
						return;
					}
					int[] latestVersion = VersionUtils.parseVersion(latestTag);
					if (!VersionUtils.isGreater(latestVersion, safeCurrent)) {
						Log.info(Core.bundle.get("update.status.up-to-date"));
						safeDone(finalDone);
						return;
					}
					Log.info(Core.bundle.format("update.status.require-update", safeCurrentStr, latestTag));
					String changelog = ChangelogFormatter.format(body, true);
					if (changelog == null || changelog.trim().isEmpty()) {
						changelog = Core.bundle.get("update.error.parse-releases");
					}
					String finalChangelog = changelog;
					try {
						Core.app.post(() -> new UpdateDialog(safeCurrentStr, latestTag, finalChangelog, latestTag, finalDone).show());
					} catch (Exception e) {
						Log.err("Failed to show beta update dialog", e);
						safeDone(finalDone);
					}
				} catch (Exception e) {
					Log.err("Beta update check failed", e);
					safeDone(finalDone);
				}
			});
		} catch (Exception e) {
			Log.err("Beta update check failed", e);
			safeDone(finalDone);
		}
	}

	private void fetchReleasesAndShowDialog(String currentVer, String latestVer, Runnable done) {
		Github.getReleases().whenComplete((body, err) -> {
			if (err != null) {
				Log.err("Failed to fetch releases", err);
				String detail = err.getMessage() != null ? err.getMessage() : "";
				String msg;
				if (!detail.isEmpty()) {
					msg = Core.bundle.format("update.error.fetch-releases-with-status", detail);
				} else {
					msg = Core.bundle.get("update.error.fetch-releases");
				}
				String finalMsg = msg;
				Core.app.post(() -> new UpdateDialog(currentVer, latestVer, finalMsg, done).show());
				return;
			}
			try {
				boolean includePrereleases = Boolean.TRUE.equals(ModSettings.betaParticipate.get());
				String changelog = ChangelogFormatter.format(body, includePrereleases);
				if (changelog == null || changelog.trim().isEmpty()) {
					changelog = Core.bundle.get("update.error.parse-releases");
				}
				String finalChangelog = changelog;
				Core.app.post(() -> new UpdateDialog(currentVer, latestVer, finalChangelog, done).show());
			} catch (Exception e) {
				Log.err("Failed to parse releases", e);
				String msg = Core.bundle.get("update.error.parse-releases");
				Core.app.post(() -> new UpdateDialog(currentVer, latestVer, msg, done).show());
			}
		});
	}

	private static void safeDone(Runnable done) {
		try {
			done.run();
		} catch (Exception e) {
			Log.err(e);
		}
	}
}
