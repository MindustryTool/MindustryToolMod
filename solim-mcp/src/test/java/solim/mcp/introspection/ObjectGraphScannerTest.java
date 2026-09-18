package solim.mcp.introspection;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;

class ObjectGraphScannerTest {

    static class Inner {
        final Signal<String> name = Signal.of("nested");
    }

    static class Root {
        final Signal<Integer> count = Signal.of(1);
        final Inner inner = new Inner();
        final Effect fx = Effect.of(() -> count.get());
    }

    @Test
    void findsReactiveInstancesInGraph() {
        List<ReactiveRef> refs = ObjectGraphScanner.scan(new Root(), 4);
        assertEquals(3, refs.size());

        long signals = refs.stream().filter(r -> r.kind == ReactiveKind.SIGNAL).count();
        long effects = refs.stream().filter(r -> r.kind == ReactiveKind.EFFECT).count();
        assertEquals(2, signals);
        assertEquals(1, effects);
    }

    @Test
    void locationsIncludeFieldPaths() {
        List<ReactiveRef> refs = ObjectGraphScanner.scan(new Root(), 4);
        assertEquals(3, refs.size(), "expected count, inner/name and exactly one effect");
        List<String> locations = refs.stream().map(r -> r.location).collect(Collectors.toList());
        assertTrue(locations.stream().anyMatch(l -> l.contains("count")), "locations=" + locations);
        assertTrue(locations.stream().anyMatch(l -> l.contains("inner/name")), "locations=" + locations);
        assertEquals(1, refs.stream().filter(r -> r.kind == ReactiveKind.EFFECT).count(), "locations=" + locations);
    }

    @Test
    void doesNotLoopOnCycles() {
        @SuppressWarnings("unused")
        class Node {
            Node next;
            Signal<Integer> value = Signal.of(0);
        }
        Node a = new Node();
        Node b = new Node();
        a.next = b;
        b.next = a;
        List<ReactiveRef> refs = ObjectGraphScanner.scan(a, 8);
        assertEquals(2, refs.size());
    }

    @Test
    void computedValueDiscoverable() {
        Signal<Integer> count = Signal.of(2);
        Computed<String> label = count.map(v -> "x" + v);
        List<ReactiveRef> refs = ObjectGraphScanner.scan(new Holder(label), 3);
        assertTrue(refs.stream().anyMatch(r -> r.kind == ReactiveKind.COMPUTED));
    }

    static class Holder {
        final Computed<String> label;

        Holder(Computed<String> label) {
            this.label = label;
        }
    }
}
