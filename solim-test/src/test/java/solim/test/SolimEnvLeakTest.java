package solim.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.runtime.AttachmentStack;

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

            AttachmentStack.clear();
        }
    }

    private static class Harness extends SolimEnv {
    }
}
