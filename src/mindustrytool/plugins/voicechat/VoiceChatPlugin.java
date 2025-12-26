package mindustrytool.plugins.voicechat;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
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

            Log.info("[VoiceChat] Plugin initialized (uses shared manager from PlayerConnectPlugin).");
        } catch (Throwable e) {
            Log.err("[VoiceChat] Failed to initialize: " + e.getMessage());
        }
    }

    private void drawWorld() {
        VoiceChatManager voiceManager = getManager();
        if (voiceManager == null || !voiceManager.isEnabled())
            return;

        for (mindustry.gen.Player player : mindustry.gen.Groups.player) {
            if (player.unit() == null)
                continue;

            boolean isSpeaking = voiceManager.isSpeaking(player.uuid());
            boolean isSelf = (player == Vars.player);

            if (isSelf) {
                isSpeaking = voiceManager.isSelfSpeaking();
            }

            // Only show if speaking
            if (isSpeaking) {
                float iconSize = 8f; // Increased size for visibility
                float x = player.unit().x;
                float y = player.unit().y + player.unit().hitSize + 4f; // Position above unit

                // Ensure it renders on top of units
                Draw.z(Layer.overlayUI);

                TextureRegion micIcon = Core.atlas.find("mindustry-tool-mic-on");
                if (Core.atlas.isFound(micIcon)) {
                    Draw.color(Color.white); // Normal color
                } else {
                    // Fallback: Red Chat Icon if sprite not found
                    micIcon = Icon.chat.getRegion();
                    Draw.color(Color.red);
                }

                Draw.rect(micIcon, x, y, iconSize, iconSize);
                Draw.reset();
            }
        }
    }

    @Override
    public void dispose() {
        // No need to dispose - PlayerConnectPlugin manages the shared instance
    }
}
