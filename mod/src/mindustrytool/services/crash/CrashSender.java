package mindustrytool.services.crash;

import java.util.concurrent.CompletableFuture;

public interface CrashSender {

    CompletableFuture<Void> send(String content);
}
