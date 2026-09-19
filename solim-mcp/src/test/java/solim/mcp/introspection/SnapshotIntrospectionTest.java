package solim.mcp.introspection;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.SolimToken;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;

class SnapshotIntrospectionTest {

	/** Arc element subclass holding reactive sources. */
	static class GadgetElement extends Table {
		final Signal<Float> progress = Signal.of(0.5f);
		final Computed<String> percent = progress.map(v -> Math.round(v * 100) + "%");
	}

	static class GadgetComponent implements Component {
		final Signal<Integer> scale = Signal.of(3);
		final Computed<String> label = scale.map(v -> "x" + v);
		final Effect fx = Effect.of(() -> scale.get());

		@Override
		public Element element() {
			return new Element();
		}
	}

	@Test
	void treeCapturesNameTypeAndChildren() {
		Table root = new Table();
		root.name = "root";
		GadgetElement gadget = new GadgetElement();
		gadget.name = "gadget";
		root.add(gadget);

		UiSnapshot snapshot = UiSnapshot.capture(root);
		assertTrue(snapshot.tree.toString().contains("\"root\""));
		assertTrue(snapshot.tree.toString().contains("\"gadget\""));
		assertTrue(snapshot.tree.toString().contains("GadgetElement"));
	}

	@Test
	void signalsDiscoveredFromElementFields() {
		Table root = new Table();
		root.name = "root";
		GadgetElement gadget = new GadgetElement();
		gadget.name = "gadget";
		root.add(gadget);

		UiSnapshot snapshot = UiSnapshot.capture(root);
		String signals = snapshot.signalsJson().toString();
		assertTrue(signals.contains("\"progress\""), "element field signal missing: " + signals);
		assertTrue(signals.contains("\"percent\""), "computed missing: " + signals);
		assertTrue(signals.contains("\"gadget\""), "source element missing: " + signals);
	}

@Test
	void signalsDiscoveredFromUserObjectComponent() throws Exception {
		Table root = new Table();
		root.name = "root";
		SolimToken.bind(root, new GadgetComponent());

		UiSnapshot snapshot = UiSnapshot.capture(root);
		JsonNode signals = snapshot.signalsJson();
		boolean hasScale = false;
		boolean hasLabel = false;
		for (JsonNode entry : signals) {
			String location = entry.path("location").asText();
			if (location.contains("scale")) hasScale = true;
			if (location.contains("label")) hasLabel = true;
		}
		assertTrue(hasScale, "scale missing: " + signals);
		assertTrue(hasLabel, "label missing: " + signals);

		JsonNode bindings = snapshot.bindingsJson();
		assertEquals(1, bindings.size(), "expected exactly one effect: " + bindings);
		assertTrue(bindings.toString().contains("dependencies"));
	}

	@Test
	void scansAmbientComponentContext() {
		class Probe extends BaseComponent {
			@SuppressWarnings("unused")
			final Signal<Integer> duringBuild = Signal.of(11);

			@Override
			protected Element build() {
				UiSnapshot snapshot = UiSnapshot.capture(null);
				String signals = snapshot.signalsJson().toString();
				assertTrue(signals.contains("duringBuild"), signals);
				return new Table();
			}
		}
		new Probe().element();
	}

	@Test
	void layoutMetricsAvailableForElements() {
		Table table = new Table();
		table.name = "panel";
		table.setPosition(10f, 20f);

		ObjectNode layout = LayoutInspector.node(table);
		assertNotNull(layout);
		assertEquals(10f, (float) layout.path("x").asDouble());
		assertEquals(20f, (float) layout.path("y").asDouble());
		assertTrue(layout.path("visible").asBoolean());
	}
}
