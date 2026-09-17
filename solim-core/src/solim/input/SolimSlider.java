package solim.input;
import solim.reactive.TwoWayBinding;

import arc.scene.ui.Slider;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.reactive.Signal;
import java.util.Objects;

/** Slider widget bound to Signal&lt;Float&gt;. */
public final class SolimSlider implements Component, ElementConfig<SolimSlider>, CellConfig<SolimSlider> {
	private final Slider slider = new Slider(0f, 1f, 0.1f, false);
	private final PendingCellConfig constraints = new PendingCellConfig();
	private Disposable binding;
	private boolean disposed = false;

	@Override
	public PendingCellConfig cellConfig() {
		return constraints;
	}

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
			Objects::equals
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

	public SolimSlider name(String name) {
		slider.name = name;
		return this;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		if (binding != null) {
			binding.dispose();
			binding = null;
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

    @Override
    public SolimSlider self() {
        return this;
    }
}
