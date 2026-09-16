package mindustrytool.features.translation;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import solim.overlay.SolimDialog;

import arc.scene.ui.TextField;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Strings;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.ui.dialogs.LanguageDialog;
import mindustry.core.NetClient;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;
import mindustry.gen.SendMessageCallPacket;
import mindustry.gen.SendMessageCallPacket2;
import mindustry.input.Binding;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import mindustrytool.features.translation.providers.DeepLTranslationProvider;
import mindustrytool.features.translation.providers.DevXTranslationProvider;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import mindustrytool.features.translation.ui.OutgoingLanguageDialog;
import mindustrytool.features.translation.ui.TranslationSettingsDialog;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.prettychat.PrettyChatFeature;
import mindustrytool.services.PacketReplacer;
import solim.signal.Signal;

/**
 * Feature that provides in-game multiplayer chat translation.
 */
public class TranslationFeature extends Feature {

    public final ConfigGroup config;

    // Provider selection
    public final ConfigValue<String> providerConfig;
    public final ConfigValue<Boolean> showOriginalConfig;

    // Gemini configs
    public final ConfigValue<String> geminiApiKeyConfig;
    public final ConfigValue<String> geminiModelConfig;
    public final ConfigValue<Integer> geminiTimeoutConfig;
    public final ConfigValue<Integer> geminiMaxHistoryConfig;

    // DeepL configs
    public final ConfigValue<String> deeplApiKeyConfig;
    public final ConfigValue<Integer> deeplTimeoutConfig;

    // MindustryTool configs
    public final ConfigValue<Integer> mindustryToolTimeoutConfig;

    // DevXconfigs
    public final ConfigValue<String> devxApiKeyConfig;
    public final ConfigValue<String> devxModelConfig;
    public final ConfigValue<Integer> devxTimeoutConfig;
    public final ConfigValue<Integer> devxMaxHistoryConfig;

    // Outgoing translation (Dịch ngược) configs
    public final ConfigValue<Boolean> outgoingEnabledConfig;
    public final ConfigValue<String> outgoingTargetLangConfig;
    public final ConfigValue<String> outgoingFormatConfig;
    public final ConfigValue<Boolean> outgoingShowOriginalConfig;

    // Diagnostic state
    public final Signal<String> lastError = Signal.of(null);

    private final Seq<TranslationProvider> providers = new Seq<>();
    private @Nullable TranslationSettingsDialog settingsDialog;

    public class SendTranslatedMessageCallPacket extends SendMessageCallPacket {
        @Override
        public void handleClient() {
            handleIncomingMessage(this.message, formatted -> NetClient.sendMessage(formatted));
        }
    }

    public class SendTranslatedMessageCallPacket2 extends SendMessageCallPacket2 {
        @Override
        public void handleClient() {
            if (Vars.player != null && Vars.player == this.playersender) {
                NetClient.sendMessage(this.message, this.unformatted, this.playersender);
            } else {
                handleIncomingMessage(this.message, formatted -> {
                    NetClient.sendMessage(formatted, this.unformatted, this.playersender);
                });
            }
        }
    }

