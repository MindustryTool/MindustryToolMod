package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.core.Units.unit;
import static solim.modifier.Values.align;
import static solim.modifier.Values.buttonStyle;
import static solim.modifier.Values.center;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.enabled;
import static solim.modifier.Values.fontScale;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.icon;
import static solim.modifier.Values.left;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.marginLeft;
import static solim.modifier.Values.marginTop;
import static solim.modifier.Values.opacity;
import static solim.modifier.Values.style;
import static solim.modifier.Values.text;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.top;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.wrap;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.Scene;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.util.Align;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.display.Badge;
import solim.display.SolimImage;
import solim.display.Text;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Scroll;
import solim.modifier.ContainerCapability;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableCapability;
import solim.modifier.Prop;
import solim.modifier.Values;
import solim.overlay.Hud;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimCoreEnv;

class PropsDslTest extends SolimCoreEnv {

    @AfterEach
    void clear() {
        AttachmentStack.clear();
        OwnershipContext.clear();
    }

    private static Prop<ElementCapability, Float> width(float value) {
        return Prop.element(ElementCapability.class, "width", value, (element, v) -> element.setWidth(v != null ? v : 0f));
    }

    private static Prop<ElementCapability, Float> width(Signal<Float> source) {
        return Prop.elementSource(ElementCapability.class, "width", source,
                (element, v) -> element.setWidth(v != null ? v : 0f));
    }

    @Test
    void staticPropAppliesOnAnAlreadyRealizedHost() {
        Text text = new Text("hi");
        text.props(width(123f));

        assertEquals(123f, text.element().getWidth());
    }

    @Test
    void rawPropRunsAgainstTheRealizedTarget() {
        StringBuilder touched = new StringBuilder();
        Text text = new Text("hi");
        text.props(Prop.raw(ElementCapability.class, "touch", Prop.Kind.ELEMENT, target -> touched.append(target.getClass().getSimpleName())));

        assertEquals("Label", touched.toString());
    }

    @Test
    void reactivePropIsOwnedByTheHostAndDisposedWithIt() {
        Signal<Float> size = Signal.of(10f);
        Text text = new Text("hi");
        text.props(width(size));

        Element element = text.element();
        assertEquals(10f, element.getWidth());

        size.set(20f);
        SignalDispatcher.flush();
        assertEquals(20f, element.getWidth());

        text.dispose();
        size.set(30f);
        SignalDispatcher.flush();
        assertEquals(20f, element.getWidth(), "binding must stop once the host is disposed");
    }

    @Test
    void propsApplyAtMostOnce() {
        float[] calls = new float[1];
        Text text = new Text("hi");
        text.props(Prop.raw(ElementCapability.class, "count", Prop.Kind.ELEMENT, target -> calls[0]++));
        text.applyPendingProps();

        assertEquals(1f, calls[0], "applyPendingProps must not re-apply an already drained Props");
    }

    @Test
    void containerAppliesTableAndCellProps() {
        Column column = new Column();
        column.props(gap(9f), margin(4f));

        assertEquals(9f, column.gap(), "gap must reach the host table");
        assertEquals(4f, column.cellConfig().padTop.peek(),
                "margin must be recorded on the pending cell configuration");
    }

    @Test
    void childrenBlockRunsInsideAnOwnershipWindow() {
        boolean[] insideWindow = new boolean[1];
        Column column = new Column();

        column.children(() -> insideWindow[0] = (OwnershipContext.size() > 0));

        assertTrue(insideWindow[0], "children() must open an ownership window");
    }

    @Test
    void alignPropRunsTheCoordinatedActionsOfItsHost() {
        Column column = new Column();

        column.props(align(Align.center));

        assertEquals(Align.center, column.cellConfig().align,
                "align must be recorded on the pending cell configuration, not only on the table");
        assertEquals(Align.center, column.table().getAlign(), "align must reach the host table");
    }

    @Test
    void centerPropRunsTheCoordinatedActionsOfItsHost() {
        Column column = new Column();

        column.props(center());

        assertEquals(Align.center, column.cellConfig().align,
                "center must be recorded on the pending cell configuration");
        assertEquals(Align.center, column.table().getAlign(), "center must reach the host table");
    }

