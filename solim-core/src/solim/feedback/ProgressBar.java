package solim.feedback;

import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import java.util.function.Consumer;
import solim.core.Disposable;
import solim.signal.Effect;
import solim.signal.Signal;

/** ProgressBar bound to Signal&lt;Float&gt; 0..1. */
public final class ProgressBar implements Disposable {
	private final Table bar = new Table();
	private final Signal<Float> signal;
	private Effect effect;
	private float current = 0f;

	public ProgressBar(Signal<Float> signal) {
		this.signal = signal;
		bar.top().left();
		updateBar();
		this.effect = Effect.of((Consumer<Effect.Cleanup>) cleanup -> {
			updateBar();
		});
	}

	public static ProgressBar of(Signal<Float> signal) {
		return new ProgressBar(signal);
	}

	private void updateBar() {
		float v = Math.max(0f, Math.min(1f, signal.get()));
		if (Math.abs(v - current) > 0.0001f) {
			current = v;
			bar.clearChildren();
			Image fill = new Image();
			fill.setScale(v, 1f);
			bar.add(fill).width(100f * v).height(8f);
		}
	}

	public Table bar() {
		return bar;
	}

	@Override
	public void dispose() {
		if (effect != null) effect.dispose();
	}
}
