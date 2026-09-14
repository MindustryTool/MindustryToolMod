package solim.layout;

import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.style.Drawable;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.ui.Ui;

/**
 * Clickable and stylable card container component with support for inner children, reactive
 * width/height/color bindings, and click event bubbling control.
 */
public final class Card implements Component, CellConfig<Card>, ElementConfig<Card>, TableConfig<Card>, GapContainer {

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

	private final Button cardButton;
	private final Table container = new Table();
	private final solim.modifier.PendingCellConfig constraints = new solim.modifier.PendingCellConfig();
	private final List<Disposable> bindings = new ArrayList<>();
	private float gap = 0f;
	private @Nullable Runnable onClick;

	public Card() {
		this(new Button.ButtonStyle());
	}

	public Card(Drawable background) {
		Button.ButtonStyle style = new Button.ButtonStyle();
		if (background != null) {
			style.up = background;
		}
		this.cardButton = new Button(style);
		this.cardButton.userObject = this;
		this.cardButton.name = "solim-card-cardButton";
		this.container.name = "solim-card-container";
		this.container.userObject = this;
		this.cardButton.top().left();
		this.container.top().left();
		this.cardButton.add(container).grow().top().left();
	}

	public Card(Button.ButtonStyle style) {
		this.cardButton = new Button(style != null ? style : new Button.ButtonStyle());
		this.cardButton.userObject = this;
		this.cardButton.name = "solim-card-cardButton";
		this.container.name = "solim-card-container";
		this.container.userObject = this;
		this.cardButton.top().left();
		this.container.top().left();
		this.cardButton.add(container).grow().top().left();
	}

	public static Card of(Runnable children) {
		return new Card();
	}

	public static Card of(Button.ButtonStyle style, Runnable children) {
		return new Card(style);
	}

	public Table container() {
		return container;
	}

	@Override
	public Table table() {
		return container;
	}

	@Override
	public solim.graphics.RoundedDrawable getOrCreateRounded(int defaultRadius) {
		return solim.modifier.RoundedHelper.getOrCreateRounded(cardButton, defaultRadius);
	}

	@Override
	public Card background(@Nullable Drawable bg) {
		cardButton.setBackground(bg);
		if (cardButton.getStyle() != null) {
			cardButton.getStyle().up = bg;
		}
		return this;
	}

	@Override
	public Card background(@Nullable Color color) {
		if (color == null || color.a == 0f) {
			return background((Drawable) null);
		}
		return rounded(0, color);
	}

	public Button cardButton() {
		return cardButton;
	}

	@Override
	public Element element() {
		return cardButton;
	}

	@Override
	public solim.modifier.PendingCellConfig sizeConstraints() {
		return constraints;
	}

	public Card color(Color color) {
		if (color != null) {
			cardButton.setColor(color);
		}
		return this;
	}

	public Card color(Readable<Color> color) {
		if (color != null) {
			Effect e = Effect.of(() -> {
				Color c = color.get();
				if (c != null) {
					cardButton.setColor(c);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Card children(@Nullable Runnable r) {
		ParentStack.push(container, ATTACHER);
		try {
			if (r != null) {
				r.run();
			}
		} finally {
			ParentStack.pop();
		}
		ParentStack.attachToParent(cardButton);
		respace();
		return this;
	}


	public Card gap(float g) {
		this.gap = g;
		respace();
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
		GapContainer.applySpacing(container, Direction.VERTICAL, gap);
	}

	public Card style(Button.ButtonStyle style) {
		if (style != null) {
			cardButton.setStyle(style);
		}
		return this;
	}

	public Card style(@Nullable Readable<? extends Button.ButtonStyle> style) {
		if (style != null) {
			Effect e = Effect.of(() -> {
				Button.ButtonStyle s = style.get();
				if (s != null) {
					style(s);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Card onClick(Runnable onClick) {
		this.onClick = onClick;
		if (onClick != null) {
			cardButton.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
					if (cardButton.getScene() == null) return false;
					return super.touchDown(event, x, y, pointer, button);
				}

				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (event != null && event.stopped) {
						return;
					}
					if (Card.this.onClick != null) {
						try {
							Card.this.onClick.run();
						} catch (Exception e) {
							Log.err("Error executing card onClick", e);
						}
					}
				}
			});
		}
		return this;
	}


	@Override
	public void dispose() {
		for (Disposable d : bindings) {
			d.dispose();
		}
		bindings.clear();
	}
}
