package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.ScrollPane;
import org.junit.jupiter.api.Test;
import solim.modifier.PendingCellConfig;
import solim.test.SolimEnv;

class SolimTokenTest extends SolimEnv {

    @Test
    void getOrCreateCreatesAndAssignsToken() {
        Element el = new Element();
        assertNull(el.userObject);

        SolimToken token = SolimToken.getOrCreate(el);
        assertNotNull(token);
        assertSame(token, el.userObject);
        assertSame(token, SolimToken.get(el));
    }

    @Test
    void getOrCreatePreservesExistingUserObjectInPayload() {
        Element el = new Element();
        Object legacyPayload = new Object();
        el.userObject = legacyPayload;

        SolimToken token = SolimToken.getOrCreate(el);
        assertSame(token, el.userObject);
        assertSame(legacyPayload, token.userPayload);
    }

    @Test
    void getOrCreateIdempotent() {
        Element el = new Element();
        SolimToken first = SolimToken.getOrCreate(el);
        SolimToken second = SolimToken.getOrCreate(el);

        assertSame(first, second);
    }

    @Test
    void getReturnsNullWhenNoToken() {
        assertNull(SolimToken.get(null));

        Element el = new Element();
        assertNull(SolimToken.get(el));

        el.userObject = "not-a-token";
        assertNull(SolimToken.get(el));
    }

    @Test
    void bindAttachesComponentAndConfig() {
        Element el = new Element();
        Component comp = new Component() {
            @Override
            public Element element() {
                return el;
            }
        };
        PendingCellConfig config = new PendingCellConfig();

        SolimToken.bind(el, comp, config);

        assertSame(comp, SolimToken.getComponent(el));
        SolimToken token = SolimToken.get(el);
        assertNotNull(token);
        assertSame(comp, token.component);
        assertSame(config, token.cellConfig);
    }

    @Test
    void setExpandingUpdatesFlagWithoutClobberingComponent() {
        Element el = new Element();
        Component comp = new Component() {
            @Override
            public Element element() {
                return el;
            }
        };
        SolimToken.bind(el, comp);

        assertFalse(SolimToken.isExpanding(el));

        SolimToken.setExpanding(el, true);
        assertTrue(SolimToken.isExpanding(el));
        // Component reference must not be destroyed by expanding flag
        assertSame(comp, SolimToken.getComponent(el));

        SolimToken.setExpanding(el, false);
        assertFalse(SolimToken.isExpanding(el));
        assertSame(comp, SolimToken.getComponent(el));
    }

    @Test
    void isExpandingChildMatchesLegacyHeuristics() {
        assertFalse(SolimToken.isExpandingChild(null));
        assertFalse(SolimToken.isExpandingChild(new Element()));

        Element tokenFlag = new Element();
        SolimToken.setExpanding(tokenFlag, true);
        assertTrue(SolimToken.isExpandingChild(tokenFlag));

        Element spacerName = new Element();
        spacerName.name = "spacer";
        assertTrue(SolimToken.isExpandingChild(spacerName));

        Element solimSpacer = new Element();
        solimSpacer.name = "solim-spacer-table";
        assertTrue(SolimToken.isExpandingChild(solimSpacer));

        Element fillParent = new Element();
        fillParent.fillParent = true;
        assertTrue(SolimToken.isExpandingChild(fillParent));

        assertTrue(SolimToken.isExpandingChild(new ScrollPane(new Element(), new ScrollPane.ScrollPaneStyle())));
    }
}
