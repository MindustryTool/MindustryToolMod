package mindustrytool.features.griefalert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.Events;
import arc.graphics.Camera;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.I18NBundle;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.Team;
import mindustry.gen.UnitEntity;
import mindustry.input.DesktopInput;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock;
import mindustrytool.test.MindustryTestEnv;
import solim.reactive.Readable;

class GriefAlertFeatureTest extends MindustryTestEnv {

    private static final int CORE_TILE_X = 20;
    private static final int CORE_TILE_Y = 30;
    private static final int REACTOR_TILE_X = 23;
    private static final int REACTOR_TILE_Y = 33;

    private static Control createMockControl() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            return (Control) allocate.invoke(unsafe, Control.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static @Nullable CoreBlock coreBlock;
    private static @Nullable PowerGenerator reactorBlock;
    private static @Nullable ConstructBlock constructBlock;

    @BeforeAll
    static void initCore() {
        Core.camera = new Camera();
        if (Core.bundle == null) {
            Core.bundle = I18NBundle.createEmptyBundle();
        }
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }

        coreBlock = new CoreBlock("grief-alert-test-core");
        reactorBlock = new PowerGenerator("grief-alert-test-reactor");
        reactorBlock.explosionRadius = 20;
        constructBlock = new ConstructBlock(1);
    }

    @BeforeEach
    void setUp() {
        Vars.world = new World();
        Vars.state = new GameState();
        Vars.player = null;
        Vars.ui = null;
    }

    @AfterEach
    void tearDown() {
        Vars.control = null;
        Vars.player = null;
        Vars.world = null;
        Vars.state = null;
        Vars.ui = null;
    }

    private GriefAlertFeature enabledFeature() {
        GriefAlertFeature feature = new GriefAlertFeature();
        feature.enabled().set(true);
        return feature;
    }

    private static GriefAlertInfo alertAt(float worldX, float worldY) {
        return new GriefAlertInfo("griefer", "Reactor", 5, 6, worldX, worldY, CORE_TILE_X, CORE_TILE_Y);
    }

    @Test
    void raiseAlert_publishesAlertToReactiveSignal() {
        GriefAlertFeature feature = enabledFeature();
        List<GriefAlertInfo> observed = new ArrayList<>();
        feature.activeAlertSignal().subscribe(alert -> observed.add(alert));

        GriefAlertInfo alert = alertAt(120f, 240f);
        feature.raiseAlert(alert);

        assertTrue(feature.hasActiveAlert());
        assertSame(alert, feature.activeAlert());
        assertEquals(1, observed.size(), "Raising an alert must notify the banner binding");
        assertSame(alert, observed.get(0));
    }

    @Test
    void raiseAlert_newerAlertReplacesTheShowingBanner() {
        GriefAlertFeature feature = enabledFeature();

        GriefAlertInfo first = alertAt(10f, 20f);
        GriefAlertInfo second = alertAt(30f, 40f);
        feature.raiseAlert(first);
        feature.raiseAlert(second);

        assertSame(second, feature.activeAlert());
    }

    @Test
    void dismissAlert_clearsBannerWithoutMovingCamera() {
        GriefAlertFeature feature = enabledFeature();
        Core.camera.position.set(500f, 600f);
        feature.raiseAlert(alertAt(120f, 240f));

        feature.dismissAlert();

        assertFalse(feature.hasActiveAlert());
        assertNull(feature.activeAlert());
        assertEquals(500f, Core.camera.position.x, 0.001f, "Manual dismissal must not move the camera");
        assertEquals(600f, Core.camera.position.y, 0.001f, "Manual dismissal must not move the camera");
    }

    @Test
    void tick_keepsAlertWithinWindowThenAutoDismisses() {
        GriefAlertFeature feature = enabledFeature();
        feature.raiseAlert(alertAt(120f, 240f));

        feature.tick(0L);
        assertTrue(feature.hasActiveAlert(), "Alert must survive inside its display window");

        feature.tick(Long.MAX_VALUE);
        assertFalse(feature.hasActiveAlert(), "Alert must auto-close once its window elapsed");
    }

    @Test
    void raiseAlert_honorsCustomDurationConfig() {
        GriefAlertFeature feature = enabledFeature();
        feature.durationConfig.set(25f);
        feature.raiseAlert(alertAt(120f, 240f));

        feature.tick(0L);
        assertTrue(feature.hasActiveAlert(), "Alert must survive within custom duration");

        feature.tick(Long.MAX_VALUE);
        assertFalse(feature.hasActiveAlert(), "Alert must dismiss after custom duration");
    }

    @Test
    void tick_leavesNothingToDismissWhenNoAlertIsShowing() {
        GriefAlertFeature feature = enabledFeature();

        feature.tick(Long.MAX_VALUE);

        assertFalse(feature.hasActiveAlert());
    }

    @Test
    void jumpToAlert_centersCameraStartsHighlightAndDismissesBanner() {
        Vars.control = createMockControl();
        DesktopInput input = new DesktopInput();
        Vars.control.input = input;

        GriefAlertFeature feature = enabledFeature();
        Core.camera.position.set(500f, 600f);
        GriefAlertInfo alert = alertAt(120f, 240f);
        feature.raiseAlert(alert);

        feature.jumpToAlert(alert);

        assertEquals(120f, Core.camera.position.x, 0.001f);
        assertEquals(240f, Core.camera.position.y, 0.001f);
        assertTrue(input.panning, "Camera must stay panning so it does not snap back to the player");
        assertTrue(feature.hasActiveHighlight(), "Camera jump must start the world highlight marker");
        assertFalse(feature.hasActiveAlert(), "Camera jump must dismiss the banner");
    }

