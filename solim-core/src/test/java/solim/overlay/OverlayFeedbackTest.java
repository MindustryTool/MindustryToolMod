package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.feedback.Alert;
import solim.feedback.Avatar;
import solim.feedback.Badge;
import solim.feedback.ProgressBar;
import solim.feedback.Spinner;
import solim.reactive.Computed;
import solim.reactive.Signal;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import java.util.ArrayList;
import java.util.List;

class OverlayFeedbackTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void dialogShowHideDispose() {
		SolimDialog d = new SolimDialog("Test");
		assertFalse(d.isShown());
		d.show();
		assertTrue(d.isShown());
		d.hide();
		assertFalse(d.isShown());
		d.dispose();
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
		assertTrue(d.isDisposed());
		assertTrue(disposed.get(), "Attached content component must be disposed when dialog is disposed");
	}

	@Test
	void dialogDefaultsToFullScreen() {
		SolimDialog d = new SolimDialog("Full Screen Test");
		assertTrue(d.isFillParent(), "SolimDialog must default to full screen (fillParent == true)");
		d.fillParent(false);
		assertFalse(d.isFillParent(), "fillParent(false) must allow disabling full screen");
		d.dispose();
	}

	public static class TestDialogEvent {
		public final int code;

		public TestDialogEvent(int code) {
			this.code = code;
		}
	}

	@Test
	void dialogSignalCreationAndEventRecalculation() {
		SolimDialog d = new SolimDialog("Test");
		int[] counter = new int[] {10};
		List<Runnable> resizeCallbacks = new ArrayList<>();

		// Test callback-based signal creation (e.g. this.resized(callback))
		Signal<Integer> resizeSignal = d.createSignal(cb -> {
			resizeCallbacks.add(cb);
			return () -> resizeCallbacks.remove(cb);
		}, () -> counter[0]);
		assertEquals(10, resizeSignal.get());

		counter[0] = 25;
		for (Runnable r : resizeCallbacks) r.run();
		assertEquals(25, resizeSignal.get());

		// Test event-based signal creation (Events.on)
		Signal<Integer> eventSignal = d.createSignal(TestDialogEvent.class, () -> counter[0] * 2);
		assertEquals(50, eventSignal.get());

		counter[0] = 30;
		Events.fire(new TestDialogEvent(1));
		assertEquals(60, eventSignal.get());

		// Test event-mapping signal creation
		Signal<Integer> mappedSignal = d.createSignal(TestDialogEvent.class, e -> e.code * 100, 0);
		assertEquals(0, mappedSignal.get());
		Events.fire(new TestDialogEvent(5));
		assertEquals(500, mappedSignal.get());

		// Test disposal cleans up listeners
		d.dispose();
		assertTrue(d.isDisposed());
		assertTrue(resizeCallbacks.isEmpty(), "Callback subscription must be unregistered on dispose");

		counter[0] = 999;
		Events.fire(new TestDialogEvent(99));
		// After disposal, event listener was unregistered so eventSignal should not recalculate
		assertEquals(60, eventSignal.get());
		assertEquals(500, mappedSignal.get());
	}

	@Test
	void progressBarReactive() {
		Signal<Float> progress = Signal.of(0.3f);
		ProgressBar pb = new ProgressBar(progress);
		assertEquals(1, pb.bar().getChildren().size);
		Element firstFill = pb.bar().getChildren().get(0);

		progress.set(0.7f);
		assertEquals(1, pb.bar().getChildren().size);
		Element secondFill = pb.bar().getChildren().get(0);
		assertNotSame(firstFill, secondFill, "Progress change must rebuild fill");

		pb.dispose();
		progress.set(0.9f);
		assertSame(secondFill, pb.bar().getChildren().get(0), "Disposed bar must not rebuild");
	}

	@Test
	void alertDismiss() {
		Alert a = new Alert("Saved!", Alert.Type.SUCCESS);
		assertTrue(a.isShown());
		a.dismiss();
		assertFalse(a.isShown());
	}

	@Test
	void badgeReactiveCount() {
		Signal<Integer> count = Signal.of(5);
		Computed<String> text = count.map(v -> v > 99 ? "99+" : String.valueOf(v));
		Badge b = Badge.of(text);
		assertEquals("5", ((Label) b.table().getChildren().get(0)).getText().toString());

		count.set(50);
		assertEquals("50", ((Label) b.table().getChildren().get(0)).getText().toString());

		count.set(150);
		assertEquals("99+", ((Label) b.table().getChildren().get(0)).getText().toString());

		b.dispose();
		count.set(7);
		assertEquals("99+", ((Label) b.table().getChildren().get(0)).getText().toString());
	}

	@Test
	void spinnerShowsLoadingText() {
		Spinner s = new Spinner();
		assertEquals("Loading...", s.label().getText().toString());
	}

	@Test
	void avatarContainsSizedImage() {
		Avatar a = new Avatar();
		assertEquals(1, a.table().getChildren().size);
		assertTrue(a.table().getChildren().get(0) instanceof Image);
	}

	@Test
	void popupStartsEmpty() {
		Popup<Object> p = new Popup<>();
		assertEquals(0, p.table().getChildren().size);
	}
}
