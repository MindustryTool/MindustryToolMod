package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.features.translation.providers.DeepLTranslationProvider;
import mindustrytool.features.translation.providers.DevXTranslationProvider;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import mindustrytool.features.translation.providers.GoogleWebTranslationProvider;
import solim.core.Component;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class TranslationSettingsView extends Component {

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
    private final Signal<Boolean> isTestingSignal = Signal.of(false);
    private final Signal<String> testStatusSignal = Signal.of("");

    public TranslationSettingsView(TranslationFeature feature) {
        this.feature = feature;

        boolean initialBoth = !"translated_only".equalsIgnoreCase(feature.outgoingFormatConfig.signal().peek());
        this.bothSignal = Signal.of(initialBoth);
        this.onlySignal = Signal.of(!initialBoth);
    }

    public void closeDropdown() {
        if (languageDropdown != null) {
            languageDropdown.close();
        }
    }

    @Override
    protected void onDispose() {
        closeDropdown();
    }

    @Override
    protected Element build() {
        Effect.of(this.bothSignal, checked -> {
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

        Effect.of(this.onlySignal, checked -> {
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

        Effect.of(feature.outgoingFormatConfig.signal(), fmt -> {
            boolean isBoth = !"translated_only".equalsIgnoreCase(fmt);
            if (!Boolean.valueOf(isBoth).equals(bothSignal.peek())) {
                bothSignal.set(isBoth);
            }
            if (!Boolean.valueOf(!isBoth).equals(onlySignal.peek())) {
                onlySignal.set(!isBoth);
            }
        });

        Effect.of(feature.providerConfig.signal(), id -> testStatusSignal.set(""));

        return SettingScaffold.build(() -> {
            headerCard();
            livePreviewCard();
            appearanceCard();
            outgoingCard();
            providerCard();
            resetActions();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "COMMUNICATION");
    }

    private Component livePreviewCard() {
        return SettingCard.section(Icon.chat,
                Core.bundle.get("feature.translation.incoming.preview-title", "Live Message Preview"), () -> {
                    text(Core.bundle.get("feature.translation.incoming.preview-desc",
                            "Shows how other players' messages appear in your chat."))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true), growX());

                    column().props(growX(), padding(unit(2)), rounded(6, WebStyles.Colors.SECONDARY), gap(unit(1)), left())
                            .children(() -> {
                                text("[#ffa100]Alex: [white]Bonjour! Pouvons-nous construire du thorium?")
                                        .props(left(), textWrap(true), growX());

                                String sampleTranslation = Core.bundle.get(
                                        "feature.translation.preview.incoming-sample",
                                        "Hello! Can we build thorium?");
                                Readable<String> incomingTranslatedText = computed(() -> {
                                    String hex = feature.translationColorConfig.signal().get();
                                    String colorTag = "["
                                            + (hex != null && !hex.trim().isEmpty() ? hex.trim() : "#84f491") + "]";
                                    boolean showOrig = Boolean.TRUE.equals(feature.showOriginalConfig.signal().get());
                                    return showOrig
                                            ? colorTag + "(" + sampleTranslation + ")[white]"
                                            : colorTag + "[" + sampleTranslation + "][white]";
                                });
                                text(incomingTranslatedText)
                                        .props(left(), textWrap(true), growX());
                            });

                    column().props(growX(), padding(unit(2)), rounded(6, WebStyles.Colors.SECONDARY), gap(unit(1)), left())
                            .children(() -> {
                                row().props(growX(), gap(unit(1)), left()).children(() -> {
                                    text(Core.bundle.get("feature.translation.outgoing.title", "Outgoing Chat"))
                                            .props(color(Color.lightGray), left(), textWrap(true), growX());
                                    text(feature.outgoingTargetLangConfig.signal()
                                            .map(lang -> "→ " + (lang != null ? lang : "English")))
                                            .props(color(Pal.accent), right());
                                });

                                String disabledPreview = Core.bundle.get(
                                        "feature.translation.preview.outgoing-disabled",
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

                                text(outgoingPreviewText)
                                        .props(left(), textWrap(true), growX());
                            });
                });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.spray,
                Core.bundle.get("feature.translation.appearance.title", "Appearance & Styling"), () -> {
                    column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                        text(Core.bundle.get("feature.translation.appearance.color", "Translation Color"))
                                .props(color(Color.white), left(), textWrap(true), growX());

                        wrap().props(growX(), gap(unit(1.5f))).children(() -> {
                            for (String hex : COLOR_PRESETS) {
                                Readable<Boolean> isSelected = feature.translationColorConfig.signal()
                                        .map(c -> hex.equalsIgnoreCase(c != null ? c.trim() : ""));
                                button(() -> feature.translationColorConfig.set(hex))
                                        .props(style(WebStyles.filterChip()), checked(isSelected), size(unit(8), unit(7)), prefWidth(unit(8)), prefHeight(unit(7)), padding(unit(1)))
                                        .children(() -> {
                                            row().props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)), rounded(3, Color.valueOf(hex)));
                                        });
                            }

                            row().props(height(unit(7)), prefHeight(unit(7)), paddingX(unit(1.5f)), rounded(4, WebStyles.Colors.CLEAR_BG), border(1f, WebStyles.Colors.BORDER_INPUT), gap(unit(1)), center()).children(() -> {
                                        row().props(size(unit(3.5f)), prefWidth(unit(3.5f)), prefHeight(unit(3.5f)), rounded(2, feature.translationColorConfig.signal().map(hex -> {
                                                    try {
                                                        return Color.valueOf(
                                                                hex != null && !hex.trim().isEmpty() ? hex.trim()
                                                                        : "#84f491");
                                                    } catch (Throwable t) {
                                                        return Color.gray;
                                                    }
                                                })));
                                        textField(feature.translationColorConfig.signal())
                                                .props(style(WebStyles.clearInput()), width(unit(18)), prefWidth(unit(18)), placeholder("#84f491"));
                                    });
                        });
                    });

                    divider();

                    checkbox(
                            Core.bundle.get("feature.translation.pref.show-original",
                                    "Show original message alongside translation"),
                            feature.showOriginalConfig.signal()).props(textWrap(true), growX());

                    checkbox(
                            Core.bundle.get("feature.translation.appearance.show-pill",
                                    "Show quick translation pill above chat field"),
                            feature.showPillConfig.signal()).props(textWrap(true), growX());
                });
    }

    private Component outgoingCard() {
        return SettingCard.section(Icon.upload,
                Core.bundle.get("feature.translation.outgoing.title", "Outgoing Chat"), () -> {
                    checkbox(
                            Core.bundle.get("feature.translation.outgoing.enable",
                                    "Translate outgoing chat messages automatically"),
                            feature.outgoingEnabledConfig.signal()).props(textWrap(true), growX());

                    row().props(growX(), gap(unit(2)), center()).children(() -> {
                        text(Core.bundle.get("feature.translation.outgoing.target-lang", "Outgoing Target Language"))
                                .props(color(Color.white), left(), textWrap(true), growX());
                        languageDropdown = new LanguageDropdown(feature);
                    });

                    column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                        text(Core.bundle.get("feature.translation.outgoing.format", "Outgoing Format"))
                                .props(color(Color.white), left(), textWrap(true), growX());

                        wrap().props(growX(), gap(unit(1.5f))).children(() -> {
                            button(() -> bothSignal.set(true))
                                    .props(style(WebStyles.filterChip()), checked(bothSignal), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)))
                                    .children(() -> text(Core.bundle.get("feature.translation.outgoing.format.both",
                                             "Translated (Original)")));

                            button(() -> onlySignal.set(true))
                                    .props(style(WebStyles.filterChip()), checked(onlySignal), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)))
                                    .children(() -> text(Core.bundle.get(
                                            "feature.translation.outgoing.format.translated-only", "Translated Only")));
                        });
                    });

                    row().props(growX(), padding(unit(1.5f)), rounded(4, WebStyles.Colors.SECONDARY), gap(unit(1.5f)))
                            .children(() -> {
                                icon(Icon.infoCircle).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(Pal.accent));
                                text(Core.bundle.get("feature.translation.outgoing.hint",
                                        "Tip: Press Shift + Enter to send without translating, or type // at the start of your message."))
                                        .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true), growX());
                            });
                });
    }

    private Component providerCard() {
        return SettingCard.section(Icon.settings,
                Core.bundle.get("feature.translation.settings.providers", "Translation Provider"), () -> {
                    wrap().props(growX(), gap(unit(1.5f))).children(() -> {
                        for (TranslationProvider provider : feature.getProviders()) {
                            Readable<Boolean> isSelected = feature.providerConfig.signal()
                                    .map(id -> provider.getId().equals(id));
                            button(() -> feature.providerConfig.set(provider.getId()))
                                    .props(style(WebStyles.filterChip()), checked(isSelected), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)))
                                    .children(() -> {
                                        row().props(gap(unit(1)), center()).children(() -> {
                                            text(provider.getName());
                                            if (GoogleWebTranslationProvider.ID.equals(provider.getId())) {
                                                badge(Core.bundle.get("feature.translation.provider.google-free-badge",
                                                        "Free"))
                                                        .props(color(WebStyles.Colors.PRIMARY));
                                            }
                                        });
                                    });
                        }
                    });

                    dynamic(feature.providerConfig.signal(), this::buildApiKeyRow).props(growX());

                    divider();

                    row().props(growX(), gap(unit(2)), center()).children(() -> {
                        column().props(growX(), gap(unit(0.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.translation.test.title", "Connection Test"))
                                    .props(color(Color.white), left(), textWrap(true), growX());
                            text(testStatusSignal)
                                    .props(left(), textWrap(true), growX());
                        });

                        button(this::onTestConnection)
                                .props(style(WebStyles.outline()), enabled(isTestingSignal.map(t -> !Boolean.TRUE.equals(t))), height(unit(8.5f)), prefHeight(unit(8.5f)), paddingX(unit(3)))
                                .children(() -> {
                                    row().props(gap(unit(1.5f)), center()).children(() -> {
                                        icon(Icon.play).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                        text(Core.bundle.get("feature.translation.test.button", "Check"));
                                    });
                                });
                    });

                    dynamic(feature.lastError, err -> {
                        if (err != null && !err.trim().isEmpty()) {
                            row().props(growX(), padding(unit(1.5f)), rounded(4, WebStyles.Colors.SECONDARY), gap(unit(1.5f)))
                                    .children(() -> {
                                        icon(Icon.warning).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(Color.scarlet));
                                        text(Core.bundle.format("feature.translation.last-error", err))
                                                .props(color(Color.scarlet), left(), textWrap(true), growX());
                                    });
                        }
                    });
                });
    }

    private Component resetActions() {
        return row().props(growX()).children(() -> {
            button(feature::resetToDefaults)
                    .props(style(WebStyles.outline()), height(unit(9)), prefHeight(unit(9)), growX())
                    .children(() -> {
                        row().props(gap(unit(1.5f)), center()).children(() -> {
                            icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                            text(Core.bundle.get("feature.translation.settings.reset", "Reset to Defaults"));
                        });
                    });
        });
    }

    private Component buildApiKeyRow(String providerId) {
        if (GeminiTranslationProvider.ID.equals(providerId)) {
            return column().props(growX(), gap(unit(1))).children(() -> {
                row().props(growX(), gap(unit(1)), center()).children(() -> {
                    text(Core.bundle.get("feature.translation.gemini.api-key", "Gemini API Key"))
                            .props(color(Color.lightGray), left(), textWrap(true), growX());
                    button(Core.bundle.get("feature.translation.gemini.get-key", "Get Key"),
                            () -> Core.app.openURI("https://aistudio.google.com/app/apikey"))
                            .props(style(WebStyles.outline()), height(unit(7)), prefHeight(unit(7)), paddingX(unit(2.5f)));
                });
                textField(feature.geminiApiKeyConfig.signal())
                        .props(growX(), placeholder(Core.bundle.get("feature.translation.gemini.api-key.hint", "AIzaSy...")));
            });
        } else if (DeepLTranslationProvider.ID.equals(providerId)) {
            return column().props(growX(), gap(unit(1))).children(() -> {
                row().props(growX(), gap(unit(1)), center()).children(() -> {
                    text(Core.bundle.get("feature.translation.deepl.api-key", "DeepL API Key"))
                            .props(color(Color.lightGray), left(), textWrap(true), growX());
                    button(Core.bundle.get("feature.translation.deepl.portal", "Portal"),
                            () -> Core.app.openURI("https://www.deepl.com/pro-api"))
                            .props(style(WebStyles.outline()), height(unit(7)), prefHeight(unit(7)), paddingX(unit(2.5f)));
                });
                textField(feature.deeplApiKeyConfig.signal())
                        .props(growX(), placeholder(Core.bundle.get("feature.translation.deepl.api-key.hint", "...:fx")));
            });
        } else if (DevXTranslationProvider.ID.equals(providerId)) {
            return column().props(growX(), gap(unit(1))).children(() -> {
                row().props(growX(), gap(unit(1)), center()).children(() -> {
                    text(Core.bundle.get("feature.translation.devx.api-key", "Nvidia API Key"))
                            .props(color(Color.lightGray), left(), textWrap(true), growX());
                    button(Core.bundle.get("feature.translation.devx.get-key", "Get Key"),
                            () -> Core.app.openURI("https://build.nvidia.com/"))
                            .props(style(WebStyles.outline()), height(unit(7)), prefHeight(unit(7)), paddingX(unit(2.5f)));
                });
                textField(feature.devxApiKeyConfig.signal())
                        .props(growX(), placeholder(Core.bundle.get("feature.translation.devx.api-key.hint", "nvapi-...")));
            });
        } else if (GoogleWebTranslationProvider.ID.equals(providerId)) {
            return row().props(growX(), padding(unit(1.5f)), rounded(4, WebStyles.Colors.SECONDARY), gap(unit(1.5f)))
                    .children(() -> {
                        icon(Icon.infoCircle).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(Pal.accent));
                        text(Core.bundle.get("feature.translation.google.desc",
                                "Unlimited free web translation. No API key or setup required."))
                                .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap(true), growX());
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
                        testStatusSignal.set("[scarlet]"
                                + Core.bundle.get("feature.translation.test.failed", "Connection Error") + shortMsg);
                    });
                    return null;
                });
    }
}
