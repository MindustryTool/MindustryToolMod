package mindustrytool.features.wavepreview;

import arc.math.Mathf;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.content.StatusEffects;
import mindustry.game.SpawnGroup;
import mindustry.type.UnitType;

/**
 * Computes upcoming-wave unit compositions matching legacy parity with domain grouping and sorting.
 */
public final class WavePreviewCalculator {

    private WavePreviewCalculator() {}

    public static WaveSectionData computeWave(
            int waveNumber,
            @Nullable Iterable<SpawnGroup> spawns,
            boolean isCampaign,
            float enemySpawnMultiplier) {
        ObjectIntMap<UnitType> counts = new ObjectIntMap<>();

        if (spawns != null) {
            for (SpawnGroup group : spawns) {
                if (group.type == null) {
                    continue;
                }

                int amount = group.getSpawned(waveNumber - 1);
                if (amount == 0) {
                    continue;
                }

                if (isCampaign) {
                    amount = Math.max(1, group.effect == StatusEffects.boss
                            ? (int) (amount * enemySpawnMultiplier)
                            : Mathf.round(amount * enemySpawnMultiplier));
                }

                if (amount > 0) {
                    counts.put(group.type, counts.get(group.type, 0) + amount);
                }
            }
        }

        Seq<WaveUnitEntry> ground = new Seq<>();
        Seq<WaveUnitEntry> air = new Seq<>();
        Seq<WaveUnitEntry> naval = new Seq<>();

        for (UnitType type : counts.keys()) {
            WaveUnitEntry entry = new WaveUnitEntry(type, counts.get(type));
            UnitDomain domain = UnitDomain.of(type);
            switch (domain) {
                case AIR:
                    air.add(entry);
                    break;
                case NAVAL:
                    naval.add(entry);
                    break;
                case GROUND:
                default:
                    ground.add(entry);
                    break;
            }
        }

        ground.sort(e -> e.type.health);
        air.sort(e -> e.type.health);
        naval.sort(e -> e.type.health);

        return new WaveSectionData(waveNumber, ground, air, naval);
    }
}
