package solim.display;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class BadgeTest extends SolimEnv {

    @BeforeAll
    static void checkArcContext() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
    }

    @Test
    void staticAndReactiveTextBadge() {
        Badge staticBadge = new Badge("MOD");
        assertEquals("MOD", staticBadge.text().label().getText().toString());
        staticBadge.dispose();

        Signal<String> tag = Signal.of("VIP");
        Badge reactiveBadge = new Badge(tag);
        assertEquals("VIP", reactiveBadge.text().label().getText().toString());

        tag.set("ADMIN");
        assertEquals("ADMIN", reactiveBadge.text().label().getText().toString());
        reactiveBadge.dispose();
    }

    @Test
    void counterBadgeHideOnZero() {
        Signal<Integer> unread = Signal.of(3);
        Badge badge = Badge.ofCount(unread);

        assertTrue(badge.element().visible);
        assertEquals("3", badge.text().label().getText().toString());

        unread.set(0);
        assertFalse(badge.element().visible);
        assertEquals("0", badge.text().label().getText().toString());

        unread.set(10);
        assertTrue(badge.element().visible);
        assertEquals("10", badge.text().label().getText().toString());

        badge.props(hideOnZero(false));
        unread.set(0);
        assertTrue(badge.element().visible);

        badge.dispose();
    }

    @Test
    void customColorModifiers() {
        Badge badge = new Badge("TAG")
                .props(color(Color.royal), textColor(Color.white));

        assertEquals(Color.royal, badge.table().color);
        badge.dispose();
    }
}
