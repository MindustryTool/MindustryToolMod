package mindustrytool.features.wavepreview;

import arc.struct.Seq;

/**
 * Composition data for a single wave, grouped by domain and sorted by unit health.
 */
public class WaveSectionData {
    public final int waveNumber;
    public final Seq<WaveUnitEntry> ground;
    public final Seq<WaveUnitEntry> air;
    public final Seq<WaveUnitEntry> naval;

    public WaveSectionData(
            int waveNumber,
            Seq<WaveUnitEntry> ground,
            Seq<WaveUnitEntry> air,
            Seq<WaveUnitEntry> naval) {
        this.waveNumber = waveNumber;
        this.ground = ground;
        this.air = air;
        this.naval = naval;
    }

    public boolean isEmpty() {
        return ground.isEmpty() && air.isEmpty() && naval.isEmpty();
    }
}
