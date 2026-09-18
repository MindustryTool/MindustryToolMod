package mindustrytool.features.wavepreview;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.core.ContentLoader;
import mindustry.game.SpawnGroup;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WavePreviewCalculatorTest {

    private static UnitType groundDagger;
    private static UnitType airFlare;
    private static UnitType navalRisso;
    private static UnitType weakGround;

    @BeforeAll
    static void init() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        if (StatusEffects.boss == null) {
            StatusEffects.boss = new StatusEffect("boss");
        }

        groundDagger = new UnitType("wave-test-dagger");
        groundDagger.flying = false;
        groundDagger.naval = false;
        groundDagger.health = 150f;

        airFlare = new UnitType("wave-test-flare");
        airFlare.flying = true;
        airFlare.naval = false;
        airFlare.health = 75f;

        navalRisso = new UnitType("wave-test-risso");
        navalRisso.flying = false;
        navalRisso.naval = true;
        navalRisso.health = 280f;

        weakGround = new UnitType("wave-test-crawler");
        weakGround.flying = false;
        weakGround.naval = false;
        weakGround.health = 50f;
    }

    private SpawnGroup createGroup(UnitType type, int unitAmount, int begin, int end, int spacing, int spawn) {
        SpawnGroup group = new SpawnGroup(type);
        group.unitAmount = unitAmount;
        group.begin = begin;
        group.end = end;
        group.spacing = spacing;
        group.spawn = spawn;
        return group;
    }

    @Test
    void testSingleSpawnerScaling() {
        SpawnGroup group = createGroup(groundDagger, 4, 0, 10, 1, -1);
        Seq<SpawnGroup> spawns = Seq.with(group);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f,
                g -> 1);

        assertFalse(section.isEmpty());
        assertEquals(1, section.ground.size);
        assertEquals(4, section.ground.first().amount);
        assertEquals(groundDagger, section.ground.first().type);
    }

    @Test
    void testMultipleSpawnersScaling() {
        SpawnGroup group = createGroup(groundDagger, 4, 0, 10, 1, -1);
        Seq<SpawnGroup> spawns = Seq.with(group);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f,
                g -> 3);

        assertFalse(section.isEmpty());
        assertEquals(1, section.ground.size);
        assertEquals(12, section.ground.first().amount);
    }

    @Test
    void testTargetedSpawnerMatching() {
        int matchingPos = 12345;
        int nonMatchingPos = 99999;

        SpawnGroup matchedGroup = createGroup(groundDagger, 5, 0, 10, 1, matchingPos);
        SpawnGroup unmatchedGroup = createGroup(airFlare, 3, 0, 10, 1, nonMatchingPos);
        Seq<SpawnGroup> spawns = Seq.with(matchedGroup, unmatchedGroup);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f,
                g -> g.spawn == matchingPos ? 1 : 0);

        assertFalse(section.isEmpty());
        assertEquals(1, section.ground.size);
        assertEquals(5, section.ground.first().amount);
        assertTrue(section.air.isEmpty());
    }

    @Test
    void testZeroSpawnersYieldsEmptyWave() {
        SpawnGroup group = createGroup(groundDagger, 10, 0, 10, 1, -1);
        Seq<SpawnGroup> spawns = Seq.with(group);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f,
                g -> 0);

        assertTrue(section.isEmpty());
        assertEquals(0, section.ground.size);
        assertEquals(0, section.air.size);
        assertEquals(0, section.naval.size);
    }

    @Test
    void testCampaignScalingWithSpawners() {
        SpawnGroup normalGroup = createGroup(groundDagger, 3, 0, 10, 1, -1);
        SpawnGroup bossGroup = createGroup(airFlare, 3, 0, 10, 1, -1);
        bossGroup.effect = StatusEffects.boss;

        Seq<SpawnGroup> spawns = Seq.with(normalGroup, bossGroup);

        // 3 * 1.5 = 4.5 -> normal rounded to 5; boss truncated to 4
        // With 2 spawners: normal = 5 * 2 = 10; boss = 4 * 2 = 8
        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                true,
                1.5f,
                g -> 2);

        assertFalse(section.isEmpty());
        assertEquals(10, section.ground.first().amount);
        assertEquals(8, section.air.first().amount);
    }

    @Test
    void testDomainGroupingAndHealthSorting() {
        SpawnGroup daggers = createGroup(groundDagger, 2, 0, 10, 1, -1);
        SpawnGroup crawlers = createGroup(weakGround, 6, 0, 10, 1, -1);
        SpawnGroup flares = createGroup(airFlare, 4, 0, 10, 1, -1);
        SpawnGroup rissos = createGroup(navalRisso, 1, 0, 10, 1, -1);

        Seq<SpawnGroup> spawns = Seq.with(daggers, crawlers, flares, rissos);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f,
                g -> 1);

        assertEquals(2, section.ground.size);
        assertEquals(weakGround, section.ground.get(0).type);
        assertEquals(groundDagger, section.ground.get(1).type);
        assertEquals(1, section.air.size);
        assertEquals(1, section.naval.size);
    }

    @Test
    void testDefaultOverloadUsesSingleSpawner() {
        SpawnGroup group = createGroup(groundDagger, 7, 0, 10, 1, -1);
        Seq<SpawnGroup> spawns = Seq.with(group);

        WaveSectionData section = WavePreviewCalculator.computeWave(
                1,
                spawns,
                false,
                1f);

        assertFalse(section.isEmpty());
        assertEquals(7, section.ground.first().amount);
    }
}
