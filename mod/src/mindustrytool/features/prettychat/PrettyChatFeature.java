package mindustrytool.features.prettychat;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.scene.ui.TextField;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.input.Binding;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.prettychat.ui.PrettyChatSettingsDialog;

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

    public PrettyChatFeature() {
        super(FeatureMetadata.builder()
                .id("pretty-chat")
                .icon(FileIcon.of("sparkles.png"))
                .development(false)
                .enabledByDefault(false)
                .build());

        this.config = new PrettyChatConfig(configGroup());
        this.prettiers = BuiltinPrettiers.createDefaultPrettiers();

        Events.run(Trigger.update, this::updateChatHook);
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

            // Escape prefix: //message sends /message directly without transformation
            if (text.startsWith("//")) {
                chatfield.setText(text.substring(1));
                return;
            }

            String transformed = transform(text);
            if (transformed.length() > Vars.maxTextLength) {
                transformed = clampSafe(transformed, Vars.maxTextLength);
            }
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

        String result = toTransform;
        for (String id : enabledIds) {
            Prettier p = getPrettier(id);
            if (p != null) {
                result = p.transform(result);
            }
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
        String cut = text.substring(0, maxLength);
        int lastOpenBracket = cut.lastIndexOf('[');
        int lastCloseBracket = cut.lastIndexOf(']');
        // If an open bracket has no matching close bracket in the cut string, strip it
        if (lastOpenBracket > lastCloseBracket) {
            cut = cut.substring(0, lastOpenBracket);
        }
        // If the original text had color tags, ensure color tags close cleanly
        if (cut.contains("[") && !cut.endsWith("[]")) {
            if (cut.length() + 2 <= maxLength) {
                cut = cut + "[]";
            }
        }
        return cut;
    }
}
