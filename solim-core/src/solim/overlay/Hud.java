package solim.overlay;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import mindustry.game.EventType.ResizeEvent;
import solim.core.Component;
import solim.core.Disposable;
import solim.layout.CellConfig;
import solim.layout.Row;
import solim.layout.SizeConstraints;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Floating non-modal HUD overlay component.
 * Root element defaults to {@link Touchable#childrenOnly} so background touches pass through.
 * Content children are nested within an inner enabled container.
 * Automatically adapts to screen resize events via {@link #keepInScreen()}.
 */
public class Hud implements Component, CellConfig<Hud> {

	private final Table root;
	private final Table container;
	private final SizeConstraints constraints = new SizeConstraints();
	private final List<Disposable> bindings = new ArrayList<>();
	private final Cons<ResizeEvent> resizeListener;
	private @Nullable Signal<Float> boundXSignal;
	private @Nullable Signal<Float> boundYSignal;
	private boolean disposed = false;

	public static class HudRootTable extends Table {
		private final Hud hud;

		public HudRootTable(Hud hud) {
			this.hud = hud;
		}

		@Override
		public void validate() {
			if (needsLayout()) {
				float pw = getPrefWidth();
				float ph = getPrefHeight();
				if (pw > 0f && ph > 0f && (Math.abs(getWidth() - pw) > 1.0f || Math.abs(getHeight() - ph) > 1.0f)) {
					setSize(pw, ph);
					if (hud != null) {
						hud.keepInScreen();
					}
				}
			}
			super.validate();
		}
	}

	public Hud() {
		this.root = new HudRootTable(this);
		this.root.name = "solim-hud-root";
		this.root.touchable = Touchable.childrenOnly;
        this.root.toFront();

		this.root.userObject = this;

		this.container = new Table();
		this.container.name = "solim-hud-container";
		this.container.touchable = Touchable.enabled;
		this.container.userObject = this;

		this.root.add(container).pad(0).margin(0);

		this.resizeListener = e -> {
			keepInScreen();
			Core.app.post(this::keepInScreen);
		};
		Events.on(ResizeEvent.class, resizeListener);

		ComponentContext.register(this);
	}

	@Override
	public Element element() {
		return root;
	}

	public Table root() {
		return root;
	}

	public Table container() {
		return container;
	}

	@Override
	public SizeConstraints sizeConstraints() {
		return constraints;
	}

	public Hud name(String name) {
		ElementConfig.name(root, name);
		return this;
	}

	public Hud touchable(Touchable touchable) {
		root.touchable = touchable;
		return this;
	}

	public Hud containerTouchable(Touchable touchable) {
		container.touchable = touchable;
		return this;
	}

	public Hud toFrontOnTouch() {
		root.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
				root.toFront();
				return false;
			}
		});
		return this;
	}

	public Hud mount(@Nullable Group parent) {
		if (parent != null) {
			parent.addChild(root);
		}
		return this;
	}

	public Hud mountToScene() {
		if (Core.scene != null) {
			Core.scene.add(root);
		}
		return this;
	}

	public static @Nullable Hud find(@Nullable Element element) {
		Element cur = element;
		while (cur != null) {
			if (cur.userObject instanceof Hud) {
				return (Hud) cur.userObject;
			}
			cur = cur.parent;
		}
		Table t = ParentStack.find(table -> table != null && table.userObject instanceof Hud);
		if (t != null && t.userObject instanceof Hud) {
			return (Hud) t.userObject;
		}
		return null;
	}

	public Hud background(@Nullable Drawable bg) {
		container.background(bg);
		return this;
	}

	public Hud background(@Nullable Readable<Drawable> bg) {
		if (bg != null) {
			Effect e = Effect.of(() -> container.background(bg.get()));
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Hud children(@Nullable Runnable r) {
		ParentStack.push(container, Row.ATTACHER);
		try {
			if (r != null) {
				r.run();
			}
		} finally {
			ParentStack.pop();
		}
		root.pack();
		return this;
	}

	public void bindXSignal(@Nullable Signal<Float> xSignal) {
		this.boundXSignal = xSignal;
	}

	public void bindYSignal(@Nullable Signal<Float> ySignal) {
		this.boundYSignal = ySignal;
	}

	public Hud x(float x) {
		ElementConfig.x(root, x);
		return this;
	}

	public Hud x(Readable<Float> x) {
		if (x instanceof Signal) {
			this.boundXSignal = (Signal<Float>) x;
		}
		if (x != null) {
			Effect e = Effect.of(() -> {
				Float v = x.get();
				if (v != null) {
					ElementConfig.x(root, v);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Hud y(float y) {
		ElementConfig.y(root, y);
		return this;
	}

	public Hud y(Readable<Float> y) {
		if (y instanceof Signal) {
			this.boundYSignal = (Signal<Float>) y;
		}
		if (y != null) {
			Effect e = Effect.of(() -> {
				Float v = y.get();
				if (v != null) {
					ElementConfig.y(root, v);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Hud position(float x, float y) {
		ElementConfig.position(root, x, y);
		return this;
	}

	public Hud position(Readable<Float> x, Readable<Float> y) {
		x(x);
		y(y);
		return this;
	}

	public Hud opacity(float opacity) {
		ElementConfig.opacity(container, opacity);
		return this;
	}

	public Hud opacity(Readable<Float> opacity) {
		if (opacity != null) {
			Effect e = Effect.of(() -> {
				Float v = opacity.get();
				if (v != null) {
					ElementConfig.opacity(container, v);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Hud alpha(float alpha) {
		return opacity(alpha);
	}

	public Hud alpha(Readable<Float> alpha) {
		return opacity(alpha);
	}

	public Hud scale(float s) {
		container.setScale(s);
		return this;
	}

	public Hud scale(Readable<Float> s) {
		if (s != null) {
			Effect e = Effect.of(() -> {
				Float v = s.get();
				if (v != null) {
					container.setScale(v);
					root.pack();
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public Hud draggable(Element handle) {
		ElementConfig.draggable(handle, this);
		return this;
	}

	public Hud draggable(Element handle, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
		if (xSignal != null) this.boundXSignal = xSignal;
		if (ySignal != null) this.boundYSignal = ySignal;
		ElementConfig.draggable(handle, this, xSignal, ySignal);
		return this;
	}

	public void keepInScreen() {
		float scl = arc.scene.ui.layout.Scl.scl();
		float sw = Core.scene != null ? Core.scene.getWidth() : (Core.graphics != null ? Core.graphics.getWidth() / (scl > 0f ? scl : 1f) : 0f);
		float sh = Core.scene != null ? Core.scene.getHeight() : (Core.graphics != null ? Core.graphics.getHeight() / (scl > 0f ? scl : 1f) : 0f);
		if (sw <= 0f || sh <= 0f) return;

		if (root.getWidth() <= 0f || root.getHeight() <= 0f) {
			root.pack();
		}

		float w = root.getWidth();
		float h = root.getHeight();

		float curX = root.x;
		float curY = root.y;

		if (curX + w > sw) curX = Math.max(0, sw - w);
		if (curX < 0) curX = 0;
		if (curY + h > sh) curY = Math.max(0, sh - h);
		if (curY < 0) curY = 0;

		root.setPosition(curX, curY);

		if (boundXSignal != null && (boundXSignal.get() == null || Math.abs(boundXSignal.get() - curX) > 0.5f)) {
			boundXSignal.set(curX);
		}
		if (boundYSignal != null && (boundYSignal.get() == null || Math.abs(boundYSignal.get() - curY) > 0.5f)) {
			boundYSignal.set(curY);
		}
	}

	public void pack() {
		root.pack();
	}

	@Override
	public void dispose() {
		if (disposed) return;
		disposed = true;
		Events.remove(ResizeEvent.class, resizeListener);
		for (Disposable d : bindings) {
			d.dispose();
		}
		bindings.clear();
	}
}
