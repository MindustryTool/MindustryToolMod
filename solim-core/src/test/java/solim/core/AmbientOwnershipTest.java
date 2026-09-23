package solim.core;

import solim.runtime.OwnershipContext;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import solim.input.SolimTextField;
import solim.reactive.Signal;
import solim.reactive.Binding;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class AmbientOwnershipTest extends SolimEnv {

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
			OwnershipContext.register(this);
		}

		@Override
		public void dispose() {
			disposedFlag.set(true);
		}

		@Override
		public boolean isDisposed() {
			return disposedFlag.get();
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
		assertEquals(0, OwnershipContext.size());
		BaseComponent failingComponent = new BaseComponent() {
			@Override
			protected Element build() {
				throw new IllegalStateException("Simulated build error");
			}
		};

		assertThrows(IllegalStateException.class, failingComponent::element);
		assertEquals(0, OwnershipContext.size(), "OwnershipContext stack must be unwound even if build() throws");
	}
}
