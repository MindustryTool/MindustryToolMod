package mindustrytool.features.godmode;

import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GodModeDialogs {

    public interface UnitSpawnConsumer {
        void accept(UnitType unit, int amount, Team team, float x, float y);
    }

    public interface CorePlaceConsumer {
        void accept(Block core, Team team, float x, float y);
    }

    public interface ItemAddConsumer {
        void accept(Item item, int amount, Team team);
    }

    public static void showTeamDialog(BiConsumer<Player, Team> onSelect) {
        new GodModePlayerSelectionDialog(onSelect).show();
    }

    static void showTeamSelectionDialog(Consumer<Team> onSelect) {
        new GodModeTeamSelectionDialog(onSelect).show();
    }

    public static void showItemDialog(ItemAddConsumer onAdd) {
        new GodModeItemSelectionDialog(onAdd).show();
    }

    public static void showUnitDialog(UnitSpawnConsumer onSpawn, BiConsumer<UnitType, Team> onKill) {
        new GodModeUnitSelectionDialog(onSpawn, onKill).show();
    }

    public static void showEffectDialog(BiConsumer<StatusEffect, Float> onApply, Consumer<StatusEffect> onClear) {
        new GodModeEffectDialog(onApply, onClear).show();
    }

    public static void showCoreDialog(CorePlaceConsumer onPlace) {
        new GodModeCoreSelectionDialog(onPlace).show();
    }
}
