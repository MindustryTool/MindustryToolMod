package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.AttachmentStack;
import solim.runtime.DebugAttachmentStack;
import solim.runtime.DebugReactiveContext;
import solim.runtime.DebugSignalDispatcher;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

/**
 * Verifies the {@code solim.debug.enabled} contract driven through {@link Perf#init()}: the property
 * is the only supported external trigger, it defaults to off, an unrecognized value is treated as
 * off, and {@code init()} is re-invocable.
 *
 * <p>The class-load path itself is deliberately not covered. {@code Perf} used to read the property
 * from a static initializer, which silently never ran because no shipped build referenced the
 * class; every test here imports {@code Perf}, so it could load it and the defect was invisible.
 * The invariant that {@code Main} calls {@code Perf.init()} is asserted by the corrected
 * {@code solim-debug-runtime} bootstrap scenario, not by an executable check.
 */
class PerfDebugFlagTest extends SolimEnv {

    private String previousProperty;

    @BeforeEach
    void captureProperty() {
        previousProperty = System.getProperty(Perf.debugProperty);
    }

    @AfterEach
    void restoreProperty() {
        if (previousProperty == null) {
            System.clearProperty(Perf.debugProperty);
        } else {
            System.setProperty(Perf.debugProperty, previousProperty);
        }
        Perf.setEnabled(false);
        Perf.setTracingEnabled(false);
    }

    @Test
    void propertyDefaultsToDisabledWhenAbsent() {
        System.clearProperty(Perf.debugProperty);

        assertFalse(Perf.debugEnabledByProperty(),
                "an absent property must leave debug mode off so published builds stay at zero cost");
    }

    @Test
    void explicitFalseDisablesDebugMode() {
        System.setProperty(Perf.debugProperty, "false");

        assertFalse(Perf.debugEnabledByProperty());
    }

    @Test
    void explicitTrueEnablesDebugMode() {
        System.setProperty(Perf.debugProperty, "true");

        assertTrue(Perf.debugEnabledByProperty());
    }

    @Test
    void trueIsMatchedIgnoringCase() {
        System.setProperty(Perf.debugProperty, "TRUE");

        assertTrue(Perf.debugEnabledByProperty(),
                "parseBoolean ignores case, matching the solim.mcp.* property convention");
    }

    @Test
    void unrecognizedValuesLeaveDebugModeOff() {
        // A typo'd value must fail closed rather than accidentally entering the debug path.
        for (String value : new String[]{"", " ", "yes", "1", "on", "enabled", "truthy"}) {
            System.setProperty(Perf.debugProperty, value);

            assertFalse(Perf.debugEnabledByProperty(),
                    "value '" + value + "' must not enable debug mode");
        }
    }

    @Test
    void propertyNameFollowsTheMcpConvention() {
        assertEquals("solim.debug.enabled", Perf.debugProperty);
    }

    @Test
    void initIsInertWhenPropertyIsAbsent() {
        System.clearProperty(Perf.debugProperty);
        Perf.setEnabled(false);

        Perf.init();

        assertFalse(Perf.isEnabled(), "an absent property must leave debug mode off");
        assertFalse(AttachmentStack.instance() instanceof DebugAttachmentStack);
        assertFalse(ReactiveContext.instance() instanceof DebugReactiveContext);
        assertFalse(SignalDispatcher.instance() instanceof DebugSignalDispatcher,
                "an absent property must leave the Ctrl+D poll uninstalled");
    }

    @Test
    void initIsInertWhenPropertyIsMalformed() {
        System.setProperty(Perf.debugProperty, "yes");
        Perf.setEnabled(false);

        Perf.init();

        assertFalse(Perf.isEnabled(),
                "a misspelled or typo'd flag value must fail closed, not enable debug mode");
    }

    @Test
    void initInstallsAllThreeDebugServicesWhenPropertyIsTrue() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(false);

        Perf.init();

        assertTrue(Perf.isEnabled());
        assertTrue(AttachmentStack.instance() instanceof DebugAttachmentStack);
        assertTrue(ReactiveContext.instance() instanceof DebugReactiveContext);
        assertTrue(SignalDispatcher.instance() instanceof DebugSignalDispatcher);
    }

    @Test
    void initArmsTheProfilingKeybindWhenPropertyIsTrue() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(false);

        Perf.init();

        assertTrue(((DebugSignalDispatcher) SignalDispatcher.instance()).isKeybindPolling(),
                "a property-driven enable must arm the Ctrl+D poll, otherwise the flag is useless");
    }

    @Test
    void initReEnablesAfterAnExplicitDisable() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(true);
        assertTrue(Perf.isEnabled());

        Perf.setEnabled(false);
        assertFalse(Perf.isEnabled());

        Perf.init();

        assertTrue(Perf.isEnabled(),
                "init must be re-invocable; a disabled runtime should be re-enableable by the flag");
    }

    @Test
    void initDoesNotDependOnPriorEnablement() {
        // The regression this class guards: Perf used to read the property from a static block,
        // which never ran because nothing in a shipped build referenced the class. init() is the
        // explicit trigger, so it must work from a fully clean state.
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(false);
        Perf.setTracingEnabled(false);

        Perf.init();

        assertTrue(Perf.isEnabled());
    }

    @Test
    void baseDispatcherPerformsNoProfilingKeybindPolling() {
        Perf.setEnabled(false);

        assertFalse(SignalDispatcher.instance() instanceof DebugSignalDispatcher,
                "the Ctrl+D poll lives only on DebugSignalDispatcher, so an absent property "
                        + "means no keybind polling at all");
    }

    @Test
    void enabledDispatcherPollsTheProfilingKeybind() {
        Perf.setEnabled(true);

        DebugSignalDispatcher dispatcher = (DebugSignalDispatcher) SignalDispatcher.instance();
        assertTrue(dispatcher.isKeybindPolling(),
                "enabling debug mode must arm the Ctrl+D poll");
    }

    @Test
    void disablingDebugModeDisarmsTheKeybindPoll() {
        Perf.setEnabled(true);
        DebugSignalDispatcher dispatcher = (DebugSignalDispatcher) SignalDispatcher.instance();

        Perf.setEnabled(false);

        assertFalse(dispatcher.isKeybindPolling(),
                "disabling must disarm the poll on the outgoing instance, not merely drop it");
    }

    @Test
    void explicitDisableOverridesPropertyDerivedState() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.init();
        assertTrue(Perf.isEnabled());

        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled(),
                "an explicit setEnabled(false) must fully override property-derived enablement");
        assertFalse(AttachmentStack.instance() instanceof DebugAttachmentStack);
    }
}