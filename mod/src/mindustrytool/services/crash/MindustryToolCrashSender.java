package mindustrytool.services.crash;

import java.util.concurrent.CompletableFuture;
import mindustrytool.services.MindustryTool;

/**
 * Production {@link CrashSender} that delegates to {@link MindustryTool#submitCrashReport(String)}.
 * All HTTP goes through {@link mindustrytool.services.Request} via the facade, satisfying the HTTP
 * client rule.
 */
public final class MindustryToolCrashSender implements CrashSender {

	@Override
	public CompletableFuture<Void> send(String content) {
		return MindustryTool.submitCrashReport(content);
	}
}
