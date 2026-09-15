package mindustrytool.features.playerconnect.models;

import mindustry.game.Team;
import mindustry.gen.Player;

public class JoinRequest {
    public final Player player;
    public final Team originalTeam;
    public final long timestamp;

    public JoinRequest(Player player, Team originalTeam) {
        this.player = player;
        this.originalTeam = originalTeam;
        this.timestamp = System.currentTimeMillis();
    }
}
