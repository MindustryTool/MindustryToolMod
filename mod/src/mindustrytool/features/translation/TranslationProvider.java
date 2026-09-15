package mindustrytool.features.translation;

import java.util.concurrent.CompletableFuture;

/**
 * Common contract for translation services.
 */
public interface TranslationProvider {

	/**
	 * Unique identifier for this provider (e.g. "gemini", "deepl", "mindustrytool").
	 */
	String getId();

	/**
	 * Localized display name for UI presentation.
	 */
	String getName();

	/**
	 * Translates the given text to the requested target language.
	 *
	 * @param text the message text to translate
	 * @param targetLanguage target language display name or ISO code
	 * @return CompletableFuture resolving with the translated text
	 */
	CompletableFuture<String> translate(String text, String targetLanguage);

	/**
	 * Checks whether this provider is currently configured and ready to be used.
	 */
	boolean isConfigured();

	/**
	 * Optional lifecycle callback when initialized.
	 */
	default void init() {}
}
