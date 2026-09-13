package solim.input;

import arc.scene.ui.Slider;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.signal.Signal;

/** Slider widget bound to Signal&lt;Float&gt;. */
public final class SolimSlider implements Component {
	private final Slider slider = new Slider(0f, 1f, 0.1f, false);
	private Disposable binding;

	{
		slider.name = "solim-slider-slider";
	}

	public SolimSlider(Signal<Float> signal, float min, float max, float step) {
		slider.setRange(min, max);
		slider.setStepSize(step);
		slider.setValue(signal.peek());
		this.binding = new TwoWayBinding<>(
			signal,
			slider::getValue,
			slider::setValue,
			onChange -> {
				slider.changed(onChange::run);
				return () -> {};
			},
			(a, b) -> a != null && b != null && Math.abs(a - b) <= 0.0001f
		);
		ComponentContext.register(this);
	}

	public SolimSlider(Signal<Integer> signal, int min, int max, int step) {
		slider.setRange(min, max);
		slider.setStepSize(step);
		slider.setValue(signal.peek());
		this.binding = new TwoWayBinding<>(
			signal,
			() -> Math.round(slider.getValue()),
			val -> slider.setValue(val != null ? val : 0),
			onChange -> {
				slider.changed(onChange::run);
				return () -> {};
			},
			java.util.Objects::equals
		);
		ComponentContext.register(this);
	}

	public static SolimSlider of(Signal<Float> signal, float min, float max, float step) {
		return new SolimSlider(signal, min, max, step);
	}

	public static SolimSlider of(Signal<Integer> signal, int min, int max, int step) {
		return new SolimSlider(signal, min, max, step);
	}

	public Slider slider() {
		return slider;
	}

	@Override
	public Slider element() {
		return slider;
	}

	@Override
	public SolimSlider name(String name) {
		ElementConfig.name(slider, name);
		return this;
	}

	@Override
	public void dispose() {
		if (binding != null) {
			binding.dispose();
			binding = null;
		}
	}
}
