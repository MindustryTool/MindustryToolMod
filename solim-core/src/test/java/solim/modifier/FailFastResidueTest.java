package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.style;

import arc.scene.ui.Label.LabelStyle;
import org.junit.jupiter.api.Test;
import solim.input.Button;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.SolimButtonStyle;
import solim.test.SolimEnv;

public class FailFastResidueTest extends SolimEnv {

    @Test
    void mismatchedStyleValueThrowsIllegalArgumentExceptionNamingHostAndValueClass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Button().props(style(Signal.of(new LabelStyle()))));
        assertTrue(ex.getMessage().contains(Button.class.getName()));
        assertTrue(ex.getMessage().contains(LabelStyle.class.getName()));
    }

    @Test
    void absentStyleValueIsNoOpWithoutThrowing() {
        Button button = new Button().props(style((Readable<SolimButtonStyle>) null));

        assertDoesNotThrow(button::element);
    }
}