    @Test
    void gapPropWritesTheHostFieldAndRespaces() {
        Column column = new Column();

        column.props(gap(7f));

        assertEquals(7f, column.gap(), "gap must write the host field");
    }

    @Test
    void gapPropOnASignalRewritesTheHostField() {
        Signal<Float> size = Signal.of(4f);
        Column column = new Column();

        column.props(gap(size));
        assertEquals(4f, column.gap());

        size.set(11f);
        SignalDispatcher.flush();

        assertEquals(11f, column.gap(), "a reactive gap must rewrite the host field on every change");
    }

    @Test
    void buttonStylePropAppliesAStaticStyleThroughItsHost() {
        Button button = new Button();

        button.props(buttonStyle(builder -> builder.gap(9f)));

        assertEquals(9f, button.gap(), "a static style must be applied once without allocating an effect");
    }

    @Test
    void buttonStylePropRebindsReactivelyAndDiesWithItsHost() {
        Signal<Float> spacing = Signal.of(6f);
        Button button = new Button();

        button.props(buttonStyle(builder -> builder.gap(spacing)));
        assertEquals(6f, button.gap());

        spacing.set(18f);
        SignalDispatcher.flush();
        assertEquals(18f, button.gap(), "a non-static style must rebuild on every source change");

        button.dispose();
        spacing.set(30f);
        SignalDispatcher.flush();
        assertEquals(18f, button.gap(), "the conditionally allocated effect must die with the host Button");
    }

    @Test
    void colorPropReachesTheHostElementAndRebindsReactively() {
        Signal<Color> tint = Signal.of(Color.red);
        Text text = new Text("hi");

        text.props(color(tint));
        assertEquals(Color.red, text.element().color);

        tint.set(Color.blue);
        SignalDispatcher.flush();

        assertEquals(Color.blue, text.element().color);
    }

    @Test
    void textPropSetsLabelTextAndRebindsReactively() {
        Signal<String> content = Signal.of("first");
        Text text = new Text("initial");

        text.props(text(content));
        assertEquals("first", text.label().getText().toString());

        content.set("second");
        SignalDispatcher.flush();

        assertEquals("second", text.label().getText().toString());
    }

    @Test
    void buttonTextPropCreatesExactlyOneChildAndRebindsIt() {
        Signal<String> content = Signal.of("one");
        Button button = new Button();

        button.props(text(content));
        int childrenAfterFirstApply = button.button().getChildren().size;

        content.set("two");
        SignalDispatcher.flush();

        assertEquals(1, childrenAfterFirstApply, "a button text prop must create a single label child");
        assertEquals(1, button.button().getChildren().size,
                "re-binding must reuse the child instead of appending another one");
    }

    @Test
    void fontScalePropSetsTheHostLabel() {
        Signal<Float> scale = Signal.of(1.5f);
        Text text = new Text("hi");

        text.props(fontScale(scale));
        assertEquals(1.5f, text.label().getFontScaleX());

        scale.set(2f);
        SignalDispatcher.flush();

        assertEquals(2f, text.label().getFontScaleX());
    }

    @Test
    void wrapPropReachesTheHostLabel() {
        Text text = new Text("hi");

        text.props(wrap());

        assertTrue(text.isWrap(), "wrap must reach the host label");
    }

    @Test
    void stylePropReachesTheHostStyle() {
        Text text = new Text("hi");
        Label.LabelStyle labelStyle = new Label.LabelStyle(text.label().getStyle());
        labelStyle.background = new RoundedDrawable(4, Color.red);

        text.props(style(labelStyle));

        assertSame(labelStyle, text.label().getStyle());
        assertSame(labelStyle.background, text.label().getStyle().background);
    }

    @Test
    void enabledPropReachesTheHostButtonAndDiesWithIt() {
        Signal<Boolean> usable = Signal.of(true);
        Button button = new Button();

        button.props(enabled(usable));
        assertFalse(button.button().isDisabled());

        usable.set(false);
        SignalDispatcher.flush();
        assertTrue(button.button().isDisabled());

        button.dispose();
        usable.set(true);
        SignalDispatcher.flush();
        assertTrue(button.button().isDisabled(), "the binding must stop once the host Button is disposed");
    }

