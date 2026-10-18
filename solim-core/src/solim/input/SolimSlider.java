package solim.input;

import solim.reactive.TwoWayBinding;

import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.SliderCapability;
import solim.reactive.Signal;
import java.util.Objects;

public final class SolimSlider extends Component
        implements Configurable<SolimSlider>, ElementCapability, CellCapability, AlignmentCapability, SliderCapability {
    private final Slider slider;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimSlider> pending;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public @Nullable Props<SolimSlider> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimSlider> props) {
        this.pending = props;
    }

    public SolimSlider(Signal<Float> signal, float min, float max, float step) {
        this(new Slider(0f, 1f, 0.1f, false), signal, min, max, step);
    }

    private SolimSlider(Slider slider, Signal<Float> signal, float min, float max, float step) {
        super(slider);
        this.slider = slider;
        slider.name = "solim-slider-slider";
        slider.setRange(min, max);
        slider.setStepSize(step);
        slider.setValue(signal.peek());
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    slider::getValue,
                    slider::setValue,
                    onChange -> {
                        slider.changed(onChange::run);
                        return DisposableAction.empty();
                    },
                    (a, b) -> a != null && b != null && Math.abs(a - b) <= 0.0001f);
        } finally {
            OwnershipContext.pop();
        }
    }

    public SolimSlider(Signal<Integer> signal, int min, int max, int step) {
        this(new Slider(0f, 1f, 0.1f, false), signal, min, max, step);
    }

    private SolimSlider(Slider slider, Signal<Integer> signal, int min, int max, int step) {
        super(slider);
        this.slider = slider;
        slider.name = "solim-slider-slider";
        slider.setRange(min, max);
        slider.setStepSize(step);
        slider.setValue(signal.peek());
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    () -> Math.round(slider.getValue()),
                    val -> slider.setValue(val != null ? val : 0),
                    onChange -> {
                        slider.changed(onChange::run);
                        return DisposableAction.empty();
                    },
                    Objects::equals);
        } finally {
            OwnershipContext.pop();
        }
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

    public SolimSlider name(String name) {
        setName(name);
        return this;
    }

    public void setStyle(@Nullable Slider.SliderStyle style) {
        if (style != null) {
            slider.setStyle(style);
        }
    }

    @Override
    public SolimSlider self() {
        return this;
    }
}
