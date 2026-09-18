package mindustrytool.features.wavepreview;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.SpawnGroup;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Reactive state holder for computed wave preview sections.
 */
public class WavePreviewState {
    private final Signal<Seq<WaveSectionData>> waves = Signal.of(new Seq<>());

    public Readable<Seq<WaveSectionData>> waves() {
        return waves;
    }

    public void recompute(int currentWave, int depth) {
        if (!Vars.state.isGame() || Vars.state.rules == null || !Vars.state.rules.waves) {
            waves.set(new Seq<>());
            return;
        }

        boolean isCampaign = Vars.state.isCampaign();
        float enemySpawnMultiplier = isCampaign
                && Vars.state.getPlanet() != null
                && Vars.state.getPlanet().campaignRules != null
                && Vars.state.getPlanet().campaignRules.difficulty != null
                ? Vars.state.getPlanet().campaignRules.difficulty.enemySpawnMultiplier
                : 1f;

        int clampedDepth = Math.max(WavePreviewFeature.MIN_DEPTH, Math.min(depth, WavePreviewFeature.MAX_DEPTH));
        Seq<WaveSectionData> result = new Seq<>();

        for (int i = 0; i < clampedDepth; i++) {
            int waveNum = currentWave + i;
            result.add(WavePreviewCalculator.computeWave(
                    waveNum,
                    Vars.state.rules.spawns,
                    isCampaign,
                    enemySpawnMultiplier,
                    WavePreviewState::resolveSpawnerCount));
        }

        waves.set(result);
    }

    public static int resolveSpawnerCount(SpawnGroup group) {
        if (Vars.state == null || Vars.spawner == null) {
            return 1;
        }

        Seq<Tile> spawns = Vars.spawner.getSpawns();
        if (spawns != null && spawns.size > 0) {
            if (group.spawn == -1) {
                return spawns.size;
            }
            for (int i = 0; i < spawns.size; i++) {
                if (spawns.get(i).pos() == group.spawn) {
                    return 1;
                }
            }
            return 0;
        }

        if (Vars.state.rules != null
                && Vars.state.rules.wavesSpawnAtCores
                && Vars.state.rules.attackMode
                && Vars.state.rules.waveTeam != null
                && Vars.state.teams != null
                && Vars.state.teams.isActive(Vars.state.rules.waveTeam)
                && Vars.state.teams.playerCores() != null
                && !Vars.state.teams.playerCores().isEmpty()) {
            Seq<CoreBuild> cores = Vars.state.rules.waveTeam.cores();
            if (cores != null && cores.size > 0) {
                if (group.spawn == -1) {
                    return cores.size;
                }
                for (int i = 0; i < cores.size; i++) {
                    if (cores.get(i).pos() == group.spawn) {
                        return 1;
                    }
                }
                return 0;
            }
        }

        return 0;
    }

    public void clear() {
        waves.set(new Seq<>());
    }
}
