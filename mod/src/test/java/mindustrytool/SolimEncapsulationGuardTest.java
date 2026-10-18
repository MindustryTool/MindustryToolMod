package mindustrytool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class SolimEncapsulationGuardTest extends ArcTestEnv {

    private static final String GUARD_FILE = "SolimEncapsulationGuardTest.java";

    private static final List<String> FORBIDDEN_REFERENCES = Arrays.asList(
            "solim.runtime",
            "SolimToken",
            "PendingCellConfig",
            "AttachmentStack",
            "AttachmentBridge",
            "OwnershipContext",
            "OwnershipGuard",
            "openSelfWindow(",
            "closeSelfWindow(",
            "withinOwnership(",
            "onRealized(",
            "applyPendingProps(",
            "applyToCell(",
            "cellConfig(",
            "respace(",
            "adopt(",
            "reparentTo(",
            "solim.modifier.Values");

    private static final String MANUAL_OWN = ".*\\bown\\s*\\(.*";

    @Test
    void noModSourceReferencesAnInternalOrLifecycleSolimApi() throws IOException {
        File sourceRoot = new File("src/mindustrytool");
        if (!sourceRoot.exists()) {
            sourceRoot = new File("mod/src/mindustrytool");
        }
        assertTrue(sourceRoot.exists(), "mod/src/mindustrytool must exist: " + sourceRoot.getAbsolutePath());

        File testRoot = new File(sourceRoot.getParentFile(), "test/java/mindustrytool");

        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceRoot, javaFiles);
        if (testRoot.exists()) {
            collectJavaFiles(testRoot, javaFiles);
        }

        assertFalse(javaFiles.isEmpty(), "Must find java files in mod");

        List<String> violations = new ArrayList<>();
        for (File file : javaFiles) {
            if (GUARD_FILE.equals(file.getName())) {
                continue;
            }
            scan(file, violations);
        }

        assertTrue(violations.isEmpty(),
                "Mod source code must use the declarative Solim API, never an internal or lifecycle seam:\n"
                        + String.join("\n", violations));
    }

    private void scan(File file, List<String> violations) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                    continue;
                }
                for (String forbidden : FORBIDDEN_REFERENCES) {
                    if (line.contains(forbidden)) {
                        violations.add(file.getName() + ":" + lineNum + " -> " + trimmed);
                    }
                }
                if (line.matches(MANUAL_OWN)) {
                    violations.add(file.getName() + ":" + lineNum + " -> " + trimmed);
                }
            }
        }
    }

    private void collectJavaFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaFiles(f, result);
            } else if (f.getName().endsWith(".java")
                    && !f.getAbsolutePath().contains("old")
                    && !f.getName().endsWith("._java")) {
                result.add(f);
            }
        }
    }
}
