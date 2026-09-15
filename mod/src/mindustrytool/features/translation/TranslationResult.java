package mindustrytool.features.translation;

import arc.util.Nullable;

/**
 * Immutable data holder representing the outcome of a translation request.
 */
public final class TranslationResult {

	private final String originalText;
	private final String translatedText;
	private final String providerId;
	private final boolean success;
	private final @Nullable String errorMessage;

	public TranslationResult(
			String originalText,
			String translatedText,
			String providerId,
			boolean success,
			@Nullable String errorMessage) {
		this.originalText = originalText != null ? originalText : "";
		this.translatedText = translatedText != null ? translatedText : "";
		this.providerId = providerId != null ? providerId : "";
		this.success = success;
		this.errorMessage = errorMessage;
	}

	public static TranslationResult success(String originalText, String translatedText, String providerId) {
		return new TranslationResult(originalText, translatedText, providerId, true, null);
	}

	public static TranslationResult failure(String originalText, String providerId, String errorMessage) {
		return new TranslationResult(originalText, originalText, providerId, false, errorMessage);
	}

	public String originalText() {
		return originalText;
	}

	public String translatedText() {
		return translatedText;
	}

	public String providerId() {
		return providerId;
	}

	public boolean isSuccess() {
		return success;
	}

	public @Nullable String errorMessage() {
		return errorMessage;
	}
}
