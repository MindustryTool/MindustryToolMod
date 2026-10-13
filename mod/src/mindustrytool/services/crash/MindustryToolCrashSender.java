package mindustrytool.services.crash;

import java.util.concurrent.CompletableFuture;
import mindustrytool.services.MindustryTool;

public final class MindustryToolCrashSender implements CrashSender {

    @Override
    public CompletableFuture<Void> send(String content) {
        return MindustryTool.submitCrashReport(content);
    }
}
