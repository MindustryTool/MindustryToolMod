package mindustrytool;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SolimEncapsulationGuardTest {

	@Test
	void noModSourceReferencesSolimRuntimeOrManualOwn() throws IOException {
		File modSrcDir = new File("src/mindustrytool");
		if (!modSrcDir.exists()) {
			modSrcDir = new File("mod/src/mindustrytool");
		}
		assertTrue(modSrcDir.exists(), "mod/src/mindustrytool must exist: " + modSrcDir.getAbsolutePath());

		List<File> javaFiles = new ArrayList<>();
		collectJavaFiles(modSrcDir, javaFiles);

		assertFalse(javaFiles.isEmpty(), "Must find java files in mod");

		List<String> runtimeViolations = new ArrayList<>();
		List<String> ownViolations = new ArrayList<>();

		for (File file : javaFiles) {
			// Ignore old/ legacy code
			if (file.getAbsolutePath().contains("old") || file.getName().endsWith("._java")) {
				continue;
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				int lineNum = 0;
				while ((line = reader.readLine()) != null) {
					lineNum++;
					String trimmed = line.trim();
					if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
						continue;
					}
					if (line.contains("solim.runtime")) {
						runtimeViolations.add(file.getName() + ":" + lineNum + " -> " + trimmed);
					}
					if (line.matches(".*\\bown\\s*\\(.*")) {
						ownViolations.add(file.getName() + ":" + lineNum + " -> " + trimmed);
					}
				}
			}
		}

		assertTrue(runtimeViolations.isEmpty(),
				"Mod source code must NEVER reference internal solim.runtime package:\n" + String.join("\n", runtimeViolations));

		assertTrue(ownViolations.isEmpty(),
				"Mod source code must NEVER call manual own() method:\n" + String.join("\n", ownViolations));
	}

	private void collectJavaFiles(File dir, List<File> result) {
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			if (f.isDirectory()) {
				collectJavaFiles(f, result);
			} else if (f.getName().endsWith(".java")) {
				result.add(f);
			}
		}
	}
}