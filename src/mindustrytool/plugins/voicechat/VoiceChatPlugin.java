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
                float x = player.unit().x;
                float y = player.unit().y;

                float iconSize = 5f; // Small icon to avoid distraction

                TextureRegion micIcon = Core.atlas.find("mindustry-tool-mic-on", Icon.chat.getRegion());

                // Use a softer green (Pastel)
                Draw.color(Color.valueOf("88ff88"));
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
