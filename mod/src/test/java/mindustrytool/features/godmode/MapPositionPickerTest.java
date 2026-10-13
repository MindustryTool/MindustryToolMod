package mindustrytool.features.godmode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Application;
import arc.Core;
import arc.Events;
import arc.mock.MockApplication;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;
import mindustry.core.UI;
import mindustry.world.Tile;
import solim.test.ArcTestEnv;

class MapPositionPickerTest extends ArcTestEnv {

    private Application originalApp;
    private UI originalUi;
    private Seq<Runnable> postedTasks;

    @BeforeEach
    void setUp() {
        originalApp = Core.app;
        originalUi = Vars.ui;
        postedTasks = new Seq<>();
        Core.app = new MockApplication() {
            @Override
            public void post(Runnable runnable) {
                if (runnable instanceof MapPositionPicker.RemoveListenerTask) {
                    synchronized (postedTasks) {
                        postedTasks.add(runnable);
                    }
                } else {
                    runnable.run();
                }
            }
        };

        Vars.ui = null;
        MapPositionPicker.cancel();
        drainPostedTasks();
    }

    @AfterEach
    void tearDown() {
        MapPositionPicker.cancel();
        drainPostedTasks();
        Core.app = originalApp;
        Vars.ui = originalUi;
    }

    private void drainPostedTasks() {
        synchronized (postedTasks) {
            while (!postedTasks.isEmpty()) {
                postedTasks.pop().run();
            }
        }
    }

    @Test
    void pick_invokesOnStartAndTracksListener() {
        boolean[] started = { false };
        MapPositionPicker.pick(() -> started[0] = true, (x, y) -> {
        });

        assertTrue(started[0]);
        assertNotNull(MapPositionPicker.currentListener);
    }

    @Test
    void cancel_clearsCurrentListenerAndSchedulesRemoval() {
        MapPositionPicker.pick(null, (x, y) -> {
        });
        assertNotNull(MapPositionPicker.currentListener);

        MapPositionPicker.cancel();
        assertNull(MapPositionPicker.currentListener);
        assertEquals(1, postedTasks.size);

        drainPostedTasks();
        assertNull(MapPositionPicker.currentListener);
    }

    @Test
    void pick_repeatedCallsCancelPreviousListener() {
        int[] pickedCount = { 0 };

        MapPositionPicker.pick(null, (x, y) -> pickedCount[0]++);
        assertEquals(0, pickedCount[0]);

        MapPositionPicker.pick(null, (x, y) -> pickedCount[0] += 10);
        assertEquals(1, postedTasks.size);

        Tile tile = new Tile(10, 20);
        TapEvent event = new TapEvent(null, tile);

        Events.fire(event);

        assertEquals(10, pickedCount[0]);
    }

    @Test
    void tapEvent_executesOnceAndDefersRemoval() {
        float[] captured = { -1f, -1f };
        int[] callCount = { 0 };

        MapPositionPicker.pick(null, (x, y) -> {
            captured[0] = x;
            captured[1] = y;
            callCount[0]++;
        });

        Tile tile = new Tile(5, 8);
        TapEvent event = new TapEvent(null, tile);

        assertDoesNotThrow(() -> Events.fire(event));

        assertEquals(1, callCount[0]);
        assertEquals(tile.worldx(), captured[0]);
        assertEquals(tile.worldy(), captured[1]);
        assertNull(MapPositionPicker.currentListener);
        assertEquals(1, postedTasks.size);

        Events.fire(event);
        assertEquals(1, callCount[0]);

        drainPostedTasks();
        Events.fire(event);
        assertEquals(1, callCount[0]);
    }

    @Test
    void tapEvent_doesNotCorruptEventsFireIterationWithMultipleListeners() {
        int[] otherListenerCalls = { 0 };

        MapPositionPicker.pick(null, (x, y) -> {
        });

        Events.on(TapEvent.class, e -> otherListenerCalls[0]++);

        Tile tile = new Tile(1, 2);
        TapEvent event = new TapEvent(null, tile);

        assertDoesNotThrow(() -> Events.fire(event));

        assertEquals(1, otherListenerCalls[0]);

        drainPostedTasks();
    }
}
