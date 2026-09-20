package mindustrytool.features.godmode;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Events;
import arc.mock.MockApplication;
import arc.struct.Seq;
import mindustry.game.EventType.TapEvent;
import mindustry.world.Tile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapPositionPickerTest {

    private Application originalApp;
    private Seq<Runnable> postedTasks;

    @BeforeAll
    static void initCore() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
    }

    @BeforeEach
    void setUp() {
        originalApp = Core.app;
        postedTasks = new Seq<>();
        Core.app = new MockApplication() {
            @Override
            public void post(Runnable runnable) {
                postedTasks.add(runnable);
            }
        };
        MapPositionPicker.cancel();
        drainPostedTasks();
    }

    @AfterEach
    void tearDown() {
        MapPositionPicker.cancel();
        drainPostedTasks();
        Core.app = originalApp;
    }

    private void drainPostedTasks() {
        while (!postedTasks.isEmpty()) {
            postedTasks.pop().run();
        }
    }

    @Test
    void pick_invokesOnStartAndTracksListener() {
        boolean[] started = {false};
        MapPositionPicker.pick(() -> started[0] = true, (x, y) -> {});

        assertTrue(started[0]);
        assertNotNull(MapPositionPicker.currentListener);
    }

    @Test
    void cancel_clearsCurrentListenerAndSchedulesRemoval() {
        MapPositionPicker.pick(null, (x, y) -> {});
        assertNotNull(MapPositionPicker.currentListener);

        MapPositionPicker.cancel();
        assertNull(MapPositionPicker.currentListener);
        assertEquals(1, postedTasks.size);

        drainPostedTasks();
        assertNull(MapPositionPicker.currentListener);
    }

    @Test
    void pick_repeatedCallsCancelPreviousListener() {
        int[] pickedCount = {0};

        MapPositionPicker.pick(null, (x, y) -> pickedCount[0]++);
        assertEquals(0, pickedCount[0]);

        // Second pick should cancel the first
        MapPositionPicker.pick(null, (x, y) -> pickedCount[0] += 10);
        assertEquals(1, postedTasks.size); // First listener removal scheduled

        Tile tile = new Tile(10, 20);
        TapEvent event = new TapEvent(null, tile);

        Events.fire(event);

        // Only the second callback should have fired (+10)
        assertEquals(10, pickedCount[0]);
    }

    @Test
    void tapEvent_executesOnceAndDefersRemoval() {
        float[] captured = {-1f, -1f};
        int[] callCount = {0};

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
        assertEquals(1, postedTasks.size); // Removal deferred via Core.app.post

        // Second tap before drain should not fire again
        Events.fire(event);
        assertEquals(1, callCount[0]);

        // After drain, listener is permanently removed from Events
        drainPostedTasks();
        Events.fire(event);
        assertEquals(1, callCount[0]);
    }

    @Test
    void tapEvent_doesNotCorruptEventsFireIterationWithMultipleListeners() {
        int[] otherListenerCalls = {0};

        MapPositionPicker.pick(null, (x, y) -> {});

        // Add a secondary listener after MapPositionPicker
        Events.on(TapEvent.class, e -> otherListenerCalls[0]++);

        Tile tile = new Tile(1, 2);
        TapEvent event = new TapEvent(null, tile);

        // Events.fire iterates over both listeners. If removal was synchronous,
        // Arc's cached loop length would trigger a NullPointerException.
        assertDoesNotThrow(() -> Events.fire(event));

        assertEquals(1, otherListenerCalls[0]);

        drainPostedTasks();
    }
}
