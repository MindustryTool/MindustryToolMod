package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import solim.core.OwnershipContext;
import solim.display.Text;
import solim.layout.Row;
import solim.test.SolimCoreEnv;

/**
 * Disposal proof for the structural component {@link When}.
 *
 * <p>{@link When} owns components it did not create the usual way: every branch is captured as a
 * fragment, mounted into the container, and dropped again on a condition switch or on disposal.
 * The failure this guards against is a branch that survives its {@code When} — still observing
 * signals, still holding widgets — long after nothing renders it.
 */
class WhenDisposalTest extends SolimCoreEnv {

	@BeforeEach
	void setUp() {
		newScene();
	}

	@Test
	void switchingBranchesDisposesTheReplacedBranch() {
		Signal<Boolean> condition = Signal.of(true);
		Signal<String> thenLabel = Signal.of("then");
		Signal<String> elseLabel = Signal.of("else");
		Text[] thenBranch = new Text[1];
		Text[] elseBranch = new Text[1];

		When when = When.of(condition)
				.thenDo(() -> thenBranch[0] = Text.of(thenLabel))
				.elseDo(() -> elseBranch[0] = Text.of(elseLabel));
		when.element();
		flushEffects();
		assertEquals(1, when.container().getChildren().size);
		assertFalse(thenBranch[0].isDisposed());

		condition.set(false);
		flushEffects();
		assertTrue(thenBranch[0].isDisposed(), "The replaced branch must dispose on switch");
		assertEquals(0, thenLabel.observerCount(),
				"A replaced branch must stop observing its own signals");
		assertFalse(elseBranch[0].isDisposed());

		when.dispose();
	}

	@Test
	void disposingTheContainerDisposesTheActiveBranch() {
		Signal<Boolean> condition = Signal.of(true);
		Signal<String> label = Signal.of("then");
		Text[] branch = new Text[1];

		When when = When.of(condition).thenDo(() -> branch[0] = Text.of(label));
		when.element();
		flushEffects();
		assertEquals(1, label.observerCount(), "A live branch observes its signal");

		when.dispose();
		assertTrue(when.isDisposed());
		assertTrue(branch[0].isDisposed(), "The active branch must dispose with the container");
		assertEquals(0, label.observerCount(), "A disposed branch must stop observing its signal");
		assertEquals(0, when.container().getChildren().size, "A disposed container must be emptied");
	}

	@Test
	void conditionChangesAfterDisposalMountNothing() {
		Signal<Boolean> condition = Signal.of(true);
		Signal<String> thenLabel = Signal.of("then");
		Signal<String> elseLabel = Signal.of("else");
		Text[] thenBranch = new Text[1];
		Text[] elseBranch = new Text[1];

		When when = When.of(condition)
				.thenDo(() -> thenBranch[0] = Text.of(thenLabel))
				.elseDo(() -> elseBranch[0] = Text.of(elseLabel));
		when.element();
		flushEffects();
		when.dispose();

		condition.set(false);
		condition.set(true);
		flushEffects();

		assertEquals(0, when.container().getChildren().size, "A disposed When must not mount a branch again");
		assertTrue(thenBranch[0].isDisposed());
		assertNull(elseBranch[0], "A disposed When must not build the branch it never mounted");
		assertEquals(0, thenLabel.observerCount(), "Post-dispose condition changes must observe nothing");
		assertEquals(0, elseLabel.observerCount(), "Post-dispose condition changes must observe nothing");
	}

	@Test
	void branchCellConstraintBindingsDieWithTheBranch() {
		Signal<Boolean> condition = Signal.of(true);
		Signal<Float> minWidth = Signal.of(40f);
		Signal<String> elseLabel = Signal.of("else");
		Text[] elseBranch = new Text[1];

		When when = When.of(condition)
				.thenDo(() -> new Row().growX().minWidth(minWidth))
				.elseDo(() -> elseBranch[0] = Text.of(elseLabel));
		when.element();
		flushEffects();
		assertTrue(minWidth.observerCount() > 0, "A mounted branch's cell constraints must observe the signal");

		when.dispose();
		assertEquals(0, minWidth.observerCount(),
				"Disposing the container must release the branch's cell constraint bindings");
	}

	@Test
	void disposingIsIdempotentAndLeavesNoScope() {
		Signal<Boolean> condition = Signal.of(true);
		When when = When.of(condition).thenDo(() -> Text.of("body"));
		when.element();
		flushEffects();

		when.dispose();
		assertDoesNotThrow(when::dispose, "Double dispose must be safe");
		assertDoesNotThrow(when::dispose, "Repeated dispose must stay safe");
		assertTrue(when.isDisposed());
		assertEquals(0, OwnershipContext.size(), "No ownership scope may leak");
	}
}