package solim.modifier;

import solim.core.Component;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.test.SolimEnv;

class ElementConfigClickTest extends SolimEnv {

    @Test
    void onClickRunsHandlerAndEnablesTouchable() {
        Clickable c = new Clickable();
        int[] count = { 0 };
        c.onClick(() -> count[0]++);

        assertEquals(Touchable.enabled, c.element().touchable);
        fire(c);
        assertEquals(1, count[0]);
    }

    @Test
    void repeatedOnClickReplacesHandler() {
        Clickable c = new Clickable();
        boolean[] first = { false };
        boolean[] second = { false };
        c.onClick(() -> first[0] = true).onClick(() -> second[0] = true);

        assertEquals(1, clickListenerCount(c.element()), "Repeated onClick must not stack listeners");
        fire(c);
        assertFalse(first[0]);
        assertTrue(second[0]);
    }

    @Test
    void stoppedEventSkipsHandler() {
        Clickable c = new Clickable();
        boolean[] ran = { false };
        c.onClick(() -> ran[0] = true);

        InputEvent event = new InputEvent();
        event.stop();
        fire(c, event);
        assertFalse(ran[0]);
    }

    @Test
    void propagationNotStoppedByDefault() {
        Clickable c = new Clickable();
        c.onClick(() -> {
        });

        InputEvent event = new InputEvent();
        fire(c, event);
        assertFalse(event.stopped);
    }

    @Test
    void stopClickPropagationOptInStopsEvent() {
        Clickable c = new Clickable();
        boolean[] ran = { false };
        c.onClick(() -> ran[0] = true).stopClickPropagation(true);

        InputEvent event = new InputEvent();
        fire(c, event);
        assertTrue(ran[0]);
        assertTrue(event.stopped);
    }

    @Test
    void stopClickPropagationWithoutHandlerIsSafe() {
        Clickable c = new Clickable();
        assertDoesNotThrow(() -> c.stopClickPropagation(true));
    }

    private static void fire(Clickable c) {
        fire(c, new InputEvent());
    }

    private static void fire(Clickable c, InputEvent event) {
        for (EventListener listener : c.element().getListeners()) {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        }
    }

    private static int clickListenerCount(Element element) {
        int count = 0;
        for (EventListener listener : element.getListeners()) {
            if (listener instanceof ClickListener) {
                count++;
            }
        }
        return count;
    }

    private static final class Clickable extends Component
            implements Configurable<Clickable>, ElementCapability {
        private final PendingCellConfig constraints = new PendingCellConfig();
        private @Nullable Props<Clickable> pending;

        Clickable() {
            super(new Table());
        }

        public Clickable onClick(@Nullable Runnable action) {
            props(Values.onClick(action));
            return this;
        }

        public Clickable stopClickPropagation(boolean stop) {
            props(Values.stopClickPropagation(stop));
            return this;
        }

        @Override
        public PendingCellConfig cellConfig() {
            return constraints;
        }

        @Override
        public @Nullable Props<Clickable> pendingProps() {
            return pending;
        }

        @Override
        public void setPendingProps(@Nullable Props<Clickable> props) {
            this.pending = props;
        }

        public Clickable name(String name) {
            setName(name);
            return this;
        }

        public Clickable self() {
            return this;
        }
    }
}
