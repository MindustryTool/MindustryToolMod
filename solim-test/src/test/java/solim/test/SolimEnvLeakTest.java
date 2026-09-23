package solim.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.runtime.AttachmentStack;

/**
 * Verifies the strict teardown contract: leaked solim ambient state must fail
 * teardown with a clear assertion, and cleanup must still happen so following
 * tests are not contaminated.
 */
class SolimEnvLeakTest {

	@Test
	void leakedAttachmentStackFailsTeardown() {
		Harness env = new Harness();
		env.setUpSolimEnv();

		AttachmentStack.push(new Table());

		try {
			AssertionError error = assertThrows(AssertionError.class, env::verifyAndTearDownSolimEnv);
			assertTrue(error.getMessage().contains("AttachmentStack"),
					"teardown failure should name the leaked ambient state, got: " + error.getMessage());
		} finally {
			// verifyAndTearDownSolimEnv cleans up before asserting; nothing leaks here
			AttachmentStack.clear();
		}
	}

	private static class Harness extends SolimEnv {
	}
}
