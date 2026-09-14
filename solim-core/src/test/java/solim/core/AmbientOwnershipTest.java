package solim.core;

import solim.runtime.ComponentContext;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import solim.input.SolimTextField;
import solim.signal.Signal;
import solim.ui.Binding;
import solim.runtime.SignalDispatcher;

class AmbientOwnershipTest {

	static class ChildComponent extends BaseComponent {
		final AtomicBoolean disposedFlag;

		ChildComponent(AtomicBoolean disposedFlag) {
			this.disposedFlag = disposedFlag;
		}

		@Override
		protected Element build() {
			return new Element();
		}

		@Override
		protected void onDispose() {
			disposedFlag.set(true);
		}
	}

	static class CustomDisposable implements Disposable {
		final AtomicBoolean disposedFlag = new AtomicBoolean(false);

		CustomDisposable() {
			ComponentContext.register(this);
		}

		@Override
		public void dispose() {
			disposedFlag.set(true);
		}
	}

	static class ParentComponent extends BaseComponent {
		ChildComponent child;
		CustomDisposable customDisposable;
		SolimTextField textField;
		final Signal<String> textSignal = Signal.of("initial");
		final Signal<Float> widthSignal = Signal.of(100f);
		Element boundElement;
		final AtomicBoolean childDisposed = new AtomicBoolean(false);

		@Override
		protected Element build() {
			child = new ChildComponent(childDisposed);
			customDisposable = new CustomDisposable();
			if (Core.app != null && Core.scene != null) {
				textField = SolimTextField.of(textSignal);
			}
			boundElement = new Element();
			Binding.bindWidth(boundElement, widthSignal);
			return new Element();
		}
	}

	@Test
	void ambientOwnershipAutomaticallyDisposesChildrenAndBindings() {
		ParentComponent parent = new ParentComponent();
		parent.element();

		assertFalse(parent.childDisposed.get());
		assertFalse(parent.customDisposable.disposedFlag.get());

		// Reactive bindings update
		parent.widthSignal.set(200f);
		SignalDispatcher.flush();
		assertEquals(200f, parent.boundElement.getWidth());

		// Disposing parent automatically disposes child, custom disposable, and binding
		parent.dispose();
		assertTrue(parent.isDisposed());
		assertTrue(parent.child.isDisposed());
		assertTrue(parent.childDisposed.get());
		assertTrue(parent.customDisposable.disposedFlag.get());

		// Post-disposal signal update should not affect boundElement
		parent.widthSignal.set(300f);
		SignalDispatcher.flush();
		assertEquals(200f, parent.boundElement.getWidth());
	}

	@Test
	void cleanStackUnwindingOnBuildFailure() {
		assertEquals(0, ComponentContext.size());
		BaseComponent failingComponent = new BaseComponent() {
			@Override
			protected Element build() {
				throw new IllegalStateException("Simulated build error");
			}
		};

		assertThrows(IllegalStateException.class, failingComponent::element);
		assertEquals(0, ComponentContext.size(), "ComponentContext stack must be unwound even if build() throws");
	}
}
