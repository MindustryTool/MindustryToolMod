package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import arc.scene.Element;
import org.junit.jupiter.api.Test;
import solim.test.SolimCoreEnv;

/**
 * The ownership stack holds the owner itself, so registering one cannot allocate a callback.
 *
 * <p>Before the unification an owning component pushed a {@code Cons<Disposable>} method
 * reference: one captured allocation for every component that ever built. These tests pin the
 * replacement structurally — what the stack stores, and that a push costs exactly one slot.
 */
class OwnershipStackAllocationTest extends SolimCoreEnv {

	/** A component that needs neither a scene nor a build: its element exists from the start. */
	private static final class FixedElement extends Component {
		FixedElement() {
			super(new Element());
		}
	}

	@Test
	void theStackHoldsTheOwnerItself() {
		Component owner = new FixedElement();

		OwnershipContext.push(owner);
		try {
			assertSame(owner, OwnershipContext.current(),
					"The stack must hold the owner instance, not a callback wrapping it");
		} finally {
			OwnershipContext.pop();
		}
		assertNull(OwnershipContext.current());
	}

	@Test
	void aPushCostsExactlyOneStackSlot() {
		int before = OwnershipContext.size();

		OwnershipContext.push(new FixedElement());
		OwnershipContext.push(new FixedElement());
		assertEquals(before + 2, OwnershipContext.size());

		OwnershipContext.pop();
		OwnershipContext.pop();
		assertEquals(before, OwnershipContext.size());
	}

	@Test
	void aPushedOwnerAdoptsDirectlyWithNoCallbackInBetween() {
		Component owner = new FixedElement();
		Disposable leaf = DisposableAction.of(() -> {
		});

		OwnershipContext.push(owner);
		try {
			OwnershipContext.register(leaf);
		} finally {
			OwnershipContext.pop();
		}

		assertEquals(false, leaf.isDisposed());
		owner.dispose();
		assertEquals(true, leaf.isDisposed(),
				"A resource registered against the pushed owner must be disposed with it");
	}
}