    @Test
    void checkedPropReachesTheHostButton() {
        Signal<Boolean> selection = Signal.of(true);
        Button button = new Button();

        button.props(checked(selection));

        assertTrue(button.button().isChecked());
    }

    @Test
    void iconPropCreatesExactlyOneChildAndRebindsIt() {
        Signal<Drawable> glyph = Signal.of(new RoundedDrawable(4, Color.red));
        Button button = new Button();

        button.props(icon(glyph));
        assertEquals(1, button.button().getChildren().size);

        glyph.set(new RoundedDrawable(8, Color.blue));
        SignalDispatcher.flush();

        assertEquals(1, button.button().getChildren().size,
                "re-binding must reuse the image child instead of appending another one");
    }

    @Test
    void singleWidgetHostsAcceptPropsWithoutATable() {
        Scene scene = newScene();
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = scene.getStyle(Label.LabelStyle.class).font;
        scene.addStyle(ButtonStyle.class, new ButtonStyle());
        scene.addStyle(Slider.SliderStyle.class, new Slider.SliderStyle());
        scene.addStyle(TextButton.TextButtonStyle.class, textButtonStyle);
        SolimTextField field = new SolimTextField(Signal.of(""));
        SolimSlider slider = new SolimSlider(Signal.of(5f), 0f, 10f, 1f);
        Switch toggle = new Switch(Signal.of(false));

        field.props(growX(), margin(unit(3)));
        slider.props(growX(), margin(unit(4)));
        toggle.props(growX(), margin(unit(5)));

        assertTrue(field.cellConfig().growX, "a widget without a table still records cell props");
        assertTrue(slider.cellConfig().growX);
        assertTrue(toggle.cellConfig().growX);
        assertFalse(TableCapability.class.isAssignableFrom(field.getClass()), "a widget that wraps a single Arc widget exposes no table");
        assertFalse(TableCapability.class.isAssignableFrom(slider.getClass()));
        assertFalse(TableCapability.class.isAssignableFrom(toggle.getClass()));
    }

    @Test
    void containerChildrenBlockOpensAWindow() {
        newScene();
        Column col = new Column();
        boolean[] insideWindow = new boolean[1];

        col.children(() -> insideWindow[0] = (OwnershipContext.size() > 0));

        assertTrue(insideWindow[0], "children() must open an ownership window");
        assertFalse(ContainerCapability.class.isAssignableFrom(SolimTextField.class),
                "leaf widgets must not implement ContainerCapability");
    }

    @Test
    void colorPropRunsTheCoordinatedActionsOfABadge() {
        Badge badge = new Badge("7");

        badge.props(color(Color.red));

        RoundedDrawable background = (RoundedDrawable) badge.table().getBackground();
        assertEquals(Color.red, badge.element().color);
        assertEquals(0.16f, background.getFillColor().a, 0.001f,
                "a Badge colour must tint its rounded background, not just set the element colour");
    }

    @Test
    void scrollHostAcceptsPropsAndCells() {
        Scroll scroll = new Scroll();

        scroll.props(gap(5f), growX());
        scroll.children(() -> {
        });

        assertTrue(scroll.cellConfig().growX, "a table-backed host records cell props before its block runs");
    }

    @Test
    void everyComponentFamilyAcceptsProps() {
        Divider divider = new Divider();
        SolimImage image = new SolimImage();
        Badge badge = new Badge("1");

        divider.props(growX(), margin(unit(1)));
        image.props(growX(), margin(unit(1)));
        badge.props(growX(), margin(unit(1)));

        assertTrue(divider.cellConfig().growX);
        assertTrue(image.cellConfig().growX);
        assertTrue(badge.cellConfig().growX);
    }

    @Test
    void alignPropsReachANonTableHost() {
        Text text = new Text("hi");

        text.props(center());

        assertEquals(Align.center, text.cellConfig().align, "align must reach the pending cell configuration");
        assertEquals(Align.center, text.label().getLabelAlign(), "align must reach the host label");
    }

