package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.instrumentation.Instrumentation;
import solim.test.SolimEnv;

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
        for (String value : new String[] { "", " ", "yes", "1", "on", "enabled", "truthy" }) {
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
        assertFalse(Instrumentation.enabled(), "no sink may be installed without the property");
        assertNull(Instrumentation.sink());
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
    void initInstallsAnInstrumentationSinkWhenPropertyIsTrue() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(false);

        Perf.init();

        assertTrue(Perf.isEnabled());
        assertTrue(Instrumentation.enabled());
        assertSame(Perf.sink(), Instrumentation.sink(),
                "Perf must consume events as a subscriber, never substitute a runtime service");
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
        System.setProperty(Perf.debugProperty, "true");
        Perf.setEnabled(false);
        Perf.setTracingEnabled(false);

        Perf.init();

        assertTrue(Perf.isEnabled());
    }

    @Test
    void disabledPerfLeavesNoSinkInstalled() {
        Perf.setEnabled(false);

        assertFalse(Instrumentation.enabled(),
                "with no sink installed the emit branch is constant-folded away at every call site");
    }

    @Test
    void enablingPerfInstallsExactlyOneSink() {
        Perf.setEnabled(true);
        Object first = Instrumentation.sink();

        Perf.setEnabled(true);

        assertSame(first, Instrumentation.sink(), "re-enabling must not stack duplicate subscribers");
    }

    @Test
    void explicitDisableOverridesPropertyDerivedState() {
        System.setProperty(Perf.debugProperty, "true");
        Perf.init();
        assertTrue(Perf.isEnabled());

        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled(),
                "an explicit setEnabled(false) must fully override property-derived enablement");
        assertFalse(Instrumentation.enabled());
    }
}