    public TranslationFeature() {
        super(FeatureMetadata.builder()
                .id("translation")
                .icon(FileIcon.of("translate.png"))
                .order(22)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();

        providerConfig = config.stringValue("provider", GeminiTranslationProvider.ID);
        showOriginalConfig = config.boolValue("show-original", true);

        // Outgoing translation configs
        outgoingEnabledConfig = config.boolValue("outgoing.enabled", true);
        outgoingTargetLangConfig = config.stringValue("outgoing.target-lang", "English");
        outgoingFormatConfig = config.stringValue("outgoing.format", "both");
        outgoingShowOriginalConfig = config.boolValue("outgoing.show-original", true);

        geminiApiKeyConfig = config.stringValue("gemini.api-key", "");
        geminiModelConfig = config.stringValue("gemini.model", GeminiTranslationProvider.MODELS[0]);
        geminiTimeoutConfig = config.intValue("gemini.timeout", 10);
        geminiMaxHistoryConfig = config.intValue("gemini.max-history", 5);

        deeplApiKeyConfig = config.stringValue("deepl.api-key", "");
        deeplTimeoutConfig = config.intValue("deepl.timeout", 10);

        mindustryToolTimeoutConfig = config.intValue("mindustrytool.timeout", 30);

        devxApiKeyConfig = config.stringValue("devx.api-key", "");
        if ("API".equalsIgnoreCase(devxApiKeyConfig.get())) {
            devxApiKeyConfig.set("");
        }
        if (Core.settings != null && "API".equalsIgnoreCase(Core.settings.getString("devx.apiKey", ""))) {
            Core.settings.remove("devx.apiKey");
        }
        devxModelConfig = config.stringValue("devx.model", DevXTranslationProvider.MODEL);
        devxTimeoutConfig = config.intValue("devx.timeout", 10);
        devxMaxHistoryConfig = config.intValue("devx.max-history", 5);

        providers.add(new GeminiTranslationProvider(this));
        providers.add(new DevXTranslationProvider(this));
        providers.add(new DeepLTranslationProvider(this));

        // Register packet replacements for incoming translation
        PacketReplacer.register(SendMessageCallPacket.class, SendTranslatedMessageCallPacket::new);
        PacketReplacer.register(SendMessageCallPacket2.class, SendTranslatedMessageCallPacket2::new);

        // Register outgoing chat hook
        Events.run(Trigger.update, this::updateChatHook);
    }

    public Seq<TranslationProvider> getProviders() {
        return providers;
    }

    public TranslationProvider getActiveProvider() {
        String id = providerConfig.get();
        TranslationProvider found = providers.find(p -> p.getId().equals(id));
        return found != null ? found : providers.first();
    }

    public String getTargetLanguage() {
        Locale locale = Core.bundle != null ? Core.bundle.getLocale() : Locale.getDefault();
        String display = locale.getDisplayLanguage(Locale.ENGLISH);
        return (display != null && !display.trim().isEmpty()) ? display : "English";
    }

    public Locale getOutgoingTargetLocale() {
        String lang = outgoingTargetLangConfig.get();
        if (lang == null || lang.trim().isEmpty() || "none".equalsIgnoreCase(lang.trim())) {
            return Locale.ENGLISH;
        }
        String clean = lang.trim();
        for (Locale loc : Vars.locales) {
            if (clean.equalsIgnoreCase(loc.toString())
                    || clean.equalsIgnoreCase(loc.getLanguage())
                    || clean.equalsIgnoreCase(LanguageDialog.getDisplayName(loc))
                    || clean.equalsIgnoreCase(loc.getDisplayLanguage(Locale.ENGLISH))) {
                return loc;
            }
        }
        return Locale.ENGLISH;
    }

    public String getOutgoingTargetLanguage() {
        Locale loc = getOutgoingTargetLocale();
        String display = loc.getDisplayLanguage(Locale.ENGLISH);
        return (display != null && !display.trim().isEmpty()) ? display : "English";
    }

    public String getOutgoingTargetLanguageDisplayName(String current) {
        if ("none".equalsIgnoreCase(current) || !Boolean.TRUE.equals(outgoingEnabledConfig.get())) {
            return Core.bundle.get("feature.translation.outgoing.none", Core.bundle.get("none", "None"));
        }
        Locale loc = getOutgoingTargetLocale();
        return LanguageDialog.getDisplayName(loc);
    }

    public void setOutgoingTargetLocale(Locale loc) {
        outgoingTargetLangConfig.set(loc.toString());
    }

    public boolean isSameLanguage(String current, Locale loc) {
        return getOutgoingTargetLocale().equals(loc);
    }

    public boolean shouldTranslateOutgoing(String raw) {
        if (!isEnabled() || raw == null) {
            return false;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return false;
        }
        // Escape prefix: //message bypasses translation
        if (text.startsWith("//")) {
            return false;
        }
        // Fast command prefixes: /tr <text> or /dich <text>
        if (text.startsWith("/tr ") || text.startsWith("/dich ")) {
            return true;
        }
        // If outgoing translation is disabled or target language is none, do not
        // translate
        if (!Boolean.TRUE.equals(outgoingEnabledConfig.get())
                || "none".equalsIgnoreCase(outgoingTargetLangConfig.get())) {
            return false;
        }
        // Allow /t (team chat) and /a (admin chat) to be translated
        if (text.startsWith("/t ") || text.startsWith("/a ")) {
            return true;
        }
        // Other commands starting with / are game commands (e.g. /vote, /help)
        if (text.startsWith("/")) {
            return false;
        }
        return true;
    }

    public static class OutgoingParts {
        public final String commandPrefix;
        public final String content;

        public OutgoingParts(String commandPrefix, String content) {
            this.commandPrefix = commandPrefix;
            this.content = content;
        }
    }

    public static OutgoingParts extractOutgoingParts(String raw) {
        if (raw == null) {
            return new OutgoingParts("", "");
        }
        String text = raw.trim();
        String cmdPrefix = "";

        if (text.startsWith("/tr ")) {
            text = text.substring(4).trim();
        } else if (text.startsWith("/dich ")) {
            text = text.substring(6).trim();
        }

        if (text.startsWith("/t ") || text.startsWith("/a ")) {
            cmdPrefix = text.substring(0, 3);
            text = text.substring(3).trim();
        }

        return new OutgoingParts(cmdPrefix, text);
    }

    public String formatOutgoingMessage(String original, String translated) {
        if (translated == null || translated.trim().isEmpty() || translated.equalsIgnoreCase(original)) {
            return original;
        }
        String format = outgoingFormatConfig.get();
        if ("translated_only".equalsIgnoreCase(format) || !Boolean.TRUE.equals(outgoingShowOriginalConfig.get())) {
            return translated;
        }
        String combined = translated + " (" + original + ")";
        if (combined.length() <= Vars.maxTextLength) {
            return combined;
        }
        int remainingForOriginal = Vars.maxTextLength - translated.length() - 5;
        if (remainingForOriginal >= 5) {
            return translated + " (" + original.substring(0, remainingForOriginal) + "...)";
        }
        return translated.length() <= Vars.maxTextLength ? translated : translated.substring(0, Vars.maxTextLength);
    }

    public void handleOutgoingMessage(String rawMessage, Cons<String> onDeliver) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            onDeliver.get(rawMessage != null ? rawMessage : "");
            return;
        }

