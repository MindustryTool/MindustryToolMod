package mindustrytool.features.smartdrill;

import arc.struct.Seq;
import mindustrytool.features.smartdrill.plan.BlockSpec;
import mindustrytool.features.smartdrill.plan.DefaultDrillPlanner;
import mindustrytool.features.smartdrill.plan.DrillPlanRequest;
import mindustrytool.features.smartdrill.plan.DrillPlanResult;
import mindustrytool.features.smartdrill.plan.DrillWorldView;
import mindustrytool.features.smartdrill.plan.OreFlood;
import mindustrytool.features.smartdrill.plan.OreKind;
import mindustrytool.features.smartdrill.plan.PlacementRules;
import mindustrytool.features.smartdrill.plan.PlannedBuild;
import mindustrytool.features.smartdrill.plan.Rect;
import mindustrytool.features.smartdrill.plan.SchematicData;
import mindustrytool.features.smartdrill.support.PlanSchematics;
import mindustrytool.features.smartdrill.support.TestDrillWorld;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartDrillPlannerTest extends MindustryTestEnv {

    private static final String COPPER = "copper";
    private static final String LEAD = "lead";
    private static final String THORIUM = "thorium";

    private static final String MECHANICAL = "mechanical-drill";
    private static final String PNEUMATIC = "pneumatic-drill";
    private static final String LASER = "laser-drill";
    private static final String PLASMA = "plasma-bore";

    private static final String ITEM_BRIDGE = "item-bridge";
    private static final String DUCT = "duct";
    private static final String BEAM_NODE = "beam-node";

    private DefaultDrillPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new DefaultDrillPlanner();
    }

    @Test
    void densePatchProducesPureDrillsAndWritesPreviews() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(6, 6, 12, 12, COPPER)
                .build();

        DrillPlanResult result = plan("01-dense-2x2", world, request(COPPER, OreKind.FLOOR,
                new Rect(6, 6, 12, 12), PNEUMATIC, 0));

        assertTrue(result.drillCount > 0, "a dense patch must yield drills");
        assertInvariants(world, request(COPPER, OreKind.FLOOR, new Rect(6, 6, 12, 12), PNEUMATIC, 0), result);
    }

    @Test
    void thinVeinYieldsAFewDrills() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(5, 14, 24, 2, COPPER)
                .build();

        DrillPlanResult result = plan("02-thin-vein", world, request(COPPER, OreKind.FLOOR,
                new Rect(5, 14, 24, 2), PNEUMATIC, 0));

        assertTrue(result.drillCount > 0, "a two-tile vein must still yield drills");
        assertInvariants(world, request(COPPER, OreKind.FLOOR, new Rect(5, 14, 24, 2), PNEUMATIC, 0), result);
    }

    @Test
    void veinNarrowerThanDrillYieldsNothing() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(5, 14, 24, 1, COPPER)
                .build();

        DrillPlanRequest request = request(COPPER, OreKind.FLOOR, new Rect(5, 14, 24, 1), PNEUMATIC, 0);
        DrillPlanResult result = plan("03-vein-narrower-than-drill", world, request);

        assertEquals(0, result.drillCount,
                "a 2x2 drill footprint cannot fit inside a one-tile-tall rectangle");
    }

    @Test
    void mixedOreRejectsStraddlingFootprints() {
        TestDrillWorld world = baseWorld(40, 30).build();
        world.set(10, 10, COPPER, null, false);
        world.set(11, 10, COPPER, null, false);
        world.set(10, 11, COPPER, null, false);
        world.set(11, 11, LEAD, null, false);

        DrillPlanRequest request = request(COPPER, OreKind.FLOOR, new Rect(10, 10, 2, 2), PNEUMATIC, 0);
        DrillPlanResult result = planner.plan(request, world);

        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild build = result.builds.get(i);
            if (build.role == PlannedBuild.Role.DRILL) {
                assertFalse(build.x == 10 && build.y == 10,
                        "the footprint covering both copper and lead must be rejected");
            }
        }
        assertInvariants(world, request, result);
    }

    @Test
    void diagonalVeinFloodClampsToTenByTen() {
        TestDrillWorld world = baseWorld(40, 40).build();
        for (int i = 0; i < 20; i++) {
            world.set(5 + i, 5 + i, COPPER, null, false);
        }

        OreFlood.Result flood = OreFlood.flood(world, 5, 5, OreKind.FLOOR, COPPER,
                DrillPlanRequest.DEFAULT_MAX_TILES, DrillPlanRequest.DEFAULT_MAX_RECT_SIZE);

        assertTrue(flood.rect.width <= 10, "clamped width must not exceed 10");
        assertTrue(flood.rect.height <= 10, "clamped height must not exceed 10");
    }

    @Test
    void twoPatchesSeparatedByRockStaySeparate() {
        TestDrillWorld world = baseWorld(40, 20).build();
        for (int y = 2; y < 8; y++) {
            for (int x = 2; x < 8; x++) {
                world.set(x, y, COPPER, null, false);
            }
            for (int x = 12; x < 18; x++) {
                world.set(x, y, COPPER, null, false);
            }
        }

        OreFlood.Result flood = OreFlood.flood(world, 3, 3, OreKind.FLOOR, COPPER,
                DrillPlanRequest.DEFAULT_MAX_TILES, DrillPlanRequest.DEFAULT_MAX_RECT_SIZE);

        assertEquals(6, flood.rect.width, "the flood must stop at the rock between the patches");
        assertEquals(6, flood.rect.height, "the flood must stop at the rock between the patches");
    }

    @Test
    void latticePatternIsTranslationInvariant() {
        for (int x = -12; x < 12; x++) {
            for (int y = -12; y < 12; y++) {
                assertEquals(PlacementRules.isAnchor(x, y), PlacementRules.isAnchor(x + 12, y + 12),
                        "the lattice must repeat every 6 tiles in both axes");
            }
        }
        assertTrue(PlacementRules.isAnchor(0, 1));
        assertTrue(PlacementRules.isAnchor(-6, 1 - 6));
        assertFalse(PlacementRules.isAnchor(0, 0));
    }

    @Test
    void densestFirstPrefersHigherCoverage() {
        TestDrillWorld world = baseWorld(30, 30).build();
        for (int y = 2; y <= 5; y++) {
            for (int x = 2; x <= 5; x++) {
                world.set(x, y, COPPER, null, false);
            }
        }

        DrillPlanRequest request = request(COPPER, OreKind.FLOOR, new Rect(2, 2, 4, 4), LASER, 0);
        DrillPlanResult result = planner.plan(request, world);

        assertEquals(1, result.drillCount, "a 3x3 drill on a 4x4 patch fits exactly once");
        assertEquals(9, result.coveredOre, "the densest position must cover the whole patch");
    }

    @Test
    void beamDrillProducesUnitWithAdjacentDuct() {
        TestDrillWorld world = baseWorld(40, 20).build();
        world.wallBand(20, 0, 2, 20, THORIUM);

        DrillPlanRequest request = request(THORIUM, OreKind.WALL, new Rect(20, 0, 2, 20), PLASMA, 0);
        DrillPlanResult result = planner.plan(request, world);

        assertTrue(result.drillCount > 0, "a wall seam must yield beam drills");

        int bodies = 0;
        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild build = result.builds.get(i);
            if (build.role != PlannedBuild.Role.BEAM_DRILL) {
                continue;
            }
            bodies++;
            boolean ductAdjacent = false;
            for (int j = 0; j < result.builds.size; j++) {
                PlannedBuild other = result.builds.get(j);
                if (other.role != PlannedBuild.Role.DUCT) {
                    continue;
                }
                int manhattan = Math.abs(other.x - build.x) + Math.abs(other.y - build.y);
                if (manhattan == 1) {
                    ductAdjacent = true;
                    break;
                }
            }
            assertTrue(ductAdjacent, "the duct must be orthogonally adjacent to the body");
        }
        assertEquals(bodies, result.drillCount);
    }

    @Test
    void beamDrillRejectsLaneHittingDifferentOre() {
        TestDrillWorld world = baseWorld(40, 20).build();
        world.wallBand(20, 0, 1, 20, LEAD);

        DrillPlanRequest request = request(THORIUM, OreKind.WALL, new Rect(20, 0, 1, 20), PLASMA, 0);
        DrillPlanResult result = planner.plan(request, world);

        assertEquals(0, result.drillCount, "a lane hitting a different ore must reject the position");
    }

    @Test
    void schematicPlacesOnceAtBestPosition() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(5, 5, 12, 12, COPPER)
                .build();

        SchematicData schematic = schematicOf(new int[] {1, 1}, PNEUMATIC, new int[] {1, 2}, DUCT);

        DrillPlanRequest request = DrillPlanRequest.builder()
                .target(COPPER, OreKind.FLOOR)
                .rect(new Rect(5, 5, 12, 12))
                .facing(0)
                .drill(PNEUMATIC)
                .schematic(schematic)
                .build();

        DrillPlanResult result = planner.plan(request, world);

        assertEquals(1, result.drillCount, "a bound schematic is placed exactly once");
        assertInvariants(world, request, result);
    }

    @Test
    void schematicRejectsBlockedTileInsideRig() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(5, 5, 12, 12, COPPER)
                .build();
        world.wall(9, 9);

        SchematicData schematic = schematicOf(new int[] {1, 1}, PNEUMATIC, new int[] {1, 2}, DUCT);

        DrillPlanRequest request = DrillPlanRequest.builder()
                .target(COPPER, OreKind.FLOOR)
                .rect(new Rect(5, 5, 12, 12))
                .facing(0)
                .drill(PNEUMATIC)
                .schematic(schematic)
                .build();

        DrillPlanResult result = planner.plan(request, world);

        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild build = result.builds.get(i);
            BlockSpec spec = world.block(build.block);
            if (spec == null) {
                continue;
            }
            for (int dx = 0; dx < spec.size; dx++) {
                for (int dy = 0; dy < spec.size; dy++) {
                    int tx = build.x + dx + spec.sizeOffset;
                    int ty = build.y + dy + spec.sizeOffset;
                    assertFalse(tx == 9 && ty == 9, "no planned block may sit on the wall tile");
                }
            }
        }
    }

    @Test
    void schematicPrefersPositionCoveringMoreOre() {
        TestDrillWorld world = baseWorld(40, 40)
                .rect(5, 5, 6, 6, COPPER)
                .build();

        SchematicData schematic = schematicOf(new int[] {1, 1}, PNEUMATIC, new int[] {1, 2}, DUCT);

        DrillPlanRequest request = DrillPlanRequest.builder()
                .target(COPPER, OreKind.FLOOR)
                .rect(new Rect(2, 2, 20, 20))
                .facing(0)
                .drill(PNEUMATIC)
                .schematic(schematic)
                .build();

        DrillPlanResult result = planner.plan(request, world);

        assertEquals(4, result.coveredOre,
                "the chosen position must be fully on ore rather than straddling the patch edge");
        assertEquals(1, result.drillCount);
    }

    @Test
    void plannerIsDeterministic() {
        TestDrillWorld world = baseWorld(40, 30)
                .rect(6, 6, 12, 12, COPPER)
                .build();

        DrillPlanRequest request = request(COPPER, OreKind.FLOOR, new Rect(6, 6, 12, 12), PNEUMATIC, 0);
        DrillPlanResult first = planner.plan(request, world);
        DrillPlanResult second = planner.plan(request, world);

        assertEquals(first.builds.size, second.builds.size);
        assertEquals(first.drillCount, second.drillCount);
        assertEquals(first.coveredOre, second.coveredOre);
        for (int i = 0; i < first.builds.size; i++) {
            PlannedBuild a = first.builds.get(i);
            PlannedBuild b = second.builds.get(i);
            assertEquals(a.block, b.block);
            assertEquals(a.x, b.x);
            assertEquals(a.y, b.y);
            assertEquals(a.rotation, b.rotation);
        }
    }

    @Test
    void emptyPatchProducesNoPlan() {
        TestDrillWorld world = baseWorld(30, 30).build();

        DrillPlanRequest request = request(COPPER, OreKind.FLOOR, new Rect(5, 5, 10, 10), PNEUMATIC, 0);
        DrillPlanResult result = planner.plan(request, world);

        assertEquals(0, result.drillCount);
        assertTrue(result.builds.isEmpty());
        assertEquals(0.0f, result.efficiency());
    }

    private DrillPlanRequest request(String ore, OreKind kind, Rect rect, String drill, int facing) {
        return DrillPlanRequest.builder()
                .target(ore, kind)
                .rect(rect)
                .facing(facing)
                .drill(drill)
                .build();
    }

    private DrillPlanResult plan(String name, DrillWorldView world, DrillPlanRequest request) {
        DrillPlanResult result = planner.plan(request, world);
        assertInvariants(world, request, result);
        PlanSchematics.write((TestDrillWorld) world, result, name);
        return result;
    }

    private void assertInvariants(DrillWorldView world, DrillPlanRequest request, DrillPlanResult result) {
        boolean[] footprint = new boolean[world.width() * world.height()];
        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild build = result.builds.get(i);
            BlockSpec spec = world.block(build.block);
            if (spec == null) {
                continue;
            }
            if (build.role != PlannedBuild.Role.DRILL) {
                continue;
            }
            assertTrue(PlacementRules.footprintInRect(request.rect, spec, build.x, build.y),
                    "drill at " + build.x + "," + build.y + " must lie inside the rectangle");
            assertTrue(PlacementRules.footprintIsPureTarget(world, spec, build.x, build.y, request.targetOre),
                    "drill at " + build.x + "," + build.y + " must sit on pure target ore");

            for (int dx = 0; dx < spec.size; dx++) {
                for (int dy = 0; dy < spec.size; dy++) {
                    int tx = build.x + dx + spec.sizeOffset;
                    int ty = build.y + dy + spec.sizeOffset;
                    int index = ty * world.width() + tx;
                    assertFalse(footprint[index], "drill footprints must not overlap at " + tx + "," + ty);
                    footprint[index] = true;
                }
            }
        }
    }

    private static SchematicData schematicOf(int[] drillAt, String drillBlock, int[] extraAt,
            String extraBlock) {
        Seq<SchematicData.Tile> tiles = new Seq<>();
        tiles.add(new SchematicData.Tile(drillBlock, drillAt[0], drillAt[1], 0, null));
        tiles.add(new SchematicData.Tile(extraBlock, extraAt[0], extraAt[1], 0, null));
        return new SchematicData("test-rig", 3, 3, tiles);
    }

    private static TestDrillWorld.Builder baseWorld(int width, int height) {
        return TestDrillWorld.create(width, height)
                .fill(null)
                .register(MECHANICAL, 1, true, false, 1, 0)
                .register(PNEUMATIC, 2, true, false, 2, 0)
                .register(LASER, 3, true, false, 3, 0)
                .register(PLASMA, 1, false, true, 2, 5)
                .register(ITEM_BRIDGE, 1, false, false, 0, 0)
                .register(DUCT, 1, false, false, 0, 0)
                .register(BEAM_NODE, 1, false, false, 0, 0);
    }
}