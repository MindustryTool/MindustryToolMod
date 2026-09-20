package solim;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.input.SolimSelect;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.reactive.Query;
import solim.reactive.Signal;

class ArcFacadeTest {

	@AfterEach
	void clear() {
		ParentStack.clear();
	}

	@Test
	void arcAttachesRawElement() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent test");
		Table root = new Table();
		ParentStack.push(root);
		Label label = UI.arc(new Label("hi"));
		ParentStack.pop();
		assertEquals("hi", label.getText().toString());
		assertTrue(root.getChildren().contains(label, true));
	}

	@Test
	void arcAttachesPlainElementHeadless() {
		Table root = new Table();
		ParentStack.push(root);
		Element el = UI.arc(new Element());
		ParentStack.pop();
		assertTrue(root.getChildren().contains(el, true));
	}

	@Test
	void arcReturnsSameInstance() {
		Element el = new Element();
		assertSame(el, UI.arc(el));
	}

	@Test
	void arcOutsideScopeIsNoOp() {
		Element el = UI.arc(new Element());
		assertNull(el.parent);
	}

    @Test
    void initRegistersSignalDispatcher() {
        UI.init();
        assertTrue(SignalDispatcher.isRegistered());
    }

    @Test
    void selectAttachesAndShowsCurrentValue() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent test");
        Table root = new Table();
        ParentStack.push(root);
        SolimSelect<String> s = UI.select(Signal.of("b"), Arrays.asList("a", "b"));
        ParentStack.pop();
        assertTrue(root.getChildren().contains(s.selectBox(), true));
        assertEquals("b", s.selectBox().getText().toString());
        s.dispose();
    }

    @Test
    void selectOutsideScopeIsDetached() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent test");
        SolimSelect<String> s = UI.select(Signal.of("a"), Arrays.asList("a", "b"));
        assertNull(s.selectBox().parent);
        s.dispose();
    }

    @Test
    void queryAttachedViaUIQueryRendersData() {
        Query<String> query = Query.of(() -> CompletableFuture.completedFuture("ui-query-data"));
        Table root = new Table();
        ParentStack.push(root);
        UI.query(query).data(d -> new Text(d));
        ParentStack.pop();

        SignalDispatcher.flush();

        assertEquals(1, root.getChildren().size);
        Element child = root.getChildren().first();
        assertTrue(child instanceof Table);
        Table qvTable = (Table) child;
        assertEquals(1, qvTable.getChildren().size);
        assertTrue(qvTable.getChildren().first() instanceof Label);
        assertEquals("ui-query-data", ((Label) qvTable.getChildren().first()).getText().toString());
    }
}
