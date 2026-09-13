package solim.layout;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementModifiers;
import solim.overlay.Hud;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.runtime.ParentStack;
import solim.ui.Ui;

/** Row layout — horizontal Table wrapper. */
public final class Row implements Component, LayoutModifiers<Row>, GapContainer {

	public static final ParentStack.Attacher ATTACHER = (table, child) -> {
		Cell<?> cell = table.add(child);
		if (Ui.isExpanding(child)) {
			cell.growX();
		}
		if (table.userObject instanceof GapContainer) {
			GapContainer gc = (GapContainer) table.userObject;
			GapContainer.spaceAttachedCell(table, cell, Direction.HORIZONTAL, gc.gap());
		}
		return cell;
	};

	private final Table table;
	private final SizeConstraints constraints = new SizeConstraints();
	private float gap = 0f;

	public Row() {
		this.table = new Table();
		this.table.userObject = this;
		this.table.name = "solim-row-table";
		this.table.left();
	}

	public Table table() {
		return table;
	}

	@Override
	public Element element() {
		return table;
	}

	@Override
	public SizeConstraints sizeConstraints() {
		return constraints;
	}

	public Row name(String name) {
		ElementModifiers.name(table, name);
		return this;
	}

	public Row fillParent(boolean fillParent) {
		table.setFillParent(fillParent);
		return this;
	}

	public Row fillParent() {
		return fillParent(true);
	}

	public Row touchable(Touchable touchable) {
		table.touchable = touchable;
		return this;
	}

	public Row gap(float g) {
		this.gap = g;
		respace();
		return this;
	}

	public Row gap(@Nullable Readable<Float> gapSignal) {
		if (gapSignal != null) {
			Effect e = Effect.of(() -> {
				Float g = gapSignal.get();
				if (g != null) {
					gap(g);
				}
			});
			ComponentContext.register(e);
		}
		return this;
	}

	@Override
	public Direction direction() {
		return Direction.HORIZONTAL;
	}

	@Override
	public float gap() {
		return gap;
	}

	@Override
	public void respace() {
		GapContainer.applySpacing(table, Direction.HORIZONTAL, gap);
	}

	public Row padding(float p) {
		ElementModifiers.padding(table, p);
		return this;
	}

	public Row padding(float top, float left, float bottom, float right) {
		ElementModifiers.padding(table, top, left, bottom, right);
		return this;
	}

	public Row paddingTop(float top) {
		ElementModifiers.paddingTop(table, top);
		return this;
	}

	public Row paddingBottom(float bottom) {
		ElementModifiers.paddingBottom(table, bottom);
		return this;
	}

	public Row paddingLeft(float left) {
		ElementModifiers.paddingLeft(table, left);
		return this;
	}

	public Row paddingRight(float right) {
		ElementModifiers.paddingRight(table, right);
		return this;
	}

	public Row margin(float m) {
		ElementModifiers.margin(table, m);
		return this;
	}

	public Row margin(float top, float left, float bottom, float right) {
		ElementModifiers.margin(table, top, left, bottom, right);
		return this;
	}

	public Row marginTop(float top) {
		ElementModifiers.marginTop(table, top);
		return this;
	}

	public Row marginBottom(float bottom) {
		ElementModifiers.marginBottom(table, bottom);
		return this;
	}

	public Row marginLeft(float left) {
		ElementModifiers.marginLeft(table, left);
		return this;
	}

	public Row marginRight(float right) {
		ElementModifiers.marginRight(table, right);
		return this;
	}

	public Row paddingX(float x) {
		ElementModifiers.paddingX(table, x);
		return this;
	}

	public Row paddingY(float y) {
		ElementModifiers.paddingY(table, y);
		return this;
	}

	@Override
	public Row marginX(float x) {
		ElementModifiers.marginX(table, x);
		return this;
	}

	@Override
	public Row marginY(float y) {
		ElementModifiers.marginY(table, y);
		return this;
	}

	public Row x(float x) {
		ElementModifiers.x(table, x);
		return this;
	}

	public Row y(float y) {
		ElementModifiers.y(table, y);
		return this;
	}

	public Row position(float x, float y) {
		ElementModifiers.position(table, x, y);
		return this;
	}

	public Row visible(boolean visible) {
		ElementModifiers.visible(table, visible);
		return this;
	}

	public Row visible(@Nullable Readable<Boolean> visible) {
		ElementModifiers.visible(table, visible);
		return this;
	}

	@Override
	public Row top() {
		ElementModifiers.top(table);
		table.defaults().top();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.top();
		}
		return LayoutModifiers.super.top();
	}

	@Override
	public Row bottom() {
		ElementModifiers.bottom(table);
		table.defaults().bottom();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.bottom();
		}
		return LayoutModifiers.super.bottom();
	}

	@Override
	public Row left() {
		ElementModifiers.left(table);
		table.defaults().left();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.left();
		}
		return LayoutModifiers.super.left();
	}

	@Override
	public Row right() {
		ElementModifiers.right(table);
		table.defaults().right();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.right();
		}
		return LayoutModifiers.super.right();
	}

	@Override
	public Row center() {
		ElementModifiers.center(table);
		table.defaults().center();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.center();
		}
		return LayoutModifiers.super.center();
	}

	public Row justify(Justify j) {
		switch (j) {
			case START:
				return left();
			case CENTER:
				return center();
			case END:
				return right();
			case BETWEEN:
			case AROUND:
			case EVENLY:
			default:
				break;
		}
		return this;
	}

	public Row align(Align a) {
		switch (a) {
			case START:
			case STRETCH:
				return top();
			case CENTER:
				return center();
			case END:
				return bottom();
			default:
				break;
		}
		return this;
	}

	public Row children(@Nullable Runnable r) {
		ParentStack.push(table, ATTACHER);
		try {
			if (r != null) {
				r.run();
			}
		} finally {
			ParentStack.pop();
		}
		ParentStack.attachToParent(table);
		respace();
		return this;
	}

	public Row background(@Nullable Drawable bg) {
		table.background(bg);
		return this;
	}

	public Row draggable() {
		ElementModifiers.draggable(table);
		return this;
	}

	public Row draggable(@Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
		ElementModifiers.draggable(table, null, xSignal, ySignal);
		return this;
	}

	public Row draggable(@Nullable Hud hud) {
		ElementModifiers.draggable(table, hud);
		return this;
	}

	public Row draggable(@Nullable Hud hud, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
		ElementModifiers.draggable(table, hud, xSignal, ySignal);
		return this;
	}

	public Cell<?> add(Element e) {
		Cell<?> cell = table.add(e);
		respace();
		return cell;
	}
}