    @Test
    void namedAlignPropsAccumulateRatherThanReplace() {
        Text text = new Text("hi");

        text.props(left(), top());

        assertEquals(Align.left | Align.top, text.label().getLabelAlign(),
                "a named align must clear only its opposite edge, not the whole alignment");
    }

    @Test
    void widthPropClampsAndInvalidates() {
        Text text = new Text("hi");

        text.props(Values.width(-5f));

        assertEquals(0f, text.element().getWidth(), "a negative width must clamp to zero");
    }

    @Test
    void visiblePropRebindsReactively() {
        Signal<Boolean> shown = Signal.of(true);
        Text text = new Text("hi");

        text.props(visible(shown));
        assertTrue(text.element().visible);

        shown.set(false);
        SignalDispatcher.flush();

        assertFalse(text.element().visible);
    }

    @Test
    void tooltipPropWithNullRemovesRatherThanThrows() {
        newScene();
        Text text = new Text("hi");

        text.props(tooltip((String) null));
        text.props(tooltip("tip"));
        text.props(tooltip((String) null));
    }

    @Test
    void opacityPropReachesBothHudTables() {
        Hud hud = new Hud();

        hud.props(opacity(0.25f));

        assertEquals(0.25f, hud.element().color.a, 0.001f);
        assertEquals(0.25f, hud.container().color.a, 0.001f,
                "a Hud opacity must fade the container as well as the root");
    }

    @Test
    void cellSizePropsRecordTheirSourceRatherThanAResolvedValue() {
        Column column = new Column();

        column.props(Values.prefWidth(120f), Values.minWidth(40f), Values.maxHeight(60f));

        PendingCellConfig config = column.cellConfig();
        assertEquals(120f, config.prefWidth.get());
        assertEquals(40f, config.minWidth.get());
        assertEquals(60f, config.maxHeight.get());
    }

    @Test
    void cellSizePropsAcceptAReactiveSource() {
        Signal<Float> width = Signal.of(90f);
        Column column = new Column();

        column.props(Values.prefWidth(width));

        assertEquals(90f, column.cellConfig().prefWidth.get(),
                "a reactive cell source must be stored as a source, not frozen at its first value");

        width.set(150f);
        SignalDispatcher.flush();

        assertEquals(150f, column.cellConfig().prefWidth.get());
    }

    @Test
    void marginEdgePropsRecordTheirSource() {
        Signal<Float> inset = Signal.of(6f);
        Column column = new Column();

        column.props(marginTop(4f), marginLeft(inset));

        PendingCellConfig config = column.cellConfig();
        assertEquals(4f, config.padTop.get());
        assertEquals(6f, config.padLeft.get());

        inset.set(11f);
        SignalDispatcher.flush();

        assertEquals(11f, config.padLeft.get());
    }

    @Test
    void recordedCellConstraintsReachTheAttachedCell() {
        Column column = new Column();
        Text[] holder = new Text[1];

        column.children(() -> holder[0] = new Text("child").props(Values.prefWidth(64f)));

        assertNotNull(column.table().getCell(holder[0].element()),
                "the child must be attached through the attachment pipeline");

        column.table().invalidateHierarchy();
        column.table().validate();

        assertEquals(64f, holder[0].element().getWidth(), 0.01f,
                "a prefWidth recorded by a prop must be honoured by the cell configurator");
    }

    @Test
    void fixedArityPropsOverloadsApplyCorrectly() {
        Column column = new Column();
        column.props(
                Values.growX(),
                Values.growY(),
                Values.gap(4f),
                Values.padding(8f),
                Values.top(),
                Values.left(),
                Values.minWidth(10f),
                Values.minHeight(12f),
                Values.maxWidth(100f),
                Values.maxHeight(120f)
        );

        PendingCellConfig config = column.cellConfig();
        assertTrue(config.growX);
        assertTrue(config.growY);
        assertEquals(4f, column.gap());
        assertEquals(10f, config.minWidth.get());
        assertEquals(12f, config.minHeight.get());
        assertEquals(100f, config.maxWidth.get());
        assertEquals(120f, config.maxHeight.get());
    }
}
