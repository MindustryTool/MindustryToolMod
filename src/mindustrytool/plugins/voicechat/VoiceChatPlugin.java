package mindustrytool.plugins.voicechat;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustrytool.Plugin;
import mindustrytool.plugins.playerconnect.PlayerConnectPlugin;
import mindustry.graphics.Layer;

public class VoiceChatPlugin implements Plugin {

    // Use shared instance from PlayerConnectPlugin instead of creating our own

    // UI is now handled by QuickAccessPlugin, so we removed the direct button
    // injection in Player List

    // Track settings dialog for toggle behavior
    private VoiceChatSettingsDialog settingsDialog;

    /** Get VoiceChatManager from shared LazyComponent in PlayerConnectPlugin */
    private VoiceChatManager getManager() {
        return PlayerConnectPlugin.getVoiceChatManager();
    }

    @Override
    public String getName() {
        return "VoiceChat";
    }

    @Override
    public void init() {
        // Voice Chat now supports both Desktop and Android (via VoiceChatCompanion app)
        try {
            Events.run(EventType.Trigger.draw, this::drawWorld);

            // Keybind listener for Voice Chat Settings (Toggle)
            Events.run(EventType.Trigger.update, () -> {
                if (Core.input.keyTap(mindustrytool.plugins.browser.ModKeybinds.voiceChatSettings)) {
                    VoiceChatManager manager = getManager();
                    if (manager != null) {
                        // Toggle: if dialog is showing, hide it; otherwise show new one
                        if (settingsDialog != null && Core.scene.root.getChildren().contains(settingsDialog, true)) {
                            settingsDialog.hide();
                            settingsDialog = null;
                        } else {
                            settingsDialog = new VoiceChatSettingsDialog(manager);
                            settingsDialog.show();
                        }
                    }
                }
            });

            Log.info("[VoiceChat] Plugin initialized (uses shared manager from PlayerConnectPlugin).");
        } catch (Throwable e) {
            Log.err("[VoiceChat] Failed to initialize: " + e.getMessage());
        }
    }

    private void drawWorld() {
        VoiceChatManager voiceManager = getManager();
        if (voiceManager == null || !voiceManager.isEnabled())
            return;

        // Draw HUD indicator for self (Bottom-Right corner)
        if (voiceManager.isSelfSpeaking()) {
            Draw.z(Layer.max); // Topmost layer
            float hudSize = 32f;
            float hudX = Core.graphics.getWidth() - hudSize - 20f;
            float hudY = hudSize + 20f;

            TextureRegion hudIcon = Core.atlas.find("mindustry-tool-mic-on");
            if (!Core.atlas.isFound(hudIcon))
                hudIcon = Core.atlas.find("mic-on"); // Try without prefix

            if (Core.atlas.isFound(hudIcon)) {
                Draw.color(Color.white);
                Draw.rect(hudIcon, hudX, hudY, hudSize, hudSize);
            } else {
                Draw.color(Color.green);
                mindustry.graphics.Drawf.tri(hudX, hudY, hudSize / 2, hudSize / 2, 0); // Triangle fallback
            }
            Draw.reset();
        }

        // Draw World indicators for players
        for (mindustry.gen.Player player : mindustry.gen.Groups.player) {
            if (player.unit() == null)
                continue;

            boolean isSpeaking = voiceManager.isSpeaking(String.valueOf(player.id));
            boolean isSelf = (player == Vars.player);

            if (isSelf) {
                isSpeaking = voiceManager.isSelfSpeaking();
            }

            // Only show if speaking
            if (isSpeaking) {
                float iconSize = 8f; // Increased size for visibility
                float x = player.unit().x;
                float y = player.unit().y + player.unit().hitSize + 8f; // Higher position

                // Ensure it renders on top of units
                Draw.z(Layer.overlayUI);

                TextureRegion micIcon = Core.atlas.find("mindustry-tool-mic-on");
                if (!Core.atlas.isFound(micIcon))
                    micIcon = Core.atlas.find("mic-on"); // Try without prefix

                if (Core.atlas.isFound(micIcon)) {
                    Draw.color(Color.white); // Normal color
                    Draw.rect(micIcon, x, y, iconSize, iconSize);
                } else {
                    // Fallback: Green Circle if sprite not found (Better than red chat icon)
                    Draw.color(Color.green);
                    arc.graphics.g2d.Fill.circle(x, y, iconSize / 2);
                }

                Draw.reset();
            }
        }
    }

    @Override
    public void dispose() {
        // No need to dispose - PlayerConnectPlugin manages the shared instance
    }
}
