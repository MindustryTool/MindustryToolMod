package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.checked;

import arc.Core;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.Switch;
import solim.reactive.Signal;
import solim.test.SolimEnv;

public class MultiHostTraitTest extends SolimEnv {

    @Test
    void checkedTraitAppliesToButtonAndSwitch() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");

        Signal<Boolean> isChecked = Signal.of(false);

        Button button = new Button().props(checked(isChecked));
        Switch switchBox = new Switch(Signal.of(false)).props(checked(isChecked));

        button.element();
        switchBox.element();

        assertFalse(button.button().isChecked());
        assertFalse(switchBox.isChecked());

        isChecked.set(true);

        assertTrue(button.button().isChecked());
        assertTrue(switchBox.isChecked());

        isChecked.set(false);

        assertFalse(button.button().isChecked());
        assertFalse(switchBox.isChecked());
    }

    @Test
    void checkedTraitAppliesToCheckboxWhenSceneAvailable() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");

        Signal<Boolean> isChecked = Signal.of(false);
        Checkbox checkbox = new Checkbox("Check", false, null).props(checked(isChecked));
        checkbox.element();

        assertFalse(checkbox.isChecked());

        isChecked.set(true);
        assertTrue(checkbox.isChecked());

        isChecked.set(false);
        assertFalse(checkbox.isChecked());
    }
}
