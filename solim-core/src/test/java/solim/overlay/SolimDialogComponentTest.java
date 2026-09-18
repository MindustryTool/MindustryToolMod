package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import java.util.ArrayList;
import java.util.List;
import solim.reactive.Signal;

class SolimDialogComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void dialogCreatesWithDefaultName() {
		SolimDialog d = new SolimDialog("Test");
		assertEquals("solim-dialog-dialog", d.name);
		assertSame(d.dialog(), d.element());
		d.dispose();
	}

	@Test
	void dialogShowHideLifecycle() {
		SolimDialog d = new SolimDialog("Test");
		assertFalse(d.isShown());
		d.show();
		assertTrue(d.isShown());
		d.hide();
		assertFalse(d.isShown());
		d.dispose();
	}

	@Test
	void dialogDisposeMarksDisposed() {
		SolimDialog d = new SolimDialog("Test");
		assertFalse(d.isDisposed());
		d.dispose();
		assertTrue(d.isDisposed());
	}

	@Test
	void dialogContentComponentAutomaticallyDisposed() {
		AtomicBoolean disposed = new AtomicBoolean(false);

		SolimDialog d = new SolimDialog("Lifecycle Test");
		d.children(() -> new BaseComponent() {
			@Override
			protected Element build() {
				return new Table();
			}

			@Override
			public void dispose() {
				super.dispose();
				disposed.set(true);
			}
		});
		d.ensureContentBuilt();
		assertFalse(disposed.get());
		d.dispose();
		assertTrue(disposed.get(), "Attached content component must be disposed when dialog is disposed");
	}

	@Test
	void dialogChildrenExecutedLazily() {
		AtomicBoolean built = new AtomicBoolean(false);
		SolimDialog d = new SolimDialog("Lazy Test");
		d.children(() -> built.set(true));

		assertFalse(built.get(), "Children builder must not run during dialog instantiation");

		d.ensureContentBuilt();
		assertTrue(built.get(), "Children builder must run when ensureContentBuilt is called");
		d.dispose();
	}

	@Test
	void dialogChildrenOnlyExecutedOnce() {
		AtomicInteger runCount = new AtomicInteger(0);
		SolimDialog d = new SolimDialog("Single Execution Test");
		d.children(runCount::incrementAndGet);

		assertEquals(0, runCount.get());
		d.ensureContentBuilt();
		assertEquals(1, runCount.get());
		d.ensureContentBuilt();
		assertEquals(1, runCount.get(), "Children builder must only be executed once");
		d.dispose();
	}

	@Test
	void dialogDefaultsToFullScreen() {
		SolimDialog d = new SolimDialog("Full Screen Test");
		assertTrue(d.isFillParent(), "SolimDialog must default to full screen");
		d.fillParent(false);
		assertFalse(d.isFillParent());
		d.dispose();
	}

	@Test
	void dialogSignalCreationAndEventRecalculation() {
		SolimDialog d = new SolimDialog("Test");
		int[] counter = new int[] {10};
		List<Runnable> resizeCallbacks = new ArrayList<>();

		Signal<Integer> resizeSignal = d.createSignal(cb -> {
			resizeCallbacks.add(cb);
			return () -> resizeCallbacks.remove(cb);
		}, () -> counter[0]);
		assertEquals(10, resizeSignal.get());

		counter[0] = 25;
		for (Runnable r : resizeCallbacks) r.run();
		assertEquals(25, resizeSignal.get());

		d.dispose();
		assertTrue(d.isDisposed());
	}

	@Test
	void dialogFluentApiReturnsSameInstance() {
		SolimDialog d = new SolimDialog("Test");
		assertSame(d, d.fillParent(false));
		assertTrue(d.isFillParent() == false);
		assertSame(d, d.fillParent(true));
		d.dispose();
	}

	@Test
	void dialogUnwrapBuildsContentOnce() {
		AtomicInteger runCount = new AtomicInteger(0);
		SolimDialog d = new SolimDialog("Unwrap Test");
		d.children(runCount::incrementAndGet);

		assertEquals(0, runCount.get());
		d.dialog();
		assertEquals(1, runCount.get(), "dialog() must build deferred content");
		d.cont();
		d.show();
		assertEquals(1, runCount.get(), "show/cont/dialog must share a single build");
		assertSame(d.dialog().cont, d.cont());
		d.dispose();
	}
}
