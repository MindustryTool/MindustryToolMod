package solim.layout;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementModifiers;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.runtime.ParentStack;
import solim.ui.Ui;

/** Column layout — vertical Table wrapper. */
public final class Column implements Component, LayoutModifiers<Column>, GapContainer {

	public static final ParentStack.Attacher ATTACHER = (table, child) -> {
		Cell<?> cell = table.add(child);
		if (Ui.isExpanding(child)) {
			cell.growY();
		}
		cell.row();
		if (table.userObject instanceof GapContainer) {
			GapContainer gc = (GapContainer) table.userObject;
			GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
		}
		return cell;
	};

	private final Table table;
	private final SizeConstraints constraints = new SizeConstraints();
	private float gap = 0f;

	public Column() {
		this.table = new Table();
		this.table.userObject = this;
		this.table.name = "solim-column-table";
		this.table.top().left();
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

	public Column name(String name) {
		ElementModifiers.name(table, name);
		return this;
	}

	public Column fillParent(boolean fillParent) {
		table.setFillParent(fillParent);
		return this;
	}

	public Column fillParent() {
		return fillParent(true);
	}

	public Column touchable(Touchable touchable) {
		table.touchable = touchable;
		return this;
	}

	public Column gap(float g) {
		this.gap = g;
		respace();
		return this;
	}

	public Column gap(@Nullable Readable<Float> gapSignal) {
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
		return Direction.VERTICAL;
	}

	@Override
	public float gap() {
		return gap;
	}

	@Override
	public void respace() {
		GapContainer.applySpacing(table, Direction.VERTICAL, gap);
	}

	public Column padding(float p) {
		ElementModifiers.padding(table, p);
		return this;
	}

	public Column padding(float top, float left, float bottom, float right) {
		ElementModifiers.padding(table, top, left, bottom, right);
		return this;
	}

	public Column paddingTop(float top) {
		ElementModifiers.paddingTop(table, top);
		return this;
	}

	public Column paddingBottom(float bottom) {
		ElementModifiers.paddingBottom(table, bottom);
		return this;
	}

	public Column paddingLeft(float left) {
		ElementModifiers.paddingLeft(table, left);
		return this;
	}

	public Column paddingRight(float right) {
		ElementModifiers.paddingRight(table, right);
		return this;
	}

	public Column margin(float m) {
		ElementModifiers.margin(table, m);
		return this;
	}

	public Column margin(float top, float left, float bottom, float right) {
		ElementModifiers.margin(table, top, left, bottom, right);
		return this;
	}

	public Column marginTop(float top) {
		ElementModifiers.marginTop(table, top);
		return this;
	}

	public Column marginBottom(float bottom) {
		ElementModifiers.marginBottom(table, bottom);
		return this;
	}

	public Column marginLeft(float left) {
		ElementModifiers.marginLeft(table, left);
		return this;
	}

	public Column marginRight(float right) {
		ElementModifiers.marginRight(table, right);
		return this;
	}

	public Column paddingX(float x) {
		ElementModifiers.paddingX(table, x);
		return this;
	}

	public Column paddingY(float y) {
		ElementModifiers.paddingY(table, y);
		return this;
	}

	@Override
	public Column marginX(float x) {
		ElementModifiers.marginX(table, x);
		return this;
	}

	@Override
	public Column marginY(float y) {
		ElementModifiers.marginY(table, y);
		return this;
	}

	public Column x(float x) {
		ElementModifiers.x(table, x);
		return this;
	}

	public Column y(float y) {
		ElementModifiers.y(table, y);
		return this;
	}

	public Column position(float x, float y) {
		ElementModifiers.position(table, x, y);
		return this;
	}

	public Column visible(boolean visible) {
		ElementModifiers.visible(table, visible);
		return this;
	}

	public Column visible(@Nullable Readable<Boolean> visible) {
		ElementModifiers.visible(table, visible);
		return this;
	}

	@Override
	public Column top() {
		ElementModifiers.top(table);
		return LayoutModifiers.super.top();
	}

	@Override
	public Column bottom() {
		ElementModifiers.bottom(table);
		return LayoutModifiers.super.bottom();
	}

	@Override
	public Column left() {
		ElementModifiers.left(table);
		return LayoutModifiers.super.left();
	}

	@Override
	public Column right() {
		ElementModifiers.right(table);
		return LayoutModifiers.super.right();
	}

	@Override
	public Column center() {
		ElementModifiers.center(table);
		return LayoutModifiers.super.center();
	}

	public Column align(Align a) {
		switch (a) {
			case START:
				table.top();
				break;
			case CENTER:
				table.center();
				break;
			case END:
				table.bottom();
				break;
			case STRETCH:
				table.top();
				break;
			default:
				break;
		}
		return this;
	}

	public Column children(@Nullable Runnable r) {
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

	public Cell<?> add(Element e) {
		Cell<?> cell = table.add(e);
		respace();
		return cell;
	}
}
