package solim.feedback;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.func.Prov;
import arc.scene.ui.Label;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.reactive.Computed;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.OwnershipEnv;

class FeedbackDisposalTest extends OwnershipEnv {

	/**
	 * {@code ProgressBar} and {@code FeedbackBadge} create their binding effect in the
	 * constructor, so they must be instantiated inside an ownership window. The helper
	 * opens a window owned by a throwaway holder, mirroring how a component would own
	 * them through {@code children()}.
	 */
	private static final class Holder extends Component {
	}

	private static <T> T insideWindow(Prov<T> factory) {
		Holder holder = new Holder();
		OwnershipContext.push(holder);
		try {
			return factory.get();
		} finally {
			OwnershipContext.pop();
		}
	}

	@BeforeEach
	void setUp() {
		newScene();
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void badgeBindingDiesOnDispose() {
		Signal<String> source = Signal.of("a");
		Computed<String> text = insideWindow(() -> source.map(v -> "[" + v + "]"));
		text.peek();
		FeedbackBadge badge = insideWindow(() -> FeedbackBadge.of(text));
		assertFalse(badge.isDisposed());

		source.set("b");
		SignalDispatcher.flush();
		assertEquals("[b]", badgeLabel(badge));

		badge.dispose();
		assertTrue(badge.isDisposed());

		source.set("c");
		SignalDispatcher.flush();
		assertEquals("[b]", badgeLabel(badge), "Badge text must freeze after dispose");

		assertDoesNotThrow(badge::dispose, "Double dispose must be safe");
		assertTrue(badge.isDisposed());
		text.dispose();
	}

	static String badgeLabel(FeedbackBadge badge) {
		return ((Label) badge.table().getChildren().first()).getText().toString();
	}

	@Test
	void badgeDefaultConstructorDisposesSafely() {
		FeedbackBadge badge = new FeedbackBadge();
		assertFalse(badge.isDisposed());

		badge.dispose();
		assertTrue(badge.isDisposed());

		assertDoesNotThrow(badge::dispose, "Double dispose must be safe");
		assertTrue(badge.isDisposed());
	}

	@Test
	void progressBarEffectDiesOnDispose() {
		Signal<Float> progress = Signal.of(0f);
		ProgressBar bar = insideWindow(() -> ProgressBar.of(progress));
		assertFalse(bar.isDisposed());

		progress.set(0.5f);
		SignalDispatcher.flush();
		int children = bar.bar().getChildren().size;

		bar.dispose();
		assertTrue(bar.isDisposed());

		progress.set(0.9f);
		SignalDispatcher.flush();
		assertEquals(children, bar.bar().getChildren().size, "Bar must freeze after dispose");

		assertDoesNotThrow(bar::dispose, "Double dispose must be safe");
		assertTrue(bar.isDisposed());
	}
}
