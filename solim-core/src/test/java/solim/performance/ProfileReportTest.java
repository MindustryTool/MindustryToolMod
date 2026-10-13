package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solim.core.Component;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.test.FixedComponent;
import solim.test.SolimEnv;
import solim.test.TestDisposable;

class ProfileReportTest extends SolimEnv {

    @TempDir
    File tempDir;

    @BeforeEach
    void enableDebugServices() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(true);
    }

    @AfterEach
    void restoreDefaults() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
    }

    @Test
    void snapshotCapturesComponentNameClassAndChildren() {
        FixedComponent parent = new FixedComponent(namedElement("parent"));
        FixedComponent child = new FixedComponent(namedElement("child"));
        parent.adopt(child);
        child.element();
        parent.element();

        List<ComponentNodeSnapshot> roots = ComponentSnapshotBuilder.capture();
        ComponentNodeSnapshot parentNode = findByName(roots, "parent");

        assertNotNull(parentNode, "the adopted root component must appear in the snapshot");
        assertEquals("FixedComponent", parentNode.className);
        assertEquals(1, parentNode.children.size());
        assertEquals("child", parentNode.children.get(0).name);
    }

    @Test
    void snapshotAttributesSignalCreatedDuringComponentBuild() {
        Component host = new BuildSignalComponent();

        host.element();

        List<ComponentNodeSnapshot> roots = ComponentSnapshotBuilder.capture();
        ComponentNodeSnapshot node = findByClass(roots, BuildSignalComponent.class);

        assertNotNull(node, "the building component must appear in the snapshot");
        assertEquals(1, node.signals.size(),
                "a Signal created inside build() must be attributed to that component");
        assertEquals(1, node.signals.get(0).writeCount);
    }

    @Test
    void snapshotDiscoversExternalSignalThroughOwnedEffectDependencyGraph() {
        Signal<Integer> external = Signal.of(0);
        Component host = new EffectReadingComponent(external);

        host.element();
        external.set(1);
        flushEffects();

        List<ComponentNodeSnapshot> roots = ComponentSnapshotBuilder.capture();
        ComponentNodeSnapshot node = findByClass(roots, EffectReadingComponent.class);

        assertNotNull(node);
        assertEquals(1, node.signals.size(),
                "an externally created signal read by an owned effect must be discovered");
        assertEquals("1", node.signals.get(0).value);
        assertEquals(1, node.signals.get(0).writeCount);
        assertEquals(1, node.effects.size(), "the owned effect must be listed");
        assertTrue(node.effects.get(0).runCount >= 2,
                "the effect runs once at creation and again on the signal write");
    }

    @Test
    void snapshotAttributesOwnedComputed() {
        Signal<Integer> source = Signal.of(1);
        Component host = new ComputedHostComponent(source);

        host.element();

        List<ComponentNodeSnapshot> roots = ComponentSnapshotBuilder.capture();
        ComponentNodeSnapshot node = findByClass(roots, ComputedHostComponent.class);

        assertNotNull(node);
        assertEquals(1, node.computeds.size(), "the owned computed must be listed");
        assertTrue(node.computeds.get(0).recomputeCount >= 1);
        assertFalse(node.computeds.get(0).dependencies.isEmpty(),
                "a computed reading a signal must report that dependency");
    }

    @Test
    void snapshotListsOtherOwnedDisposables() {
        Component host = new DisposableHostComponent();

        host.element();

        List<ComponentNodeSnapshot> roots = ComponentSnapshotBuilder.capture();
        ComponentNodeSnapshot node = findByClass(roots, DisposableHostComponent.class);

        assertNotNull(node);
        assertEquals(Collections.singletonList("TestDisposable"), node.otherDisposables,
                "non-reactive owned disposables must still be reported");
    }

    @Test
    void snapshotDoesNotBuildLazyComponents() {
        Component host = new BuildSignalComponent();

        assertNotNull(host);
        ComponentNodeSnapshot node = findByClass(ComponentSnapshotBuilder.capture(),
                BuildSignalComponent.class);

        assertNotNull(node);
        assertNull(node.elementType,
                "an unbuilt lazy component must report no element type rather than being built");
    }

    @Test
    void renderedReportEmbedsAllThreeInteractiveViews() {
        String html = ProfileReportExporter.render(
                Collections.singletonList(new TraceSpan(1L, 0, -1, "root", "build", 0L, 1000L, 0, 1)),
                0, 1L, Collections.singletonList(sampleComponentNode()));

        assertTrue(html.contains("id=\"tab-flame\""), "flame graph tab must be present");
        assertTrue(html.contains("id=\"tab-tree\""), "component tree tab must be present");
        assertTrue(html.contains("id=\"tab-hotspots\""), "reactive hotspots tab must be present");
        assertTrue(html.contains("id=\"flame-canvas\""), "flame graph must render to a canvas");
        assertTrue(html.contains("id=\"tree-view\""), "component tree must have a view pane");
        assertTrue(html.contains("id=\"inspector\""), "component tree must have an inspector pane");
        assertTrue(html.contains("id=\"hot-signals\""), "hotspots must rank signals");
        assertTrue(html.contains("id=\"hot-computeds\""), "hotspots must rank computeds");
        assertTrue(html.contains("id=\"hot-effects\""), "hotspots must rank effects");
    }

    @Test
    void renderedReportEmbedsProfileAndExportPayloads() {
        String html = ProfileReportExporter.render(
                Collections.singletonList(new TraceSpan(1L, 0, -1, "root", "build", 0L, 1000L, 0, 1)),
                0, 1L, Collections.singletonList(sampleComponentNode()));

        assertTrue(html.contains("id=\"solim-profile\""), "the profile payload must be embedded");
        assertTrue(html.contains("id=\"solim-chrome\""), "the chrome trace payload must be embedded");
        assertTrue(html.contains("id=\"solim-speedscope\""), "the speedscope payload must be embedded");
        assertTrue(html.contains("traceEvents"), "the chrome trace payload must contain trace events");
        assertTrue(html.contains("speedscope.app"), "the speedscope payload must be a native profile");
    }

    @Test
    void renderedReportIsSelfContainedWithNoExternalResources() {
        String html = ProfileReportExporter.render(
                Collections.<TraceSpan>emptyList(), 0, -1L, Collections.<ComponentNodeSnapshot>emptyList());

        assertFalse(html.contains("http://"), "the report must not reference plaintext external URLs");
        assertFalse(html.contains("<link "), "the report must not load external stylesheets");
        assertFalse(html.contains("src=\""), "the report must not load external scripts or images");
    }

    @Test
    void payloadCarriesComponentMetadataAndReactiveState() {
        String payload = ProfileReportExporter.payload(
                Collections.singletonList(new TraceSpan(1L, 0, -1, "root", "build", 0L, 1000L, 0, 1)),
                0, 1L, Collections.singletonList(sampleComponentNode()));

        assertTrue(payload.contains("\"className\":\"Column\""));
        assertTrue(payload.contains("\"name\":\"Panel\""));
        assertTrue(payload.contains("\"elementType\":\"Table\""));
        assertTrue(payload.contains("\"bounds\":[10.0,20.0,100.0,50.0]"));
        assertTrue(payload.contains("\"writeCount\":7"));
        assertTrue(payload.contains("\"recomputeCount\":4"));
        assertTrue(payload.contains("\"runCount\":9"));
        assertTrue(payload.contains("\"phase\":\"build\""));
    }

    @Test
    void payloadEscapesValuesThatCouldBreakOutOfTheScriptBlock() {
        ComponentNodeSnapshot hostile = new ComponentNodeSnapshot(1, "Row", "</script><img>",
                "Table", new float[] { 0f, 0f, 0f, 0f }, true, false,
                Collections.<ComponentNodeSnapshot>emptyList(),
                Collections.singletonList(new SignalSnapshot(1, "</script>", 0, 0, 0)),
                Collections.<ComputedSnapshot>emptyList(),
                Collections.<EffectSnapshot>emptyList(),
                Collections.<String>emptyList());

        String payload = ProfileReportExporter.payload(null, 0, -1L,
                Collections.singletonList(hostile));
        String html = ProfileReportExporter.render(null, 0, -1L, Collections.singletonList(hostile));

        assertFalse(payload.contains("</script>"), "payload values must not close the script block");
        assertFalse(html.contains("</script><img>"), "hostile names must not break out of the report");
    }

    @Test
    void writtenReportIsReadableAndContainsThePayloads() throws Exception {
        File target = new File(tempDir, "nested/profile-test.html");

        ProfileReportExporter.writeFile(
                Collections.singletonList(new TraceSpan(1L, 0, -1, "root", "build", 0L, 1000L, 0, 1)),
                0, 1L, Collections.singletonList(sampleComponentNode()), target);

        assertTrue(target.exists(), "the exporter must create missing parent directories");
        String html = new String(Files.readAllBytes(target.toPath()), Charset.forName("UTF-8"));
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains("id=\"solim-profile\""));
        assertTrue(html.contains("id=\"tab-hotspots\""));
    }

    @Test
    void profilerWritesReportToAnExplicitFileWhenStopped() throws Exception {
        File target = new File(tempDir, "explicit-profile.html");

        Perf.setTracingEnabled(true);
        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        Perf.startProfiling();
        assertTrue(Perf.isProfiling());

        signal.set(2);
        Perf.stopProfiling(target);

        assertFalse(Perf.isProfiling(), "stopping must end the session");
        assertTrue(target.exists(), "the session must write its report to the requested file");
        String html = new String(Files.readAllBytes(target.toPath()), Charset.forName("UTF-8"));
        assertTrue(html.contains("id=\"tab-flame\""));
    }

    @Test
    void togglingProfilingRequiresPerfToBeEnabled() {
        Perf.setEnabled(false);

        Perf.toggleProfiling();

        assertFalse(Perf.isProfiling(),
                "with debug services uninstalled there is nothing to record a session");
    }

    private static Element namedElement(String name) {
        Element element = new Element();
        element.name = name;
        return element;
    }

    private static ComponentNodeSnapshot sampleComponentNode() {
        return new ComponentNodeSnapshot(1, "Column", "Panel", "Table",
                new float[] { 10f, 20f, 100f, 50f }, true, false,
                Collections.<ComponentNodeSnapshot>emptyList(),
                Collections.singletonList(new SignalSnapshot(1, "42", 7, 2, 1)),
                Collections.singletonList(new ComputedSnapshot(2, "84", 4, 4000L, 1000L, false,
                        Collections.singletonList("Signal(Integer)"))),
                Collections.singletonList(new EffectSnapshot(3, 9, 9000L, 500L,
                        Collections.singletonList("Signal(Integer)"))),
                Collections.singletonList("Subscription"));
    }

    private static ComponentNodeSnapshot findByName(List<ComponentNodeSnapshot> nodes, String name) {
        for (ComponentNodeSnapshot node : nodes) {
            if (name.equals(node.name)) {
                return node;
            }
            ComponentNodeSnapshot found = findByName(node.children, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static ComponentNodeSnapshot findByClass(List<ComponentNodeSnapshot> nodes, Class<?> type) {
        for (ComponentNodeSnapshot node : nodes) {
            if (type.getSimpleName().equals(node.className)) {
                return node;
            }
            ComponentNodeSnapshot found = findByClass(node.children, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static class BuildSignalComponent extends Component {
        @Override
        protected Element build() {
            Signal.of(0).set(1);
            return new Element();
        }
    }

    static class EffectReadingComponent extends Component {
        private final Signal<Integer> external;

        EffectReadingComponent(Signal<Integer> external) {
            this.external = external;
        }

        @Override
        protected Element build() {
            Signal<Integer> signal = external;
            Effect.of(() -> {
                signal.get();
            });
            return new Element();
        }
    }

    static class ComputedHostComponent extends Component {
        private final Signal<Integer> source;

        ComputedHostComponent(Signal<Integer> source) {
            this.source = source;
        }

        @Override
        protected Element build() {
            Signal.computed(() -> source.get()).get();
            return new Element();
        }
    }

    static class DisposableHostComponent extends Component {
        @Override
        protected Element build() {
            own(new TestDisposable());
            return new Element();
        }
    }
}
