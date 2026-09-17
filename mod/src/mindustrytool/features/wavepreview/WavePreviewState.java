package mindustrytool.features.wavepreview;

import arc.struct.Seq;
import mindustry.Vars;
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
                    enemySpawnMultiplier));
        }

        waves.set(result);
    }

    public void clear() {
        waves.set(new Seq<>());
    }
}
