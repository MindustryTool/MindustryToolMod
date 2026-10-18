package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class OwnershipGuardTest extends SolimEnv {

    private static final class Host extends Component {
    }

    @Test
    void aResourceHeldByALiveOwnerIsNotSilentlyMoved() {
        resetAmbientState();
        Host owner = new Host();
        Host thief = new Host();
        DisposableAction resource = DisposableAction.of(() -> {
        });

        owner.adopt(resource);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> thief.adopt(resource));
        assertTrue(thrown.getMessage().contains("Host"),
                "The message must name the owner that already holds the resource");
        assertTrue(thrown.getMessage().contains("DisposableAction"),
                "The message must name the resource");
    }

    @Test
    void aDisposedOwnerNoLongerHoldsItsResources() {
        Host dead = new Host();
        Host fresh = new Host();
        DisposableAction resource = DisposableAction.of(() -> {
        });

        dead.adopt(resource);
        dead.dispose();

        fresh.adopt(resource);
        assertEquals(1, fresh.disposables().size(), "A disposed owner releases its resources");
    }

    @Test
    void aComponentChildMayBeReparented() {
        Host first = new Host();
        Host second = new Host();
        Host child = new Host();

        first.adopt(child);
        second.adopt(child);

        assertEquals(1, second.children().size());
        assertEquals(0, first.children().size());
    }

    @Test
    void theSameOwnerMayAdoptTwiceWithoutConflict() {
        Host owner = new Host();
        DisposableAction resource = DisposableAction.of(() -> {
        });

        owner.adopt(resource);
        owner.adopt(resource);

        assertEquals(1, owner.disposables().size());
    }

    @Test
    void sessionRootResourcesAreVisibleToTheWalk() {
        DisposableAction resource = DisposableAction.of(() -> {
        });
        SessionRoot.get().adopt(resource);

        Host claimant = new Host();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> claimant.adopt(resource));
        assertTrue(thrown.getMessage().contains("SessionRoot"));

        SessionRoot.get().clear();
    }
}
