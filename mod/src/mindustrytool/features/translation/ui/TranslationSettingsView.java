package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.features.translation.providers.DeepLTranslationProvider;
import mindustrytool.features.translation.providers.DevXTranslationProvider;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import mindustrytool.features.translation.providers.GoogleWebTranslationProvider;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Subscription;

/**
 * Modern declarative Solim settings view for Chat Translation with tabbed navigation:
 * - Tab 0: Display & Live Message Preview
 * - Tab 1: Outgoing Chat Translation
 * - Tab 2: Translation Engine & Providers
 */
public class TranslationSettingsView extends BaseComponent {

	public static final String[] COLOR_PRESETS = {
		"#84f491",
		"#00ff00",
		"#00ffff",
		"#ffd700",
		"#ff79c6",
		"#61afef",
		"#ffffff"
	};

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
			scroll().grow().scrollX(false).children(() -> {
				column().growX().gap(unit(2.5f)).padding(unit(2)).top().children(() -> {
					livePreviewCard();
					appearanceCard();
					outgoingCard();
					providerCard();
					resetActions();
				});
			});
		}).element();
	}

	private Card sectionCard() {
		return card()
				.growX()
				.padding(unit(2.5f))
				.rounded(8, WebStyles.Colors.SECTION_BG)
				.border(1.5f, WebStyles.Colors.SECTION_BORDER)
				.gap(unit(2));
	}

	private Component livePreviewCard() {
		return sectionCard().children(() -> {
			// Header
			row().growX().gap(unit(2)).center().children(() -> {
				icon(Icon.chat).size(unit(5)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.incoming.preview-title", "Live Message Preview"))
						.color(Pal.accent)
						.growX()
						.left();
				badge(Core.bundle.get("feature.pretty-chat.settings.live-badge", "LIVE")).color(WebStyles.Colors.PRIMARY);
			});

			text(Core.bundle.get("feature.translation.incoming.preview-desc",
					"Shows how other players' messages appear in your chat."))
					.color(WebStyles.Colors.GHOST_FG)
					.left()
					.wrap()
					.growX();

			// Incoming Simulation Box
			column().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1)).left().children(() -> {
				text("[#ffa100]Alex: [white]Bonjour! Pouvons-nous construire du thorium?").left();

				String sampleTranslation = Core.bundle.get("feature.translation.preview.incoming-sample",
						"Hello! Can we build thorium?");
				Readable<String> incomingTranslatedText = computed(() -> {
					String hex = feature.translationColorConfig.signal().get();
					String colorTag = "[" + (hex != null && !hex.trim().isEmpty() ? hex.trim() : "#84f491") + "]";
					boolean showOrig = Boolean.TRUE.equals(feature.showOriginalConfig.signal().get());
					return showOrig
							? colorTag + "(" + sampleTranslation + ")[white]"
							: colorTag + "[" + sampleTranslation + "][white]";
				});
				text(incomingTranslatedText).left().wrap();
			});

			// Outgoing Simulation Box
			column().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1)).left().children(() -> {
				row().growX().gap(unit(1)).left().children(() -> {
					text(Core.bundle.get("feature.translation.outgoing.title", "Outgoing Chat")).color(Color.lightGray).left();
					spacer();
					text(feature.outgoingTargetLangConfig.signal().map(lang -> "→ " + (lang != null ? lang : "English"))).color(Pal.accent).right();
				});

				String disabledPreview = Core.bundle.get("feature.translation.preview.outgoing-disabled",
						"[lightgray](Outgoing translation is OFF - messages sent as typed)[white]");
				String bothPreview = Core.bundle.get("feature.translation.preview.outgoing-both",
						"I need more copper [gray](Tôi cần thêm đồng)[white]");
				String onlyPreview = Core.bundle.get("feature.translation.preview.outgoing-only",
						"I need more copper");

				Readable<String> outgoingPreviewText = computed(() -> {
					boolean enabled = Boolean.TRUE.equals(feature.outgoingEnabledConfig.signal().get());
					if (!enabled) {
						return disabledPreview;
					}
					String fmt = feature.outgoingFormatConfig.signal().get();
					boolean isBoth = !"translated_only".equalsIgnoreCase(fmt);
					return isBoth ? bothPreview : onlyPreview;
				});

				text(outgoingPreviewText).left().wrap();
			});
		});
	}

	private Component appearanceCard() {
		return sectionCard().children(() -> {
			// Section Header
			row().growX().gap(unit(2)).center().children(() -> {
				icon(Icon.spray).size(unit(5)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.appearance.title", "Appearance & Styling"))
						.color(Pal.accent)
						.growX()
						.left();
			});

			// Highlight Color Palette
			column().growX().gap(unit(1.5f)).left().children(() -> {
				text(Core.bundle.get("feature.translation.appearance.color", "Translation Color"))
						.left()
						.color(Color.white);

				wrap().growX().gap(unit(1.5f)).children(() -> {
					for (String hex : COLOR_PRESETS) {
						Readable<Boolean> isSelected = feature.translationColorConfig.signal()
								.map(c -> hex.equalsIgnoreCase(c != null ? c.trim() : ""));
						button(() -> feature.translationColorConfig.set(hex))
								.style(WebStyles.filterChip())
								.checked(isSelected)
								.size(unit(8), unit(7))
								.padding(unit(1))
								.children(() -> {
									row().size(unit(4.5f)).rounded(3, Color.valueOf(hex));
								});
					}

					// Custom hex input with color indicator swatch
					row().height(unit(7)).gap(unit(1)).paddingX(unit(1.5f))
							.rounded(4, WebStyles.Colors.CLEAR_BG)
							.border(1f, WebStyles.Colors.BORDER_INPUT)
							.center().children(() -> {
						row().size(unit(3.5f)).rounded(2, feature.translationColorConfig.signal().map(hex -> {
							try {
								return Color.valueOf(hex != null && !hex.trim().isEmpty() ? hex.trim() : "#84f491");
							} catch (Throwable t) {
								return Color.gray;
							}
						}));
						textField(feature.translationColorConfig.signal())
								.placeholder("#84f491")
								.width(unit(18))
								.style(WebStyles.clearInput());
					});
				});
			});

			divider();

			// Checkbox: Show original alongside translated message
			checkbox(
					Core.bundle.get("feature.translation.pref.show-original",
							"Show original message alongside translation"),
					feature.showOriginalConfig.signal()).growX();

			// Checkbox: Show In-Game Chat HUD Pill
			checkbox(
					Core.bundle.get("feature.translation.appearance.show-pill",
							"Show quick translation pill above chat field"),
					feature.showPillConfig.signal()).growX();
		});
	}

	private Component outgoingCard() {
		return sectionCard().children(() -> {
			// Section Header
			row().growX().gap(unit(2)).center().children(() -> {
				icon(Icon.upload).size(unit(5)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.outgoing.title", "Outgoing Chat"))
						.color(Pal.accent)
						.growX()
						.left();
			});

			// Toggle Outgoing translation
			checkbox(
					Core.bundle.get("feature.translation.outgoing.enable",
							"Translate outgoing chat messages automatically"),
					feature.outgoingEnabledConfig.signal()).growX();

			// Target Language row
			row().growX().gap(unit(2)).center().children(() -> {
				text(Core.bundle.get("feature.translation.outgoing.target-lang", "Outgoing Target Language"))
						.left()
						.color(Color.white);
				spacer();
				languageDropdown = new LanguageDropdown(feature);
			});

			// Format row
			column().growX().gap(unit(1.5f)).left().children(() -> {
				text(Core.bundle.get("feature.translation.outgoing.format", "Outgoing Format"))
						.left()
						.color(Color.white);

				row().gap(unit(1.5f)).children(() -> {
					button(() -> bothSignal.set(true))
							.style(WebStyles.filterChip())
							.checked(bothSignal)
							.height(unit(8))
							.paddingX(unit(2.5f))
							.children(() -> text(Core.bundle.get("feature.translation.outgoing.format.both", "Translated (Original)")));

					button(() -> onlySignal.set(true))
							.style(WebStyles.filterChip())
							.checked(onlySignal)
							.height(unit(8))
							.paddingX(unit(2.5f))
							.children(() -> text(Core.bundle.get("feature.translation.outgoing.format.translated-only", "Translated Only")));
				});
			});

			// Quick hint
			row().growX().gap(unit(1.5f)).padding(unit(1.5f)).rounded(4, WebStyles.Colors.SECONDARY).children(() -> {
				icon(Icon.infoCircle).size(unit(4)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.outgoing.hint",
						"Tip: Press Shift + Enter to send without translating, or type // at the start of your message."))
						.color(WebStyles.Colors.GHOST_FG)
						.wrap()
						.growX();
			});
		});
	}

	private Component providerCard() {
		return sectionCard().children(() -> {
			// Section Header
			row().growX().gap(unit(2)).center().children(() -> {
				icon(Icon.settings).size(unit(5)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.settings.providers", "Translation Provider"))
						.color(Pal.accent)
						.growX()
						.left();
			});

			// Provider chips wrap
			wrap().growX().gap(unit(1.5f)).children(() -> {
				for (TranslationProvider provider : feature.getProviders()) {
					Readable<Boolean> isSelected = feature.providerConfig.signal()
							.map(id -> provider.getId().equals(id));
					button(() -> feature.providerConfig.set(provider.getId()))
							.style(WebStyles.filterChip())
							.checked(isSelected)
							.height(unit(8))
							.paddingX(unit(2.5f))
							.children(() -> {
								row().gap(unit(1)).center().children(() -> {
									text(provider.getName());
									if (GoogleWebTranslationProvider.ID.equals(provider.getId())) {
										badge(Core.bundle.get("feature.translation.provider.google-free-badge", "Free"))
												.color(WebStyles.Colors.PRIMARY);
									}
								});
							});
				}
			});

			// Dynamic Provider Configuration (API Key, links, etc.)
			dynamic(feature.providerConfig.signal(), this::buildApiKeyRow).growX();

			divider();

			// Connection Test row
			row().growX().gap(unit(2)).center().children(() -> {
				column().gap(unit(0.5f)).left().children(() -> {
					text(Core.bundle.get("feature.translation.test.title", "Connection Test")).left().color(Color.white);
					text(testStatusSignal).left();
				});

				spacer();

				button(this::onTestConnection)
						.style(WebStyles.outline())
						.height(unit(8.5f))
						.paddingX(unit(3))
						.enabled(isTestingSignal.map(t -> !Boolean.TRUE.equals(t)))
						.children(() -> {
							row().gap(unit(1.5f)).center().children(() -> {
								icon(Icon.play).size(unit(4));
								text(Core.bundle.get("feature.translation.test.button", "Check"));
							});
						});
			});

			// Error indicator if last error is present
			dynamic(feature.lastError, err -> {
				if (err != null && !err.trim().isEmpty()) {
					row().growX().gap(unit(1.5f)).padding(unit(1.5f)).rounded(4, WebStyles.Colors.SECONDARY).children(() -> {
						icon(Icon.warning).size(unit(4)).color(Color.scarlet);
						text(Core.bundle.format("feature.translation.last-error", err))
								.color(Color.scarlet)
								.wrap()
								.growX();
					});
				}
			});
		});
	}

	private Component resetActions() {
		return row().growX().children(() -> {
			button(feature::resetToDefaults)
					.style(WebStyles.outline())
					.height(unit(9))
					.growX()
					.children(() -> {
						row().gap(unit(1.5f)).center().children(() -> {
							icon(Icon.refresh).size(unit(4));
							text(Core.bundle.get("feature.translation.settings.reset", "Reset to Defaults"));
						});
					});
		});
	}

	private Component buildApiKeyRow(String providerId) {
		if (GeminiTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().gap(unit(1)).center().children(() -> {
					text(Core.bundle.get("feature.translation.gemini.api-key", "Gemini API Key"))
							.left()
							.color(Color.lightGray)
							.growX();
					button(Core.bundle.get("feature.translation.gemini.get-key", "Get Key"),
							() -> Core.app.openURI("https://aistudio.google.com/app/apikey"))
							.style(WebStyles.outline())
							.height(unit(7))
							.paddingX(unit(2.5f));
				});
				textField(feature.geminiApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.gemini.api-key.hint", "AIzaSy..."))
						.growX();
			});
		} else if (DeepLTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().gap(unit(1)).center().children(() -> {
					text(Core.bundle.get("feature.translation.deepl.api-key", "DeepL API Key"))
							.left()
							.color(Color.lightGray)
							.growX();
					button(Core.bundle.get("feature.translation.deepl.portal", "Portal"),
							() -> Core.app.openURI("https://www.deepl.com/pro-api"))
							.style(WebStyles.outline())
							.height(unit(7))
							.paddingX(unit(2.5f));
				});
				textField(feature.deeplApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.deepl.api-key.hint", "...:fx"))
						.growX();
			});
		} else if (DevXTranslationProvider.ID.equals(providerId)) {
			return column().growX().gap(unit(1)).children(() -> {
				row().growX().gap(unit(1)).center().children(() -> {
					text(Core.bundle.get("feature.translation.devx.api-key", "Nvidia API Key"))
							.left()
							.color(Color.lightGray)
							.growX();
					button(Core.bundle.get("feature.translation.devx.get-key", "Get Key"),
							() -> Core.app.openURI("https://build.nvidia.com/"))
							.style(WebStyles.outline())
							.height(unit(7))
							.paddingX(unit(2.5f));
				});
				textField(feature.devxApiKeyConfig.signal())
						.placeholder(Core.bundle.get("feature.translation.devx.api-key.hint", "nvapi-..."))
						.growX();
			});
		} else if (GoogleWebTranslationProvider.ID.equals(providerId)) {
			return row().growX().gap(unit(1.5f)).padding(unit(1.5f)).rounded(4, WebStyles.Colors.SECONDARY).children(() -> {
				icon(Icon.infoCircle).size(unit(4)).color(Pal.accent);
				text(Core.bundle.get("feature.translation.google.desc",
						"Unlimited free web translation. No API key or setup required."))
						.color(WebStyles.Colors.GHOST_FG)
						.wrap()
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

		feature.testTranslate("Hello")
				.thenAccept(result -> {
					feature.lastError.set(null);
					Core.app.post(() -> {
						isTestingSignal.set(false);
						testStatusSignal.set("[#84f491]OK (" + (result != null ? result : "") + ")");
					});
				})
				.exceptionally(err -> {
					Throwable cause = err.getCause() != null ? err.getCause() : err;
					String msg = cause.getMessage() != null ? cause.getMessage() : "";
					feature.lastError.set(msg);
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
