package solim.layout;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementConfig;
import solim.overlay.Hud;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.runtime.ParentStack;
import solim.ui.Ui;

/** Row layout — horizontal Table wrapper. */
public final class Row implements Component, CellConfig<Row>, GapContainer {

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
		ElementConfig.name(table, name);
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
		ElementConfig.padding(table, p);
		return this;
	}

	public Row padding(float top, float left, float bottom, float right) {
		ElementConfig.padding(table, top, left, bottom, right);
		return this;
	}

	public Row paddingTop(float top) {
		ElementConfig.paddingTop(table, top);
		return this;
	}

	public Row paddingBottom(float bottom) {
		ElementConfig.paddingBottom(table, bottom);
		return this;
	}

	public Row paddingLeft(float left) {
		ElementConfig.paddingLeft(table, left);
		return this;
	}

	public Row paddingRight(float right) {
		ElementConfig.paddingRight(table, right);
		return this;
	}

	public Row margin(float m) {
		ElementConfig.margin(table, m);
		return this;
	}

	public Row margin(float top, float left, float bottom, float right) {
		ElementConfig.margin(table, top, left, bottom, right);
		return this;
	}

	public Row marginTop(float top) {
		ElementConfig.marginTop(table, top);
		return this;
	}

	public Row marginBottom(float bottom) {
		ElementConfig.marginBottom(table, bottom);
		return this;
	}

	public Row marginLeft(float left) {
		ElementConfig.marginLeft(table, left);
		return this;
	}

	public Row marginRight(float right) {
		ElementConfig.marginRight(table, right);
		return this;
	}

	public Row paddingX(float x) {
		ElementConfig.paddingX(table, x);
		return this;
	}

	public Row paddingY(float y) {
		ElementConfig.paddingY(table, y);
		return this;
	}

	@Override
	public Row cellPaddingX(float x) {
		ElementConfig.marginX(table, x);
		return this;
	}

	@Override
	public Row cellPaddingY(float y) {
		ElementConfig.marginY(table, y);
		return this;
	}

	public Row x(float x) {
		ElementConfig.x(table, x);
		return this;
	}

	public Row y(float y) {
		ElementConfig.y(table, y);
		return this;
	}

	public Row position(float x, float y) {
		ElementConfig.position(table, x, y);
		return this;
	}

	public Row visible(boolean visible) {
		ElementConfig.visible(table, visible);
		return this;
	}

	public Row visible(@Nullable Readable<Boolean> visible) {
		ElementConfig.visible(table, visible);
		return this;
	}

	@Override
	public Row top() {
		ElementConfig.top(table);
		table.defaults().top();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.top();
		}
		return CellConfig.super.top();
	}

	@Override
	public Row bottom() {
		ElementConfig.bottom(table);
		table.defaults().bottom();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.bottom();
		}
		return CellConfig.super.bottom();
	}

	@Override
	public Row left() {
		ElementConfig.left(table);
		table.defaults().left();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.left();
		}
		return CellConfig.super.left();
	}

	@Override
	public Row right() {
		ElementConfig.right(table);
		table.defaults().right();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.right();
		}
		return CellConfig.super.right();
	}

	@Override
	public Row center() {
		ElementConfig.center(table);
		table.defaults().center();
		for (Cell<?> c : table.getCells()) {
			if (c != null) c.center();
		}
		return CellConfig.super.center();
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
		ElementConfig.background(table, bg);
		return this;
	}

	public Row draggable() {
		ElementConfig.draggable(table);
		return this;
	}

	public Row draggable(@Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
		ElementConfig.draggable(table, null, xSignal, ySignal);
		return this;
	}

	public Row draggable(@Nullable Hud hud) {
		ElementConfig.draggable(table, hud);
		return this;
	}

	public Row draggable(@Nullable Hud hud, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
		ElementConfig.draggable(table, hud, xSignal, ySignal);
		return this;
	}

	public Cell<?> add(Element e) {
		Cell<?> cell = table.add(e);
		respace();
		return cell;
	}
}
