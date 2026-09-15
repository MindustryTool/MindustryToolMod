package mindustrytool.services.crash;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for sending crash reports. Dependency Inversion: {@link CrashReportDialog} and {@link
 * CrashReportService} depend on this interface, not on {@code MindustryTool} directly.
 */
public interface CrashSender {

	/**
	 * Submits crash content to the backend.
	 *
	 * @param content full crash log (already enriched)
	 * @return future that completes when the request finishes
	 */
	CompletableFuture<Void> send(String content);
}
