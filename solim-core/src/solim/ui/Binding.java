package solim.ui;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import java.util.function.Consumer;
import solim.runtime.ComponentContext;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Reactive property binding utilities for Arc Scene2D elements. Automatically registered with the
 * active ComponentContext when bound during build().
 */
public final class Binding {
	private Binding() {}

	public static <T> Effect of(Consumer<T> target, Signal<T> source) {
		Effect effect = Effect.of(() -> target.accept(source.get()));
		ComponentContext.register(effect);
		return effect;
	}

	public static <T> Effect of(Consumer<T> target, Computed<T> source) {
		Effect effect = Effect.of(() -> target.accept(source.get()));
		ComponentContext.register(effect);
		return effect;
	}

	public static <T> Effect bind(Signal<T> source, Consumer<T> target) {
		return of(target, source);
	}

	public static <T> Effect bind(Readable<T> source, Consumer<T> target) {
		Effect effect = Effect.of(() -> target.accept(source.get()));
		ComponentContext.register(effect);
		return effect;
	}

	public static Effect bindWidth(Element element, Readable<Float> width) {
		return bind(width, w -> {
			float val = Math.max(0f, w != null ? w : 0f);
			element.setWidth(val);
			element.invalidateHierarchy();
		});
	}

	public static Effect bindHeight(Element element, Readable<Float> height) {
		return bind(height, h -> {
			float val = Math.max(0f, h != null ? h : 0f);
			element.setHeight(val);
			element.invalidateHierarchy();
		});
	}

	public static Effect bindColor(Element element, Readable<Color> color) {
		return bind(color, c -> {
			if (c != null) {
				element.setColor(c);
			}
		});
	}

	public static Effect bindText(Label label, Readable<String> text) {
		return bind(text, t -> {
			label.setText(t != null ? t : "");
		});
	}

	public static Effect bindVisible(Element element, Readable<Boolean> visible) {
		return bind(visible, v -> {
			element.visible = Boolean.TRUE.equals(v);
		});
	}

	public static Effect bindDisabled(Button button, Readable<Boolean> disabled) {
		return bind(disabled, d -> {
			button.setDisabled(Boolean.TRUE.equals(d));
		});
	}
}
