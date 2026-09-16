package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Computed;
import solim.signal.Signal;
import solim.runtime.ParentStack;
import static solim.ui.Ui.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;

class DisplayTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin/atlas-dependent tests");
	}

	@Test
	void staticText() {
		Table root = new Table();
		ParentStack.push(root);
		ParentStack.add(new Text("Hello").label());
		assertEquals(1, root.getChildren().size);
		ParentStack.pop();
	}

	@Test
	void reactiveTextViaComputed() {
		Signal<Integer> count = Signal.of(1);
		Computed<String> text = count.map(v -> "Count: " + v);
		Text t = Text.of(text);
		// initial compute
		text.get();
		assertEquals("Count: 1", t.label().getText().toString());
		count.set(2);
		text.get();
		assertEquals("Count: 2", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void reactiveTextViaSignal() {
		Signal<String> s = Signal.of("a");
		Text t = Text.of(s);
		assertEquals("a", t.label().getText().toString());
		s.set("b");
		assertEquals("b", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void imageDrawableIsApplied() {
		Drawable drawable = new TextureRegionDrawable(new TextureRegion());
		SolimImage img = new SolimImage(drawable);
		assertSame(drawable, img.image().getDrawable());
	}

	@Test
	void iconDrawableIsApplied() {
		Drawable drawable = new TextureRegionDrawable(new TextureRegion());
		Icon icon = new Icon(drawable);
		assertSame(drawable, icon.image().getDrawable());
	}

	@Test
	void reactiveIconUpdatesDrawableAndStopsAfterDispose() {
		Drawable first = new TextureRegionDrawable(new TextureRegion());
		Drawable second = new TextureRegionDrawable(new TextureRegion());
		Signal<Drawable> s = Signal.of(first);
		Icon icon = Icon.of(s);
		assertSame(first, icon.image().getDrawable());

		s.set(second);
		assertSame(second, icon.image().getDrawable());

		icon.dispose();
		s.set(first);
		assertSame(second, icon.image().getDrawable());
	}

	@Test
	void textDisposeStopsBinding() {
		Signal<String> s = Signal.of("a");
		Text t = Text.of(s);
		assertEquals("a", t.label().getText().toString());
		t.dispose();
		s.set("b");
		assertEquals("a", t.label().getText().toString());
	}

	@Test
	void textName() {
		Text t = new Text("Hello").name("my-text");
		assertEquals("my-text", t.element().name);
	}

	@Test
	void imageName() {
		SolimImage img = new SolimImage().name("my-image");
		assertEquals("my-image", img.element().name);
	}

	@Test
	void textPaddingAndMarginInTable() {
		Table root = new Table();
		ParentStack.push(root);

		Text t = text("Hello").padding(8f).margin(4f);
		assertEquals(12f, CellAccess.padTop(root.getCell(t.label())), 0.01f);
		assertEquals(12f, CellAccess.padLeft(root.getCell(t.label())), 0.01f);
		assertEquals(12f, CellAccess.padBottom(root.getCell(t.label())), 0.01f);
		assertEquals(12f, CellAccess.padRight(root.getCell(t.label())), 0.01f);

		t.padding(1f, 2f, 3f, 4f).margin(5f, 6f, 7f, 8f);
		assertEquals(6f, CellAccess.padTop(root.getCell(t.label())), 0.01f);
		assertEquals(8f, CellAccess.padLeft(root.getCell(t.label())), 0.01f);
		assertEquals(10f, CellAccess.padBottom(root.getCell(t.label())), 0.01f);
		assertEquals(12f, CellAccess.padRight(root.getCell(t.label())), 0.01f);

		t.paddingTop(10f);
		assertEquals(15f, CellAccess.padTop(root.getCell(t.label())), 0.01f);
		t.marginTop(2f);
		assertEquals(12f, CellAccess.padTop(root.getCell(t.label())), 0.01f);

		ParentStack.pop();
	}

	@Test
	void sizedImagePaddingAndMarginInTable() {
		Table root = new Table();
		ParentStack.push(root);

		SolimImage img = image((Drawable) null).padding(6f).margin(2f);
		assertEquals(8f, CellAccess.padTop(root.getCell(img.element())), 0.01f);
		assertEquals(8f, CellAccess.padLeft(root.getCell(img.element())), 0.01f);
		assertEquals(8f, CellAccess.padBottom(root.getCell(img.element())), 0.01f);
		assertEquals(8f, CellAccess.padRight(root.getCell(img.element())), 0.01f);

		img.padding(1f, 2f, 3f, 4f).margin(4f, 3f, 2f, 1f);
		assertEquals(5f, CellAccess.padTop(root.getCell(img.element())), 0.01f);
		assertEquals(5f, CellAccess.padLeft(root.getCell(img.element())), 0.01f);
		assertEquals(5f, CellAccess.padBottom(root.getCell(img.element())), 0.01f);
		assertEquals(5f, CellAccess.padRight(root.getCell(img.element())), 0.01f);

		img.paddingBottom(10f);
		assertEquals(12f, CellAccess.padBottom(root.getCell(img.element())), 0.01f);
		img.marginBottom(5f);
		assertEquals(15f, CellAccess.padBottom(root.getCell(img.element())), 0.01f);

		ParentStack.pop();
	}

	@Test
	void solimImageComponentPaddingAndMargin() {
		Table root = new Table();
		SolimImage comp = new SolimImage().padding(5f).margin(3f);
		root.add(comp.element());
		comp.applySpacing();

		assertEquals(8f, CellAccess.padTop(root.getCell(comp.element())), 0.01f);
		assertEquals(8f, CellAccess.padLeft(root.getCell(comp.element())), 0.01f);
		assertEquals(8f, CellAccess.padBottom(root.getCell(comp.element())), 0.01f);
		assertEquals(8f, CellAccess.padRight(root.getCell(comp.element())), 0.01f);
	}
}