    @Test
    void jumpToAlert_clearsSpectatedUnitSoCameraStaysOnTarget() {
        Vars.control = createMockControl();
        DesktopInput input = new DesktopInput();
        Vars.control.input = input;
        input.spectating = new UnitEntity() {
        };

        GriefAlertFeature feature = enabledFeature();
        GriefAlertInfo alert = alertAt(120f, 240f);
        feature.raiseAlert(alert);

        feature.jumpToAlert(alert);

        assertNull(input.spectating, "Spectating must be cleared so the camera follows the jump");
    }

    @Test
    void jumpToAlert_withoutAlertLeavesCameraAndBannerUntouched() {
        Vars.control = createMockControl();
        Vars.control.input = new DesktopInput();

        GriefAlertFeature feature = enabledFeature();
        Core.camera.position.set(500f, 600f);

        feature.jumpToAlert(null);

        assertEquals(500f, Core.camera.position.x, 0.001f);
        assertEquals(600f, Core.camera.position.y, 0.001f);
        assertFalse(feature.hasActiveHighlight());
        assertFalse(feature.hasActiveAlert());
    }

    @Test
    void jumpToAlert_afterDismissalCannotStartTheHighlight() {
        Vars.control = createMockControl();
        Vars.control.input = new DesktopInput();

        GriefAlertFeature feature = enabledFeature();
        GriefAlertInfo alert = alertAt(120f, 240f);

        feature.raiseAlert(alert);
        feature.dismissAlert();
        feature.jumpToAlert(feature.activeAlert());

        assertFalse(feature.hasActiveHighlight(), "A dismissed banner can no longer be acted upon");
    }

    @Test
    void detection_raisesAlertInsteadOfPostingToChatOrToast() {
        GriefAlertFeature feature = enabledFeature();
        placeCore(CORE_TILE_X, CORE_TILE_Y);

        assertNull(Vars.ui);

        fireReactorBuild(REACTOR_TILE_X, REACTOR_TILE_Y);

        GriefAlertInfo alert = feature.activeAlert();
        assertNotNull(alert, "An explosive reactor next to a core must raise an alert");
        assertEquals(Team.sharded.coloredName(), alert.builderName);
        assertEquals(REACTOR_TILE_X, alert.tileX);
        assertEquals(REACTOR_TILE_Y, alert.tileY);
        assertEquals(CORE_TILE_X, alert.coreTileX);
        assertEquals(CORE_TILE_Y, alert.coreTileY);
        assertNull(Vars.ui, "Detection must not reach for chat fragments or HUD toasts");
    }

    @Test
    void detection_alertSignalDrivesBannerContentAndClearsOnDismiss() {
        GriefAlertFeature feature = enabledFeature();
        placeCore(CORE_TILE_X, CORE_TILE_Y);

        Readable<String> bannerContent = feature.activeAlertSignal()
                .map(alert -> alert == null ? "" : alert.builderName + " " + alert.tileX + "," + alert.tileY);

        assertEquals("", bannerContent.get(), "Banner content must start empty");

        fireReactorBuild(REACTOR_TILE_X, REACTOR_TILE_Y);

        assertTrue(bannerContent.get().contains(Team.sharded.coloredName()),
                "Detection must flow straight into the banner content");
        assertTrue(bannerContent.get().contains(String.valueOf(REACTOR_TILE_X)), "Banner must show the tile X");
        assertTrue(bannerContent.get().contains(String.valueOf(REACTOR_TILE_Y)), "Banner must show the tile Y");

        feature.dismissAlert();
        assertEquals("", bannerContent.get(), "Banner content must clear when the alert is dismissed");
    }

    @Test
    void detection_beyondAlertRadiusDoesNotRaiseAnAlert() {
        GriefAlertFeature feature = enabledFeature();
        placeCore(CORE_TILE_X, CORE_TILE_Y);

        fireReactorBuild(CORE_TILE_X + 25, CORE_TILE_Y + 25);

        assertFalse(feature.hasActiveAlert(), "Placements outside the configured radius must stay silent");
    }

    @Test
    void hudView_buildsTableWithChildrenOnlyAndSingleCell() {
        GriefAlertFeature feature = enabledFeature();
        GriefAlertHudView view = new GriefAlertHudView(feature);
        Element element = view.element();
        assertTrue(element instanceof Table, "HUD root must be a Table");
        Table root = (Table) element;
        assertTrue(root.fillParent, "Root table must fill parent");
        assertEquals(1, root.getCells().size, "Root table must hold exactly 1 banner cell");
        view.dispose();
    }

    private void placeCore(int x, int y) {
        Tile tile = new Tile(x, y);
        CoreBlock.CoreBuild core = coreBlock.new CoreBuild();
        core.tile = tile;
        tile.build = core;
        core.x = tile.worldx();
        core.y = tile.worldy();
        Vars.state.teams.get(Team.sharded).cores.add(core);
    }

    private void fireReactorBuild(int x, int y) {
        Tile tile = new Tile(x, y);
        ConstructBlock.ConstructBuild build = constructBlock.new ConstructBuild();
        build.tile = tile;
        tile.build = build;
        build.current = reactorBlock;

        Events.fire(new BlockBuildBeginEvent(tile, Team.sharded, null, false));
    }
}
