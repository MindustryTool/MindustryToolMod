package mindustrytool.features.godmode;

import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;

public interface GodModeProvider {

    boolean isAvailable();

    void changeTeam(Player player, Team team);

    void addItems(Item item, int amount, Team team);

    void spawnUnits(UnitType unit, int amount, Team team, float x, float y);

    void killUnits(UnitType unit, Team team);

    void applyEffect(StatusEffect effect, float duration);

    void clearEffect(StatusEffect effect);

    boolean placeCore(Block coreBlock, Team team, float x, float y);

    void setFog(boolean enabled);
}
