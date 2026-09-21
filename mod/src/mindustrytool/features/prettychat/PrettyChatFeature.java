package mindustrytool.features.prettychat;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.TextField;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import mindustrytool.utils.ReflectUtil;
import mindustry.Vars;
import mindustry.game.EventType.ClientChatEvent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.input.Binding;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.prettychat.ui.PrettyChatSettingsDialog;
import mindustrytool.features.translation.TranslationFeature;
import solim.overlay.SolimDialog;

import java.util.List;

/**
 * Pretty Chat feature: transforms outgoing chat messages with customizable
 * styles.
 */
public class PrettyChatFeature extends Feature {

    private final PrettyChatConfig config;
    private final Seq<Prettier> prettiers;
    private @Nullable PrettyChatSettingsDialog settingsDialog;
    private @Nullable String lastTransformed;
    private @Nullable Object lastHookedChatfrag;
    private boolean chatHookAttached;

    public PrettyChatFeature() {
        super(FeatureMetadata.builder()
                .id("pretty-chat")
                .icon(FileIcon.of("sparkles.png"))
                .quickAccess(true)
                .development(false)
                .enabledByDefault(false)
                .build());

        this.config = new PrettyChatConfig(configGroup());
        this.prettiers = BuiltinPrettiers.createDefaultPrettiers();

        Events.on(ClientLoadEvent.class, e -> attachChatHook());
        Events.on(ClientChatEvent.class, e -> lastTransformed = null);
        Events.run(Trigger.update, this::updateChatHook);
        attachChatHook();
    }

    public PrettyChatConfig config() {
        return config;
    }

    public Seq<Prettier> prettiers() {
        return prettiers;
    }

    public @Nullable Prettier getPrettier(String id) {
        return prettiers.find(p -> p.id().equals(id));
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new PrettyChatSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void attachChatHook() {
        if (Vars.ui == null || Vars.ui.chatfrag == null) {
            return;
        }
        if (chatHookAttached && Vars.ui.chatfrag == lastHookedChatfrag) {
            return;
        }
        if (Vars.ui.chatfrag == lastHookedChatfrag) {
            // Already attempted on this fragment instance and failed; do not spam logs
            return;
        }
        lastHookedChatfrag = Vars.ui.chatfrag;
        try {
            TextField chatfield = ReflectUtil.getOrNull(Vars.ui.chatfrag, "chatfield");
            if (chatfield != null) {
                chatfield.setOnlyFontChars(false);
                chatfield.setMaxLength(Vars.maxTextLength);
                chatfield.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, KeyCode keycode) {
                        if (keycode == KeyCode.enter) {
                            handleChatSend();
                        }
                        return false;
                    }
                });
                chatHookAttached = true;
            } else {
                Log.warn("PrettyChat: Could not find chatfield on @", Vars.ui.chatfrag.getClass().getName());
            }
        } catch (Exception e) {
            Log.err("Error attaching PrettyChat field hook: @", e.getMessage());
        }
    }

    private void updateChatHook() {
        if (!chatHookAttached) {
            attachChatHook();
        }
        if (!isEnabled() || Vars.ui == null || Vars.ui.chatfrag == null) {
            return;
        }
        if (!Vars.ui.chatfrag.shown()) {
            lastTransformed = null;
            return;
        }
        if (Core.input != null && Core.input.keyTap(Binding.chat)) {
            handleChatSend();
        }
    }

    private void handleChatSend() {
        if (!isEnabled() || Vars.ui == null || Vars.ui.chatfrag == null) {
            return;
        }
        try {
            TextField chatfield = ReflectUtil.getOrNull(Vars.ui.chatfrag, "chatfield");
            if (chatfield == null) {
                return;
            }
            String raw = chatfield.getText();
            if (raw == null || raw.trim().isEmpty()) {
                return;
            }
            String text = raw.trim();
            if (text.equals(lastTransformed)) {
                return;
            }

            // Escape prefix: //message sends message directly without transformation
            if (text.startsWith("//")) {
                String unescaped = text.substring(2);
                lastTransformed = unescaped;
                chatfield.setOnlyFontChars(false);
                chatfield.setMaxLength(Vars.maxTextLength);
                chatfield.setText(unescaped);
                return;
            }

            // If TranslationFeature will handle outgoing translation, let it translate the
            // clean text.
            // PrettyChat styling will be applied by TranslationFeature when delivering the
            // message.
            TranslationFeature translation = FeatureManager.getFeature(TranslationFeature.class);
            if (translation != null && translation.isEnabled() && translation.shouldTranslateOutgoing(text)) {
                return;
            }

            String transformed = transform(text);
            if (transformed.length() > Vars.maxTextLength) {
                transformed = clampSafe(transformed, Vars.maxTextLength);
            }
            lastTransformed = transformed;
            chatfield.setOnlyFontChars(false);
            chatfield.setMaxLength(Vars.maxTextLength);
            chatfield.setText(transformed);
        } catch (Exception e) {
            Log.err("Error in PrettyChat hook: @", e.getMessage());
        }
    }

    /**
     * Transforms an input message through the active prettier pipeline. Preserves
     * server commands except /t and /a.
     *
     * @param message text to transform
     * @return transformed text
     */
    public String transform(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String cmd = "";
        String toTransform = message;

        if (message.startsWith("/")) {
            int spaceIndex = message.indexOf(' ');
            int subIndex = spaceIndex == -1 ? message.length() : spaceIndex;
            cmd = message.substring(0, subIndex);

            // Only /t (team chat) and /a (admin chat) allow text decoration
            if (!cmd.equals("/t") && !cmd.equals("/a")) {
                return message;
            }
            toTransform = spaceIndex != -1 ? message.substring(spaceIndex).trim() : "";
            cmd = cmd + " ";
        }

        List<String> enabledIds = config.getEnabledIds();
        if (enabledIds.isEmpty()) {
            return message;
        }

        int targetMax = Math.max(0, Vars.maxTextLength - cmd.length());
        String result = toTransform;
        for (String id : enabledIds) {
            Prettier p = getPrettier(id);
            if (p != null) {
                if (p instanceof BuiltinPrettiers.RainbowPrettier) {
                    result = ((BuiltinPrettiers.RainbowPrettier) p).transform(result, targetMax);
                } else {
                    result = p.transform(result);
                }
            }
        }
        if (result.length() > targetMax) {
            result = clampSafe(result, targetMax);
        }

        return cmd + result;
    }

    /**
     * Clamps text to max length without leaving incomplete color tags.
     */
    public static String clampSafe(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "";
        }
        if (maxLength <= 0) {
            return "";
        }
        String cut = text.substring(0, maxLength);
        int lastOpenBracket = cut.lastIndexOf('[');
        int lastCloseBracket = cut.lastIndexOf(']');
        // If an open bracket has no matching close bracket in the cut string, strip it
        if (lastOpenBracket > lastCloseBracket) {
            cut = cut.substring(0, lastOpenBracket);
        }
        // If the original text had color tags, ensure color tags close cleanly
        if (cut.contains("[") && !cut.endsWith("[]")) {
            if (cut.length() + 2 > maxLength && maxLength >= 2) {
                cut = cut.substring(0, maxLength - 2);
                int open = cut.lastIndexOf('[');
                int close = cut.lastIndexOf(']');
                if (open > close) {
                    cut = cut.substring(0, open);
                }
            }
            if (cut.length() + 2 <= maxLength) {
                cut = cut + "[]";
            }
        }
        return cut;
    }
}
