package solim.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Application;
import arc.Core;
import arc.Events;
import arc.Graphics;
import arc.graphics.Color;
import arc.graphics.GL20;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
import mindustry.game.EventType.ResizeEvent;
import solim.display.Badge;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.graphics.RoundedCache;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.ReactiveGrid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.VirtualList;
import solim.layout.Wrap;
import solim.overlay.Popup;
import solim.reactive.Computed;
import solim.reactive.Dynamic;
import solim.reactive.ForEach;
import solim.reactive.Signal;
import solim.runtime.ComponentContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

/**
 * Uniform disposal sweep over every {@link Disposable} in solim packages.
 *
 * <p>Deep behavior for inputs lives in {@code solim.input.TwoWayBindingDisposalTest},
 * reactive primitives in {@code solim.reactive.DisposalPrimitivesTest}, the reconciler in
 * {@code solim.runtime.StructuralReconcilerDisposalTest}, overlays in
 * {@code solim.overlay} disposal tests, and config/graphics/feedback in their own
 * disposal tests. This sweep pins the shared contract plus mounting modes here.
 */
class DisposalSweepTest extends SolimEnv {

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;
	static Scene prevScene;
	static boolean createdScene;

	static Font testFont() {
		FontData fontData = new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}
		};
		return new Font(fontData, new TextureRegion(), false);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static void addStyleIfMissing(Class type, Object style) {
		try {
			Core.scene.getStyle(type);
		} catch (IllegalArgumentException missing) {
			Core.scene.addStyle(type, style);
		}
	}

	@BeforeAll
	static void initArc() {
		prevApp = Core.app;
		prevGraphics = Core.graphics;
		prevGl = Core.gl;
		prevGl20 = Core.gl20;
		prevScene = Core.scene;
		if (Core.scene == null) {
			Core.scene = new Scene();
			createdScene = true;
		}
		Font font = testFont();
		addStyleIfMissing(ButtonStyle.class, new ButtonStyle());
		addStyleIfMissing(ScrollPaneStyle.class, new ScrollPaneStyle());
		addStyleIfMissing(SliderStyle.class, new SliderStyle());
		try {
			Core.scene.getStyle(CheckBoxStyle.class);
		} catch (IllegalArgumentException missing) {
			CheckBoxStyle style = new CheckBoxStyle();
			style.font = font;
			Core.scene.addStyle(CheckBoxStyle.class, style);
		}
		try {
			Core.scene.getStyle(TextButtonStyle.class);
		} catch (IllegalArgumentException missing) {
			TextButtonStyle style = new TextButtonStyle();
			style.font = font;
			Core.scene.addStyle(TextButtonStyle.class, style);
		}
		try {
			Core.scene.getStyle(LabelStyle.class);
		} catch (IllegalArgumentException missing) {
			LabelStyle style = new LabelStyle();
			style.font = font;
			Core.scene.addStyle(LabelStyle.class, style);
		}
		try {
			Core.scene.getStyle(TextFieldStyle.class);
		} catch (IllegalArgumentException missing) {
			TextFieldStyle style = new TextFieldStyle();
			style.font = font;
			Core.scene.addStyle(TextFieldStyle.class, style);
		}
	}

	@AfterAll
	static void tearDownArc() {
		Core.app = prevApp;
		Core.graphics = prevGraphics;
		Core.gl = prevGl;
		Core.gl20 = prevGl20;
		Core.scene = createdScene ? null : prevScene;
		prevApp = null;
		prevGraphics = null;
		prevGl = null;
		prevGl20 = null;
		prevScene = null;
		createdScene = false;
	}

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
		NetworkImage.clearCache();
		NetworkImage.setImageLoader(null);
	}

	static final class Nest extends BaseComponent {
		private final Runnable body;
		private final Table root = new Table();

		Nest(Runnable body) {
			this.body = body;
		}

		@Override
		protected Element build() {
			if (body != null) {
				body.run();
			}
			return root;
		}
	}

	static final class Probe<T extends Disposable> {
		T value;

		void setValue(T value) {
			this.value = value;
		}
	}

	static void assertContract(Disposable disposable) {
		assertTrue(disposable.isDisposed(), "isDisposed must be true after dispose: " + disposable.getClass());
		assertDoesNotThrow(disposable::dispose, "Double dispose must be safe: " + disposable.getClass());
		assertTrue(disposable.isDisposed());
	}

	static class Owner extends BaseComponent {
		Computed<String> computed;
		final Signal<Integer> source = Signal.of(1);

		@Override
		protected Element build() {
			computed = source.map(v -> "v" + v);
			own(computed);
			computed.peek();
			return new Element();
		}
	}

	@Test
	void ownedComputedDisposesWithBaseComponent() {
		Owner owner = new Owner();
		owner.element();
		assertFalse(owner.computed.isDisposed());
		owner.dispose();
		assertTrue(owner.computed.isDisposed());
		assertContract(owner.computed);
	}

	@Test
	void textBindingDiesStandalone() {
		Signal<String> name = Signal.of("a");
		Text text = Text.of(name);
		assertEquals("a", text.label().getText().toString());
		assertFalse(text.isDisposed());

		text.dispose();
		assertContract(text);

		name.set("b");
		SignalDispatcher.flush();
		assertEquals("a", text.label().getText().toString(), "Disposed Text must stop reacting");
	}

	@Test
	void textBindingDiesNested() {
		Signal<String> name = Signal.of("a");
		Probe<Text> probe = new Probe<>();
		Nest nest = new Nest(() -> probe.setValue(Text.of(name)));
		nest.element();
		assertEquals(0, ComponentContext.size());
		assertEquals("a", probe.value.label().getText().toString());

		nest.dispose();
		assertTrue(nest.isDisposed());
		assertContract(probe.value);

		name.set("b");
		SignalDispatcher.flush();
		assertEquals("a", probe.value.label().getText().toString());
	}

	@Test
	void buttonCheckedBindingDies() {
		Signal<Boolean> checked = Signal.of(false);
		Button button = new Button().checked(checked);
		assertFalse(button.isDisposed());

		checked.set(true);
		SignalDispatcher.flush();
		assertTrue(button.element().isChecked());

		button.dispose();
		assertContract(button);

		checked.set(false);
		SignalDispatcher.flush();
		assertTrue(button.element().isChecked(), "Disposed Button must stop reacting");
	}

	@Test
	void cardColorBindingDies() {
		Signal<Color> color = Signal.of(Color.red.cpy());
		Card card = new Card().color(color);
		assertEquals(1f, card.table().color.r, 0.0001f);

		card.dispose();
		assertContract(card);

		color.set(Color.blue.cpy());
		SignalDispatcher.flush();
		assertEquals(1f, card.table().color.r, 0.0001f, "Disposed Card must stop reacting");
		assertEquals(0f, card.table().color.b, 0.0001f);
	}

	@Test
	void rowColumnGridGapBindingsDie() {
		Signal<Float> gap = Signal.of(4f);
		Signal<Integer> columns = Signal.of(2);
		Row row = new Row().gap(gap);
		Column column = new Column().gap(gap);
		Grid grid = new Grid().columns(columns).gap(gap);

		gap.set(8f);
		columns.set(3);
		SignalDispatcher.flush();
		assertEquals(8f, row.gap(), 0.0001f);
		assertEquals(8f, column.gap(), 0.0001f);
		assertEquals(8f, grid.gap(), 0.0001f);

		row.dispose();
		column.dispose();
		grid.dispose();
		assertContract(row);
		assertContract(column);
		assertContract(grid);

		gap.set(16f);
		columns.set(1);
		SignalDispatcher.flush();
		assertEquals(8f, row.gap(), 0.0001f, "Disposed Row must stop reacting");
		assertEquals(8f, column.gap(), 0.0001f, "Disposed Column must stop reacting");
		assertEquals(8f, grid.gap(), 0.0001f, "Disposed Grid must stop reacting");
	}

	@Test
	void gridGapDiesNested() {
		Signal<Float> gap = Signal.of(4f);
		Probe<Grid> probe = new Probe<>();
		Nest nest = new Nest(() -> probe.setValue(new Grid().gap(gap)));
		nest.element();

		gap.set(8f);
		SignalDispatcher.flush();
		assertEquals(8f, probe.value.gap(), 0.0001f);

		nest.dispose();
		assertContract(probe.value);

		gap.set(16f);
		SignalDispatcher.flush();
		assertEquals(8f, probe.value.gap(), 0.0001f);
	}

	@Test
	void solimImageColorBindingDies() {
		Signal<Color> color = Signal.of(Color.red.cpy());
		SolimImage image = new SolimImage().color(color);
		assertFalse(image.isDisposed());

		image.dispose();
		assertContract(image);

		color.set(Color.blue.cpy());
		SignalDispatcher.flush();
	}

	@Test
	void badgeCountBindingDies() {
		Signal<Integer> count = Signal.of(1);
		Badge badge = Badge.ofCount(count);
		assertFalse(badge.isDisposed());

		badge.dispose();
		assertContract(badge);

		count.set(5);
		SignalDispatcher.flush();
	}

	@Test
	void dividerDelegatesDisposeToImage() {
		Divider divider = new Divider();
		assertFalse(divider.isDisposed());

		divider.dispose();
		assertContract(divider);
	}

	@Test
	void scrollClearsCallbacksOnDispose() {
		Scroll scroll = new Scroll();
		AtomicInteger calls = new AtomicInteger(0);
		scroll.onReachTop(calls::incrementAndGet);
		scroll.onReachBottom(calls::incrementAndGet);
		assertFalse(scroll.isDisposed());

		scroll.dispose();
		assertContract(scroll);
	}

	@Test
	void stackSpacerWrapDisposeSafely() {
		SolimStack stack = new SolimStack();
		Spacer spacer = new Spacer();
		Wrap wrap = new Wrap();
		assertFalse(stack.isDisposed());
		assertFalse(spacer.isDisposed());
		assertFalse(wrap.isDisposed());

		stack.dispose();
		spacer.dispose();
		wrap.dispose();
		assertContract(stack);
		assertContract(spacer);
		assertContract(wrap);
	}

	@Test
	void networkImageUrlBindingDies() {
		AtomicInteger loads = new AtomicInteger(0);
		NetworkImage.setImageLoader((url, radius, targetW, targetH, onSuccess, onError) -> loads.incrementAndGet());
		Signal<String> url = Signal.of("sweep://a");
		NetworkImage image = new NetworkImage(url);
		assertEquals(1, loads.get());
		assertFalse(image.isDisposed());

		url.set("sweep://b");
		SignalDispatcher.flush();
		assertEquals(2, loads.get());

		image.dispose();
		assertContract(image);

		url.set("sweep://c");
		SignalDispatcher.flush();
		assertEquals(2, loads.get(), "Disposed NetworkImage must stop loading");
	}

	@Test
	void networkImageColorBindingDiesNested() {
		Signal<Color> color = Signal.of(Color.red.cpy());
		Probe<NetworkImage> probe = new Probe<>();
		Nest nest = new Nest(() -> probe.setValue(new NetworkImage().color(color)));
		nest.element();

		nest.dispose();
		assertContract(probe.value);

		color.set(Color.blue.cpy());
		SignalDispatcher.flush();
	}

	@Test
	void loopCreatedChildrenAllDisposedWithParent() {
		Signal<String> name = Signal.of("a");
		List<Text> created = new ArrayList<>();
		Nest nest = new Nest(() -> {
			for (int i = 0; i < 10; i++) {
				created.add(Text.of(name));
			}
		});
		nest.element();
		assertEquals(10, created.size());

		nest.dispose();
		assertTrue(nest.isDisposed());
		for (Text text : created) {
			assertTrue(text.isDisposed(), "Loop-created child must dispose with parent");
		}

		name.set("b");
		SignalDispatcher.flush();
		for (Text text : created) {
			assertEquals("a", text.label().getText().toString());
		}
	}

	@Test
	void forEachRemovesAndDisposes() {
		Signal<List<String>> items = Signal.of(new ArrayList<>(Arrays.asList("a", "b")));
		Signal<String> label = Signal.of("item");
		List<Text> created = new ArrayList<>();
		ForEach<String> forEach = new ForEach<>(items);
		forEach.key(item -> item);
		forEach.children(item -> {
			Text text = Text.of(label);
			created.add(text);
			return text;
		});
		forEach.element();
		assertEquals(2, created.size());
		assertFalse(forEach.isDisposed());

		items.set(new ArrayList<>(Arrays.asList("b", "c")));
		SignalDispatcher.flush();
		assertTrue(created.get(0).isDisposed(), "Removed item must be disposed");
		assertFalse(created.get(1).isDisposed(), "Retained item must survive");
		assertEquals(3, created.size());

		forEach.dispose();
		assertContract(forEach);
		assertTrue(created.get(1).isDisposed());
		assertTrue(created.get(2).isDisposed());

		label.set("gone");
		SignalDispatcher.flush();
		for (Text text : created) {
			assertEquals("item", text.label().getText().toString());
		}
	}

	@Test
	void reactiveGridRemovesAndDisposes() {
		Signal<Integer> columns = Signal.of(2);
		Signal<List<String>> items = Signal.of(new ArrayList<>(Arrays.asList("a", "b", "c", "d")));
		Signal<String> label = Signal.of("cell");
		List<Text> created = new ArrayList<>();
		ReactiveGrid<String> grid = new ReactiveGrid<>(items);
		grid.columns(columns).key(item -> item);
		grid.children(item -> {
			Text text = Text.of(label);
			created.add(text);
			return text;
		});
		grid.element();
		assertEquals(4, created.size());

		items.set(new ArrayList<>(Arrays.asList("b", "c")));
		SignalDispatcher.flush();
		assertTrue(created.get(0).isDisposed(), "Removed grid item must be disposed");
		assertTrue(created.get(3).isDisposed(), "Removed grid item must be disposed");
		assertFalse(created.get(1).isDisposed());

		columns.set(1);
		SignalDispatcher.flush();

		grid.dispose();
		assertContract(grid);
		assertTrue(created.get(1).isDisposed());
		assertTrue(created.get(2).isDisposed());

		label.set("gone");
		SignalDispatcher.flush();
		for (Text text : created) {
			assertEquals("cell", text.label().getText().toString());
		}
	}

	@Test
	void dynamicSwitchDisposesPreviousAndCurrent() {
		Signal<Boolean> toggle = Signal.of(true);
		Signal<String> label = Signal.of("state");
		List<Text> created = new ArrayList<>();
Dynamic<Boolean> dynamic = Dynamic.of(toggle, active -> {
            Text text = Text.of(label);
            created.add(text);
        });
		dynamic.element();
		assertEquals(1, created.size());

		toggle.set(false);
		SignalDispatcher.flush();
		assertEquals(2, created.size());
		assertTrue(created.get(0).isDisposed(), "Replaced subtree must be disposed");

		dynamic.dispose();
		assertContract(dynamic);
		assertTrue(created.get(1).isDisposed(), "Active subtree must dispose with Dynamic");

		label.set("gone");
		SignalDispatcher.flush();
		for (Text text : created) {
			assertEquals("state", text.label().getText().toString());
		}
	}

	@Test
	void virtualListDisposesWithOwner() {
		Signal<List<String>> items = Signal.of(new ArrayList<>(Arrays.asList("a", "b", "c")));
		Signal<String> label = Signal.of("row");
		List<Text> created = new ArrayList<>();
		VirtualList<String> list = new VirtualList<>(items, (item, width) -> 48f);
		list.key(item -> item);
		list.children(item -> {
			Text text = Text.of(label);
			created.add(text);
			return text;
		});
		list.element();
		assertFalse(list.isDisposed());

		list.dispose();
		assertContract(list);
		for (Text text : created) {
			assertTrue(text.isDisposed(), "VirtualList item must dispose with owner");
		}

		label.set("gone");
		SignalDispatcher.flush();
		for (Text text : created) {
			assertEquals("row", text.label().getText().toString());
		}
	}

	@Test
	void popupDisposesAndUnregistersResize() {
		Popup<String> popup = new Popup<>();
		popup.element();
		assertFalse(popup.isDisposed());

		assertDoesNotThrow(() -> Events.fire(new ResizeEvent()));

		popup.dispose();
		assertTrue(popup.isDisposed());

		assertDoesNotThrow(() -> Events.fire(new ResizeEvent()));
		assertDoesNotThrow(popup::dispose, "Double dispose must be safe");
		assertTrue(popup.isDisposed());
	}

	@Test
	void settingsPanelDisposesOwnedResources() {
		SettingsPanel panel = new SettingsPanel();
		panel.element();
		assertFalse(panel.isDisposed());

		panel.dispose();
		assertTrue(panel.isDisposed(), "SettingsPanel must dispose BaseComponent resources");

		assertDoesNotThrow(panel::dispose, "Double dispose must be safe");
		assertTrue(panel.isDisposed());
	}

	@Test
	void instanceDisposalLeavesGlobalStateAlone() {
		Text text = Text.of("x");
		text.dispose();
		NetworkImage image = new NetworkImage();
		image.dispose();
		RoundedDrawable drawable = RoundedDrawable.of(4, Color.white.cpy());
		drawable.dispose();

		assertDoesNotThrow(NetworkImage::clearCache, "Static cache API must survive instance disposal");
		assertNotNull(RoundedCache.getSolid(4),
			"Shared rounded cache must survive instance disposal");

		Signal<Integer> radius = Signal.of(4);
		RoundedDrawable live = new RoundedDrawable(2).radius(radius);
		radius.set(6);
		SignalDispatcher.flush();
		assertEquals(6, live.getRadius(), "New instances must stay functional after others dispose");
		live.dispose();
		assertTrue(live.isDisposed());
	}

	@Test
	void repeatedMountUnmoutDrainsComponentGraph() {
		Signal<String> name = Signal.of("a");
		for (int i = 0; i < 20; i++) {
			Probe<Text> probe = new Probe<>();
			Nest nest = new Nest(() -> probe.setValue(Text.of(name)));
			nest.element();
			nest.dispose();
			assertTrue(probe.value.isDisposed());
		}
		SignalDispatcher.flush();
		assertDoesNotThrow(() -> name.set("b"));
		assertEquals(0, ComponentContext.size());
	}
}

