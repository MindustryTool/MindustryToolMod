package mindustrytool.services.crash;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import solim.overlay.SolimDialog;
import solim.signal.Signal;

/**
 * UI dialog for crash report opt-in and submission. Single Responsibility: render consent UI and
 * delegate sending to {@link CrashSender}. Uses the new API via {@link
 * mindustrytool.services.MindustryTool} through the injected sender, never constructing {@code
 * HttpClient}/{@code HttpRequest} or {@code arc.util.Http} directly.
 */
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

		content(() -> {
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

		// Send only on dialog hide if user still consents; captures sender/data via closure.
		hidden(() -> {
			if (!Core.settings.getBool(KEY_SEND, true)) return;
			if (data == null || data.isEmpty()) return;
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

	/** Convenience ctor for production — uses default sender. */
	public CrashReportDialog(Fi file, String data) {
		this(file, data, new MindustryToolCrashSender());
	}
}
