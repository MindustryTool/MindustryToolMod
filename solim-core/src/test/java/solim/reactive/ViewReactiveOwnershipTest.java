package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.test.SolimCoreEnv;

class ViewReactiveOwnershipTest extends SolimCoreEnv {

    static final class View extends Component {
        final Signal<String> source = Signal.of("a");
        Computed<String> derived;

        @Override
        protected Element build() {
            derived = Signal.computed(() -> source.get().toUpperCase());
            derived.get();
            return new Element();
        }
    }

    @Test
    void viewDerivedValueIsDisposedWithTheViewNotWithTheEnclosingComponent() {
        View[] built = new View[1];

        Component dialog = new Component() {
            @Override
            protected Element build() {
                View view = new View();
                view.element();
                built[0] = view;
                return new Element();
            }
        };

        dialog.element();
        Computed<String> derived = built[0].derived;
        assertFalse(derived.isDisposed());
        assertEquals(1, built[0].source.observerCount());

        built[0].dispose();
        assertTrue(derived.isDisposed(), "A view's derived values die with the view");
        assertEquals(0, built[0].source.observerCount(), "A disposed Computed detaches from its sources");

        assertFalse(dialog.isDisposed(), "The enclosing component is untouched by the view's disposal");
        dialog.dispose();
    }

    @Test
    void repeatedlyOpeningAViewDoesNotAccumulateObserversOnSessionSignals() {
        Signal<String> sessionSignal = Signal.of("s");

        for (int generation = 0; generation < 5; generation++) {
            Component dialog = new Component() {
                @Override
                protected Element build() {

                    Computed<String> derived = Signal.computed(() -> "gen" + sessionSignal.get());
                    derived.get();
                    return new Element();
                }
            };
            dialog.element();
            assertEquals(1, sessionSignal.observerCount(),
                    "Each open adds exactly one observer, never more");
            dialog.dispose();
            assertEquals(0, sessionSignal.observerCount(),
                    "Closing the dialog must remove its observer; nothing accumulates across generations");
        }
    }
}
