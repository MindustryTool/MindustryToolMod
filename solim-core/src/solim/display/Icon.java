package solim.display;

import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import java.util.function.Consumer;
import solim.signal.Effect;
import solim.signal.Signal;

/** Display widget for icon drawables. */
public final class Icon {
	private final Image image = new Image();
	private Effect binding;

	public Icon() {}

	public Icon(Drawable d) {
		image.setDrawable(d);
	}

	public static Icon of(Drawable d) {
		return new Icon(d);
	}

	public static Icon of(Signal<Drawable> s) {
		Icon icon = new Icon();
		Effect e = Effect.of((Consumer<Effect.Cleanup>) cleanup -> {
			icon.image.setDrawable(s.get());
		});
		icon.binding = e;
		return icon;
	}

	public Image image() {
		return image;
	}

	public void dispose() {
		if (binding != null) binding.dispose();
	}
}
