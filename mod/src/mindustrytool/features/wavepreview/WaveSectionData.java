package mindustrytool.features.wavepreview;

import arc.struct.Seq;

public class WaveSectionData {
    public final int waveNumber;
    public final Seq<WaveUnitEntry> ground;
    public final Seq<WaveUnitEntry> air;
    public final Seq<WaveUnitEntry> naval;
    public final float totalHealth;

    public WaveSectionData(
            int waveNumber,
            Seq<WaveUnitEntry> ground,
            Seq<WaveUnitEntry> air,
            Seq<WaveUnitEntry> naval) {
        this(waveNumber, ground, air, naval, 0f);
    }

    public WaveSectionData(
            int waveNumber,
            Seq<WaveUnitEntry> ground,
            Seq<WaveUnitEntry> air,
            Seq<WaveUnitEntry> naval,
            float totalHealth) {
        this.waveNumber = waveNumber;
        this.ground = ground;
        this.air = air;
        this.naval = naval;
        this.totalHealth = totalHealth;
    }

    public boolean isEmpty() {
        return ground.isEmpty() && air.isEmpty() && naval.isEmpty();
    }
}
