package mindustrytool.features.callout;

import arc.Core;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.util.Nullable;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.callout.ui.CalloutDialog;
import mindustrytool.features.callout.ui.CalloutSettingsDialog;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Tactical Callout feature providing instant in-game tactical alerts and resource
 * requests with a single click.
 */
public class CalloutFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> teamChatConfig;
    public final ConfigValue<Boolean> closeOnSendConfig;

    private @Nullable CalloutDialog dialog;
    private @Nullable CalloutSettingsDialog settingsDialog;

    public CalloutFeature() {
        super(FeatureMetadata.builder()
                .id("callout")
                .icon(FileIcon.of("message-circle.png", Icon.chat))
                .order(6)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();
        teamChatConfig = config.boolValue("team-chat", true);
        closeOnSendConfig = config.boolValue("close-on-send", true);

        bindDialog("calloutMenu", KeyCode.unset, this::showDialog, true);
        bindDialog("calloutSettings", KeyCode.unset, getSettingDialog(), false);
    }

    public String formatMessage(String message, boolean toTeamChat) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (toTeamChat) {
            return trimmed.startsWith("/t ") ? trimmed : "/t " + trimmed;
        }
        return trimmed;
    }

    public void sendCallout(String message) {
        sendCallout(message, Boolean.TRUE.equals(teamChatConfig.get()));
    }

    public void sendCallout(String message, boolean toTeamChat) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String finalMessage = formatMessage(message, toTeamChat);
        if (Core.app != null) {
            Core.app.post(() -> Call.sendChatMessage(finalMessage));
        } else {
            Call.sendChatMessage(finalMessage);
        }

        if (Boolean.TRUE.equals(closeOnSendConfig.get())) {
            hideDialog();
        }
    }

    public void resetToDefaults() {
        teamChatConfig.reset();
        closeOnSendConfig.reset();
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new CalloutDialog(this);
        }
        dialog.show();
    }

    public void hideDialog() {
        if (dialog != null) {
            dialog.hide();
        }
    }

    @Override
    public void onQuickAccessClick() {
        showDialog();
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return () -> {
            if (dialog == null) {
                dialog = new CalloutDialog(this);
            }
            return dialog;
        };
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new CalloutSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
