package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.core.OwnershipContext;

class AttachmentStackCaptureTest {

	/** A component that announces itself to the ambient scope, the way every real component does. */
	static class RecordingComponent extends Component {
		final String id;

		RecordingComponent(String id) {
			super(new Element());
			this.id = id;
		}
	}

	/** A component that is not attachable, standing in for one that only joins the ownership scope. */
	static final class ContextOnlyComponent extends Component {
		ContextOnlyComponent() {
			super(new Element());
		}

		@Override
		protected boolean attachesToParent() {
			return false;
		}
	}

	@AfterEach
	void clear() {
		AttachmentStack.clear();
	}

	@Test
	void captureReturnsComponentsInCreationOrder() {
		List<Component> captured = AttachmentStack.capture(() -> {
			new RecordingComponent("A");
			new RecordingComponent("B");
			new RecordingComponent("C");
		});
		assertEquals(3, captured.size());
		assertEquals("A", ((RecordingComponent) captured.get(0)).id);
		assertEquals("B", ((RecordingComponent) captured.get(1)).id);
		assertEquals("C", ((RecordingComponent) captured.get(2)).id);
		for (Component c : captured) {
			c.dispose();
		}
	}

	@Test
	void captureDoesNotAttachToAnyParent() {
		Table outer = new Table();
		AttachmentStack.push(outer);
		List<Component> captured = AttachmentStack.capture(() -> new RecordingComponent("A"));
		assertEquals(1, captured.size());
		assertEquals(0, outer.getChildren().size, "Captured component must not attach to the outer parent");
		assertSame(outer, AttachmentStack.current(), "Outer stack entry must be restored after capture");
		AttachmentStack.pop();
		for (Component c : captured) {
			c.dispose();
		}
	}

	@Test
	void captureOfEmptyBlockReturnsEmptyList() {
		List<Component> captured = AttachmentStack.capture(() -> {
		});
		assertTrue(captured.isEmpty());
	}

	@Test
	void captureCatchesContextOnlyComponents() {
		List<Component> captured = AttachmentStack.capture(() -> {
			new ContextOnlyComponent();
			new RecordingComponent("A");
			new ContextOnlyComponent();
		});
		assertEquals(3, captured.size(),
				"Components registering only via OwnershipContext (Row/Column style) must be captured");
		assertInstanceOf(ContextOnlyComponent.class, captured.get(0));
		assertInstanceOf(RecordingComponent.class, captured.get(1));
		assertInstanceOf(ContextOnlyComponent.class, captured.get(2));
		for (Component c : captured) {
			c.dispose();
		}
	}

	@Test
	void captureIgnoresPlainDisposables() {
		class PlainDisposable implements solim.core.Disposable {
			@Override
			public void dispose() {
			}

			@Override
			public boolean isDisposed() {
				return false;
			}
		}
		List<Component> captured = AttachmentStack.capture(() -> {
			OwnershipContext.register(new PlainDisposable());
			new RecordingComponent("A");
		});
		assertEquals(1, captured.size(), "Plain disposables must not be captured as roots");
		captured.get(0).dispose();
	}

	@Test
	void captureNullRunnableReturnsEmptyList() {
		assertTrue(AttachmentStack.capture(null).isEmpty());
	}

	@Test
	void captureRestoresAmbientStateAfterExceptionAndDisposesPartialRoots() {
		Table outer = new Table();
		AttachmentStack.push(outer);
		List<RecordingComponent> partiallyCreated = new ArrayList<>();

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> AttachmentStack.capture(() -> {
			partiallyCreated.add(new RecordingComponent("before"));
			throw new IllegalArgumentException("boom");
		}));

		assertEquals("boom", thrown.getMessage());
		assertEquals(1, AttachmentStack.size(), "Stack must be restored to pre-capture depth after failure");
		assertSame(outer, AttachmentStack.current(), "Outer entry must survive capture failure");
		assertEquals(0, outer.getChildren().size, "Partial root must not attach to outer parent");
		assertTrue(partiallyCreated.get(0).isDisposed(), "Partially created root must be disposed on failure");
		AttachmentStack.pop();
	}
}
