package mindustrytool.services.auth;

import java.util.concurrent.CompletableFuture;

public interface AuthProvider {

	CompletableFuture<Void> refreshIfNeeded();

	String getAccessToken();
}
