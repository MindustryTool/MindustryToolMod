package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.ui.Styles;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.features.translation.providers.DeepLTranslationProvider;
import mindustrytool.features.translation.providers.DevXTranslationProvider;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Subscription;

/**
 * Minimalist Solim settings view for Chat Translation.
 * Perfectly consistent with other feature settings dialogs.
 */
public class TranslationSettingsView extends BaseComponent {

	private final TranslationFeature feature;
	private @Nullable LanguageDropdown languageDropdown;
	private final Signal<Boolean> bothSignal;
	private final Signal<Boolean> onlySignal;
	private final @Nullable Subscription configSubscription;
	private final Signal<Boolean> isTestingSignal = Signal.of(false);
	private final Signal<String> testStatusSignal = Signal.of("");

	public TranslationSettingsView(TranslationFeature feature) {
		this.feature = feature;

		boolean initialBoth = !"translated_only".equalsIgnoreCase(feature.outgoingFormatConfig.signal().peek());
		this.bothSignal = Signal.of(initialBoth);
		this.onlySignal = Signal.of(!initialBoth);

		this.bothSignal.subscribe(checked -> {
			if (Boolean.TRUE.equals(checked)) {
				feature.outgoingFormatConfig.set("both");
				feature.outgoingShowOriginalConfig.set(true);
				if (Boolean.TRUE.equals(onlySignal.peek())) {
					onlySignal.set(false);
				}
			} else if (!Boolean.TRUE.equals(onlySignal.peek())) {
				feature.outgoingFormatConfig.set("translated_only");
				feature.outgoingShowOriginalConfig.set(false);
				onlySignal.set(true);
			}
		});

		this.onlySignal.subscribe(checked -> {
			if (Boolean.TRUE.equals(checked)) {
				feature.outgoingFormatConfig.set("translated_only");
				feature.outgoingShowOriginalConfig.set(false);
				if (Boolean.TRUE.equals(bothSignal.peek())) {
					bothSignal.set(false);
				}
			} else if (!Boolean.TRUE.equals(bothSignal.peek())) {
				feature.outgoingFormatConfig.set("both");
				feature.outgoingShowOriginalConfig.set(true);
				bothSignal.set(true);
			}
		});

		this.configSubscription = feature.outgoingFormatConfig.signal().subscribe(fmt -> {
			boolean isBoth = !"translated_only".equalsIgnoreCase(fmt);
			if (!Boolean.valueOf(isBoth).equals(bothSignal.peek())) {
				bothSignal.set(isBoth);
			}
			if (!Boolean.valueOf(!isBoth).equals(onlySignal.peek())) {
				onlySignal.set(!isBoth);
			}
		});

		feature.providerConfig.signal().subscribe(id -> testStatusSignal.set(""));
	}

	public void closeDropdown() {
		if (languageDropdown != null) {
			languageDropdown.close();
		}
	}

	@Override
	protected void onDispose() {
		closeDropdown();
		if (configSubscription != null) {
			configSubscription.dispose();
		}
	}