        OutgoingParts parts = extractOutgoingParts(rawMessage);
        if (parts.content.isEmpty()) {
            onDeliver.get(rawMessage);
            return;
        }

        TranslationProvider provider = getActiveProvider();
        if (!provider.isConfigured()) {
            onDeliver.get(rawMessage);
            return;
        }

        String targetLang = getOutgoingTargetLanguage();
        provider.translate(parts.content, targetLang)
                .thenAccept(translated -> {
                    String formatted = formatOutgoingMessage(parts.content, translated);
                    String finalMessage = parts.commandPrefix + formatted;
                    onDeliver.get(finalMessage);
                })
                .exceptionally(err -> {
                    Throwable cause = err.getCause() != null ? err.getCause() : err;
                    lastError.set(cause.getMessage());
                    Log.warn("Outgoing translation failed: @", cause.getMessage());
                    // Never drop outgoing message on error
                    onDeliver.get(rawMessage);
                    return null;
                });
    }

    private void updateChatHook() {
        if (!isEnabled() || Vars.ui == null || Vars.ui.chatfrag == null || !Vars.ui.chatfrag.shown()) {
            return;
        }
        if (Core.input == null || !Core.input.keyTap(Binding.chat)) {
            return;
        }
        try {
            TextField chatfield = Reflect.get(Vars.ui.chatfrag, "chatfield");
            if (chatfield == null) {
                return;
            }
            String raw = chatfield.getText();
            if (raw == null || raw.trim().isEmpty()) {
                return;
            }
            String text = raw.trim();

            // Escape prefix: //message sends "message" directly without translation or prettifying
            if (text.startsWith("//")) {
                return;
            }

            if (!shouldTranslateOutgoing(text)) {
                return;
            }

            // Clear the field immediately so Mindustry's scheduled sendMessage() will see
            // empty text and do nothing!
            chatfield.setText("");

            handleOutgoingMessage(text, translatedMsg -> {
                PrettyChatFeature prettyChat = FeatureManager.getFeature(PrettyChatFeature.class);
                String toSend = translatedMsg;
                if (prettyChat != null && prettyChat.isEnabled()) {
                    toSend = prettyChat.transform(toSend);
                }
                if (toSend.length() > Vars.maxTextLength) {
                    toSend = PrettyChatFeature.clampSafe(toSend, Vars.maxTextLength);
                }
                final String finalMsg = toSend;
                Core.app.post(() -> Call.sendChatMessage(finalMsg));
            });
        } catch (Exception e) {
            Log.err("Error handling outgoing chat translation: @", e.getMessage());
        }
    }

    public void handleIncomingMessage(String message, Cons<String> onDeliver) {
        if (!isEnabled() || message == null || message.trim().isEmpty()) {
            onDeliver.get(message);
            return;
        }

        String cleanText = Strings.stripColors(message).trim();
        if (cleanText.isEmpty()) {
            onDeliver.get(message);
            return;
        }

        TranslationProvider provider = getActiveProvider();
        if (!provider.isConfigured()) {
            onDeliver.get(message);
            return;
        }

        String targetLang = getTargetLanguage();
        provider.translate(cleanText, targetLang)
                .thenAccept(translated -> {
                    Core.app.post(() -> {
                        if (translated == null || translated.trim().isEmpty()
                                || translated.equalsIgnoreCase(cleanText)) {
                            onDeliver.get(message);
                        } else {
                            String formatted;
                            if (Boolean.TRUE.equals(showOriginalConfig.get())) {
                                formatted = message + " [gold](" + translated + ")[white]";
                            } else {
                                formatted = "[gold][" + translated + "][white]";
                            }
                            onDeliver.get(formatted);
                        }
                    });
                })
                .exceptionally(err -> {
                    Throwable cause = err.getCause() != null ? err.getCause() : err;
                    lastError.set(cause.getMessage());
                    Log.warn("Translation failed: @", cause.getMessage());
                    Core.app.post(() -> {
                        // Never drop messages on error
                        onDeliver.get(message);
                    });
                    return null;
                });
    }

    public CompletableFuture<String> testTranslate(String text) {
        String targetLang = getTargetLanguage();
        return getActiveProvider().translate(text, targetLang);
    }

    public CompletableFuture<String> translate(String text, String targetLanguage) {
        return getActiveProvider().translate(text, targetLanguage);
    }

    public void resetToDefaults() {
        providerConfig.reset();
        showOriginalConfig.reset();
        outgoingEnabledConfig.reset();
        outgoingTargetLangConfig.reset();
        outgoingFormatConfig.reset();
        outgoingShowOriginalConfig.reset();
        geminiModelConfig.reset();
        geminiTimeoutConfig.reset();
        geminiMaxHistoryConfig.reset();
        deeplTimeoutConfig.reset();
        mindustryToolTimeoutConfig.reset();
        devxTimeoutConfig.reset();
        devxMaxHistoryConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new TranslationSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private @Nullable OutgoingLanguageDialog languageDialog;

    public void showLanguageDialog() {
        if (languageDialog == null) {
            languageDialog = new OutgoingLanguageDialog(this);
        }
        languageDialog.show();
    }
}
