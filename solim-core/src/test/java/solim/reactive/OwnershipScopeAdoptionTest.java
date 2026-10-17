package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import arc.scene.Element;
import solim.core.Component;
import solim.core.Disposable;
import solim.runtime.Fragment;
import solim.runtime.SignalDispatcher;
import solim.runtime.StructuralReconciler;
import solim.test.SolimCoreEnv;

class OwnershipScopeAdoptionTest extends SolimCoreEnv {


    static final class Owner extends Component {
        final List<Disposable> created = new ArrayList<>();

        @Override
        protected Element build() {
            return new Element();
        }

        @Override
        public void adopt(Disposable resource) {
            created.add(resource);
            super.adopt(resource);
        }
    }

    private static Owner factory(List<Computed<String>> built, Signal<Integer> source) {
        Owner owner = new Owner();
        built.add(Signal.computed(() -> "v" + source.get()));
        built.get(built.size() - 1).get();
        return owner;
    }

    @Test
    void itemFactoryResourcesAreOwnedByTheCreatedComponent() {
        StructuralReconciler<String, Owner> reconciler = new StructuralReconciler<>();
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();

        Map<String, Owner> first = reconciler.reconcile(Arrays.asList("A"), item -> item,
                key -> factory(built, source));

        Owner ownerA = first.get("A");
        Computed<String> derived = built.get(0);
        assertTrue(ownerA.created.contains(derived),
                "The factory-created Computed must be adopted by the component the factory returned");
        assertEquals(1, source.observerCount(), "The adopted Computed must still be subscribed while live");

        reconciler.reconcile(new ArrayList<String>(), item -> item, key -> new Owner());

        assertTrue(derived.isDisposed(), "Removing the key must dispose the factory-created Computed");
        assertEquals(0, source.observerCount(), "A disposed Computed must detach from its sources");
        reconciler.dispose();
    }

    @Test
    void retainedKeyIsNotRebuiltAndKeepsItsResources() {
        StructuralReconciler<String, Owner> reconciler = new StructuralReconciler<>();
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();

        reconciler.reconcile(Arrays.asList("A"), item -> item, key -> factory(built, source));
        Computed<String> derived = built.get(0);

        Map<String, Owner> second = reconciler.reconcile(Arrays.asList("A", "B"), item -> item,
                key -> factory(built, source));

        assertEquals(2, built.size(), "Only the new key runs its factory");
        assertFalse(derived.isDisposed(), "A retained key must keep its component and its resources");
        assertSame(second.get("A").created.get(0), derived);
        reconciler.dispose();
    }

    @Test
    void whenBranchResourcesAreDisposedWhenTheBranchRebuilds() {
        Signal<Boolean> condition = Signal.of(true);
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();
        List<Component> roots = new ArrayList<>();

        When root = When.of(condition)
                .thenDo(() -> {
                    roots.add(new Owner());
                    built.add(evaluated(source, "then"));
                })
                .elseDo(() -> {
                    roots.add(new Owner());
                    built.add(evaluated(source, "else"));
                });
        root.element();

        Computed<String> thenBranch = built.get(0);
        assertFalse(thenBranch.isDisposed());
        assertEquals(1, source.observerCount(), "The live branch's Computed must stay subscribed");

        condition.set(false);
        SignalDispatcher.flush();

        assertTrue(built.get(0).isDisposed(), "Switching branches must dispose the old branch's resources");
        assertTrue(roots.get(0).isDisposed(), "Switching branches must dispose the old branch's component");
        Computed<String> elseBranch = built.get(1);
        assertFalse(elseBranch.isDisposed());
        assertEquals(1, source.observerCount(), "Exactly the live branch stays subscribed");

        root.dispose();
        assertTrue(elseBranch.isDisposed());
        assertEquals(0, source.observerCount());
    }

    @Test
    void whenBranchWithNoComponentsCollapsesAndReleasesItsResources() {
        Signal<Boolean> condition = Signal.of(true);
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();

        When root = When.of(condition).thenDo(() -> built.add(evaluated(source, "then")));
        root.element();

        assertEquals(0, root.container().getChildren().size, "A branch with no components collapses");
        assertTrue(built.get(0).isDisposed(),
                "A collapsed branch must not leave the resources it created alive");
        assertEquals(0, source.observerCount());

        root.dispose();
    }

    @Test
    void factoryFailureDisposesWhatItCreatedAndPreservesActiveComponents() {
        StructuralReconciler<String, Owner> reconciler = new StructuralReconciler<>();
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();

        Map<String, Owner> initial = reconciler.reconcile(Arrays.asList("A"), item -> item,
                key -> factory(built, source));
        Owner kept = initial.get("A");

        assertThrows(IllegalStateException.class,
                () -> reconciler.reconcile(Arrays.asList("A", "B", "C"), item -> item, key -> {
                    built.add(evaluated(source, key));
                    if ("C".equals(key)) {
                        throw new IllegalStateException("factory failed");
                    }
                    return new Owner();
                }));

        assertFalse(kept.isDisposed(), "Previously active components must survive a failed reconcile");
        assertTrue(built.get(2).isDisposed(),
                "A resource created before the factory threw must be destroyed, not leaked");
        assertEquals(1, source.observerCount(), "Only the surviving component's Computed stays subscribed");

        reconciler.dispose();
        assertEquals(0, source.observerCount());
    }

    @Test
    void effectBodyThatCreatesAComponentThenThrowsDestroysIt() {
        Signal<Integer> trigger = Signal.of(0);
        List<Component> created = new ArrayList<>();

        Effect.of(() -> {
            if (trigger.get() == 0) {
                return;
            }

            created.add(new Owner());
            created.get(created.size() - 1).element();
            throw new IllegalStateException("body failed after creating a component");
        });

        trigger.set(1);
        assertThrows(IllegalStateException.class, SignalDispatcher::flush);

        assertEquals(1, created.size());
        assertTrue(created.get(0).isDisposed(),
                "An effect body that throws must not leave the components it created alive");
    }

    @Test
    void effectOwnsResourcesItsBodyCreatesUntilItIsDisposed() {
        Signal<Integer> trigger = Signal.of(0);
        List<Computed<String>> perRun = new ArrayList<>();

        Effect effect = Effect.of(() -> {
            trigger.get();
            perRun.add(evaluated(trigger, "run"));
        });
        Computed<String> firstRun = perRun.get(0);

        trigger.set(1);
        SignalDispatcher.flush();
        Computed<String> secondRun = perRun.get(1);

        assertFalse(firstRun.isDisposed(),
                "A re-running effect keeps what it created: the resources may be live UI");
        assertFalse(secondRun.isDisposed());

        effect.dispose();
        assertTrue(firstRun.isDisposed(), "Disposing the effect disposes what its runs created");
        assertTrue(secondRun.isDisposed());
    }

    @Test
    void fragmentAdoptsNonComponentResourcesFromItsBlock() {
        Signal<Integer> source = Signal.of(1);
        List<Computed<String>> built = new ArrayList<>();

        Fragment fragment = Fragment.capture(() -> built.add(evaluated(source, "branch")));
        assertEquals(1, built.size());
        assertFalse(built.get(0).isDisposed());

        fragment.dispose();

        assertTrue(built.get(0).isDisposed(),
                "A fragment owns the non-component resources its block created");
        assertEquals(0, source.observerCount());
    }

    private static Computed<String> evaluated(Signal<Integer> source, String tag) {
        Computed<String> computed = Signal.computed(() -> tag + source.get());
        computed.get();
        return computed;
    }
}