	@Override
	protected Element build() {
		return column().grow().center().children(() -> {
			scroll().center().children(() -> {
				column().growX().gap(unit(2)).children(() -> {

					// Provider row
					row().growX().gap(unit(2)).children(() -> {
						text(Core.bundle.get("feature.translation.settings.providers", "Provider")).left();
						spacer();
						row().gap(unit(2)).children(() -> {
							for (TranslationProvider provider : feature.getProviders()) {
								Readable<Boolean> isSelected = feature.providerConfig.signal()
										.map(id -> provider.getId().equals(id));
								button(provider.getName(), () -> feature.providerConfig.set(provider.getId()))
										.style(Styles.togglet)
										.checked(isSelected)
										.height(unit(8.5f))
										.margin(unit(1.5f), unit(4), unit(1.5f), unit(4));
							}
						});
					});

					// Dynamic API Key field if provider requires it
					dynamic(feature.providerConfig.signal(), this::buildApiKeyRow).growX();

					divider();

					// Checkbox: Show original alongside translated message
					checkbox(
							Core.bundle.get("feature.translation.pref.show-original",
									"Show original message alongside translation"),
							feature.showOriginalConfig.signal()).growX();

					// Outgoing Target Language
					row().growX().gap(unit(2)).children(() -> {
						text(Core.bundle.get("feature.translation.outgoing.target-lang", "Outgoing Target Language"))
								.left();
						spacer();
						languageDropdown = new LanguageDropdown(feature);
					});

					// Outgoing Format
					row().growX().gap(unit(2)).children(() -> {
						text(Core.bundle.get("feature.translation.outgoing.format", "Outgoing Format")).left();
						spacer();
						row().gap(unit(2)).children(() -> {
							checkbox(Core.bundle.get("feature.translation.outgoing.format.both", "Both"), bothSignal);
							checkbox(Core.bundle.get("feature.translation.outgoing.format.translated-only",
									"Translated Only"), onlySignal);
						});
					});

					divider();

					// Connection Status
					row().growX().gap(unit(2)).children(() -> {
						text(Core.bundle.get("feature.translation.test.title", "Connection Test")).left();
						spacer();
						row().gap(unit(2)).children(() -> {
							text(testStatusSignal);
							button(Core.bundle.get("feature.translation.test.button", "Check"), this::onTestConnection)
									.style(Styles.defaultb)
									.height(unit(8))
									.margin(unit(1), unit(3.5f), unit(1), unit(3.5f))
									.enabled(isTestingSignal.map(t -> !Boolean.TRUE.equals(t)));
						});
					});

					divider();

					// Reset button
					button(Core.bundle.get("feature.translation.settings.reset", "Reset to Defaults"),
							feature::resetToDefaults)
							.style(Styles.defaultb)
							.height(unit(10))
							.margin(unit(2), unit(4), unit(2), unit(4))
							.growX();
				});
			});
		}).element();
	}

	private Component buildApiKeyRow(String providerId) {
		if (GeminiTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().children(() -> {
					text(Core.bundle.get("feature.translation.gemini.api-key", "Gemini API Key")).left()
							.color(Color.lightGray).growX();
					button(Core.bundle.get("feature.translation.gemini.get-key", "Get Key"),
							() -> Core.app.openURI("https://aistudio.google.com/app/apikey"))
							.style(Styles.flatt)
							.height(unit(6.5f))
							.margin(unit(1), unit(3), unit(1), unit(3));
				});
				textField(feature.geminiApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.gemini.api-key.hint", "AIzaSy..."))
						.growX();
			});
		} else if (DeepLTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().children(() -> {
					text(Core.bundle.get("feature.translation.deepl.api-key", "DeepL API Key")).left()
							.color(Color.lightGray).growX();
					button(Core.bundle.get("feature.translation.deepl.portal", "Portal"),
							() -> Core.app.openURI("https://www.deepl.com/pro-api"))
							.style(Styles.flatt)
							.height(unit(6.5f))
							.margin(unit(1), unit(3), unit(1), unit(3));
				});
				textField(feature.deeplApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.deepl.api-key.hint", "...:fx"))
						.growX();
			});
		} else if (DevXTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().children(() -> {
					text(Core.bundle.get("feature.translation.devx.api-key", "Nvidia API Key")).left()
							.color(Color.lightGray).growX();
					button(Core.bundle.get("feature.translation.devx.get-key", "Get Key"),
							() -> Core.app.openURI("https://build.nvidia.com/"))
							.style(Styles.flatt)
							.height(unit(6.5f))
							.margin(unit(1), unit(3), unit(1), unit(3));
				});
				textField(feature.devxApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.devx.api-key.hint", "nvapi-..."))
						.growX();
			});
		}
		return column();
	}

	private void onTestConnection() {
		if (Boolean.TRUE.equals(isTestingSignal.peek())) {
			return;
		}
		isTestingSignal.set(true);
		testStatusSignal.set("[accent]" + Core.bundle.get("feature.translation.test.checking", "Checking..."));

		feature.testTranslate("ping")
				.thenAccept(result -> {
					Core.app.post(() -> {
						isTestingSignal.set(false);
						testStatusSignal.set("[#84f491]OK");
					});
				})
				.exceptionally(err -> {
					Throwable cause = err.getCause() != null ? err.getCause() : err;
					String msg = cause.getMessage() != null ? cause.getMessage() : "";
					String shortMsg = msg.contains("401") || msg.contains("API key") ? " (API Key)"
							: msg.contains("timeout") ? " (Timeout)"
							: "";
					Core.app.post(() -> {
						isTestingSignal.set(false);
						testStatusSignal.set("[scarlet]" + Core.bundle.get("feature.translation.test.failed", "Connection Error") + shortMsg);
					});
					return null;
				});
	}
}
