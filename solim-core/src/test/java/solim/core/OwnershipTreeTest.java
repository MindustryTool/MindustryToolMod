package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Dynamic;
import solim.reactive.Signal;
import solim.test.SolimCoreEnv;

class OwnershipTreeTest extends SolimCoreEnv {

    static class Probe extends Component {
        final String id;
        final List<String> log;
        RuntimeException failOnDispose;

        Probe(String id, List<String> log) {
            this.id = id;
            this.log = log;
        }

        @Override
        protected Element build() {
            return new Element();
        }

        @Override
        public void dispose() {
            super.dispose();
            log.add(id);
            if (failOnDispose != null) {
                throw failOnDispose;
            }
        }
    }

    @Test
    void adoptedComponentsBecomeChildrenWithParentLinks() {
        List<String> log = new ArrayList<>();
        Probe[] children = new Probe[2];
        Component[] outerRef = new Component[1];

        Component outer = new Component() {
            @Override
            protected Element build() {
                outerRef[0] = this;
                children[0] = new Probe("child-0", log);
                children[0].element();
                children[1] = new Probe("child-1", log);
                children[1].element();
                return new Element();
            }
        };

        outer.element();

        assertSame(outer, children[0].owner(),
                "A component built inside another must record that component as its owner");
        assertEquals(Arrays.asList(children[0], children[1]), outer.children());
        assertTrue(outer.isAncestorOf(children[0]));
        assertTrue(outer.isAncestorOf(children[1]));
        assertFalse(children[0].isAncestorOf(outer), "Ancestry does not run backwards");

        outer.dispose();
        assertNull(children[0].owner(), "A disposed node detaches from its owner");
        assertEquals(0, outer.children().size(), "The parent's child list is cleared once disposal is done");
        assertTrue(children[0].isDisposed());
        assertTrue(children[1].isDisposed());
    }

    @Test
    void nonComponentResourcesStayLeavesAndDoNotBecomeChildren() {
        Computed<String> derived = Signal.computed(() -> "v");
        derived.get();
        Component[] ownerRef = new Component[1];

        Component owner = new Component() {
            @Override
            protected Element build() {
                adopt(derived);
                ownerRef[0] = this;
                return new Element();
            }
        };

        owner.element();

        assertTrue(owner.children().isEmpty(),
                "A Computed is a leaf resource, never a child node");

        owner.dispose();
        assertTrue(derived.isDisposed(), "Own leaves dispose with their owner");
    }

    @Test
    void childrenDisposeBeforeOwnResources() {
        List<String> log = new ArrayList<>();

        Component owner = new Component() {
            @Override
            protected Element build() {
                own(DisposableAction.of(() -> log.add("own-leaf-1")));
                own(DisposableAction.of(() -> log.add("own-leaf-2")));

                Component child = new Component() {
                    @Override
                    protected Element build() {
                        return new Element();
                    }

                    @Override
                    protected void onDispose() {
                        log.add("child");
                    }
                };
                child.element();
                return new Element();
            }
        };

        owner.element();
        owner.dispose();

        assertEquals(Arrays.asList("child", "own-leaf-2", "own-leaf-1"), log,
                "onDispose of a child runs in the children phase, before the owner's own leaves");
    }

    @Test
    void aReactiveLeafCannotRecomputeAgainstADisposedChild() {
        List<String> log = new ArrayList<>();
        Signal<Boolean> childAlive = Signal.of(true);

        Component owner = new Component() {
            @Override
            protected Element build() {
                Component child = new Component() {
                    @Override
                    protected Element build() {
                        return new Element();
                    }

                    @Override
                    protected void onDispose() {

                        log.add("child-disposed");
                    }
                };
                child.element();

                Computed<Boolean> derived = Signal.computed(() -> {
                    log.add("recompute:" + childAlive.get());
                    return childAlive.get();
                });
                derived.get();
                adopt(derived);
                return new Element();
            }
        };

        owner.element();
        log.clear();

        owner.dispose();

        assertEquals(Arrays.asList("child-disposed"), log,
                "Disposing the owner must not let a still-live Computed recompute against the dead child");
    }

    @Test
    void aThrowingChildDoesNotBlockRemainingDisposal() {
        List<String> log = new ArrayList<>();

        Component owner = new Component() {
            @Override
            protected Element build() {
                own(DisposableAction.of(() -> log.add("own-leaf-1")));
                own(DisposableAction.of(() -> log.add("own-leaf-2")));

                Probe bad = new Probe("bad-child", log);
                bad.failOnDispose = new RuntimeException("child disposal failed");
                bad.element();

                Probe good = new Probe("good-child", log);
                good.element();
                return new Element();
            }
        };

        owner.element();
        assertThrows(RuntimeException.class, owner::dispose, "Disposal must fail loud when a child fails");

        assertTrue(log.contains("good-child"), "A throwing child must not stop the remaining children");
        assertTrue(log.contains("own-leaf-2"));
        assertTrue(log.contains("own-leaf-1"), "Own resources must still dispose after a child throws");
    }

    @Test
    void dynamicFragmentDisposalStaysIdempotentAgainstTheChildrenPass() {
        Signal<Boolean> flag = Signal.of(true);
        List<String> log = new ArrayList<>();

        Dynamic<Boolean> dynamic = Dynamic.of(flag, value -> {
            Probe probe = new Probe("dynamic-child", log);
            probe.element();
        });
        dynamic.element();
        assertFalse(dynamic.isDisposed());
        assertTrue(dynamic.children().isEmpty(),
                "A Dynamic mounts its fragment outside the ownership tree, so the children pass has nothing to redo");

        dynamic.dispose();
        assertEquals(1, Collections.frequency(log, "dynamic-child"),
                "The fragment is disposed exactly once");

        dynamic.dispose();
        assertEquals(1, Collections.frequency(log, "dynamic-child"));
    }
}
