package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.layout.Divider;
import solim.layout.Scroll;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;
import solim.test.SolimEnv;

public class SemanticTraitRejectionTest extends SolimEnv {

    @Test
    void traitMarkersExtendElementCapability() {
        assertTrue(ElementCapability.class.isAssignableFrom(TextCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(LabelCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(ButtonCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(CheckableCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(WrappableCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(FieldCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(SliderCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(SelectCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(ScrollCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(VideoCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(IconCapability.class));
    }

    @Test
    void traitRequirementRejectsIncompatibleHostTypes() {
        assertFalse(LabelCapability.class.isAssignableFrom(Button.class));
        assertFalse(LabelCapability.class.isAssignableFrom(Checkbox.class));
        assertFalse(LabelCapability.class.isAssignableFrom(Divider.class));

        assertFalse(ButtonCapability.class.isAssignableFrom(Text.class));
        assertFalse(ButtonCapability.class.isAssignableFrom(SolimTextField.class));
        assertFalse(ButtonCapability.class.isAssignableFrom(Divider.class));

        assertFalse(CheckableCapability.class.isAssignableFrom(Text.class));
        assertFalse(CheckableCapability.class.isAssignableFrom(Divider.class));
        assertFalse(CheckableCapability.class.isAssignableFrom(SolimSlider.class));

        assertFalse(WrappableCapability.class.isAssignableFrom(Divider.class));
        assertFalse(WrappableCapability.class.isAssignableFrom(SolimSlider.class));
        assertFalse(WrappableCapability.class.isAssignableFrom(Scroll.class));

        assertFalse(FieldCapability.class.isAssignableFrom(Button.class));
        assertFalse(FieldCapability.class.isAssignableFrom(Text.class));
        assertFalse(FieldCapability.class.isAssignableFrom(Scroll.class));

        assertFalse(SliderCapability.class.isAssignableFrom(Text.class));
        assertFalse(SliderCapability.class.isAssignableFrom(Button.class));

        assertFalse(ScrollCapability.class.isAssignableFrom(Button.class));
        assertFalse(ScrollCapability.class.isAssignableFrom(Text.class));

        assertFalse(VideoCapability.class.isAssignableFrom(Button.class));
        assertFalse(VideoCapability.class.isAssignableFrom(Text.class));

        assertFalse(IconCapability.class.isAssignableFrom(Divider.class));
        assertFalse(IconCapability.class.isAssignableFrom(Text.class));
    }

    @Test
    void typedStyleOverloadsResolveFromStaticTypeAndBindMatchingCapability() {
        Prop<LabelCapability, LabelStyle> labelStyleProp = Values.style(new LabelStyle());
        assertEquals(LabelCapability.class, labelStyleProp.requires());

        SolimButtonStyle solimBtnStyle = new SolimButtonStyleBuilder().build();
        Prop<ButtonCapability, SolimButtonStyle> solimBtnProp = Values.style(solimBtnStyle);
        assertEquals(ButtonCapability.class, solimBtnProp.requires());

        ButtonStyle arcBtnStyle = new ButtonStyle();
        Prop<ButtonCapability, ButtonStyle> arcBtnProp = Values.style(arcBtnStyle);
        assertEquals(ButtonCapability.class, arcBtnProp.requires());

        TextFieldStyle fieldStyle = new TextFieldStyle();
        Prop<FieldCapability, TextFieldStyle> fieldProp = Values.style(fieldStyle);
        assertEquals(FieldCapability.class, fieldProp.requires());

        InputStyle inputStyle = InputStyle.builder().build();
        Prop<FieldCapability, InputStyle> inputProp = Values.style(inputStyle);
        assertEquals(FieldCapability.class, inputProp.requires());

        ScrollPaneStyle scrollStyle = new ScrollPaneStyle();
        Prop<ScrollCapability, ScrollPaneStyle> scrollProp = Values.style(scrollStyle);
        assertEquals(ScrollCapability.class, scrollProp.requires());

        SliderStyle sliderStyle = new SliderStyle();
        Prop<SliderCapability, SliderStyle> sliderProp = Values.style(sliderStyle);
        assertEquals(SliderCapability.class, sliderProp.requires());
    }

    @Test
    void semanticPropsBindRequiredTraits() {
        assertEquals(CheckableCapability.class, Values.checked(false).requires());
        assertEquals(IconCapability.class, Values.icon((Drawable) null).requires());
        assertEquals(WrappableCapability.class, Values.wrap().requires());
        assertEquals(WrappableCapability.class, Values.ellipsis().requires());
        assertEquals(WrappableCapability.class, Values.fontScale(1.5f).requires());
        assertEquals(TextCapability.class, Values.text("test").requires());
        assertEquals(IconCapability.class, Values.url("https://example.com/asset.png").requires());
        assertEquals(IconCapability.class, Values.placeholder((Drawable) null).requires());
        assertEquals(IconCapability.class, Values.fallback(null).requires());
        assertEquals(VideoCapability.class, Values.autoplay(true).requires());
    }
}
