package mindustrytool.plugins.voicechat;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustrytool.plugins.playerconnect.PlayerConnectPlugin;

public class VoiceChatOverlay {
    private static final float ITEM_SIZE = 10f;
    private static final float SPACING = 2f;

    public static void init() {
        Events.run(EventType.Trigger.draw, () -> {
            VoiceChatManager manager = PlayerConnectPlugin.getVoiceChatManager();
            if (manager == null || !manager.isEnabled())
                return;

            for (Player p : Groups.player) {
                if (p == null || p.unit() == null)
                    continue;
                drawOverlay(p, manager);
            }
        });
    }

    private static void drawOverlay(Player p, VoiceChatManager manager) {
        Unit unit = p.unit();

        // Calculate position above the unit's name
        // Name is usually at unit.y + unit.hitSize + ...
        // We place icons slightly above that.
        float x = unit.x;
        float y = unit.y + unit.hitSize + 18f;

        Draw.z(110f); // Draw above names (hardcoded layer)

        boolean isMutedLocal = manager.isPlayerMuted(p.uuid());
        boolean isSpeaking = manager.isSpeaking(p.id);

        // Draw Mic Icon
        // Position: Centered

        if (isSpeaking) {
            // Speaking Effect (Ripple)
            Draw.color(Pal.accent);
            Lines.stroke(1f);
            float radius = ITEM_SIZE / 1.5f + Mathf.absin(Time.time, 5f, 3f);
            Lines.circle(x, y, radius);
            Draw.reset();

            // Icon: Green/Accent
            Draw.color(Pal.accent);
            Draw.rect(Icon.chat.getRegion(), x, y, ITEM_SIZE, ITEM_SIZE);
        } else if (isMutedLocal) {
            // Muted Locally: Red
            Draw.color(Pal.remove);
            Draw.rect(Icon.chat.getRegion(), x, y, ITEM_SIZE, ITEM_SIZE);
            Draw.rect(Icon.cancel.getRegion(), x, y, ITEM_SIZE, ITEM_SIZE);
        } else {
            // Idle: Gray/Faint
            // Only show for other players, or self if debugging?
            // User requested icons "off/on", so maybe always show.
            Draw.color(Color.gray, 0.7f);
            Draw.rect(Icon.chat.getRegion(), x, y, ITEM_SIZE, ITEM_SIZE);
        }

        Draw.reset();
    }
}
