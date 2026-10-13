package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class SolimAssertTest {

    private static final List<String> excludedDirs = Arrays.asList("old", "build", "openspec", "docs", "preview",
            ".git", ".gradle");

    private static final String flagName = "throw" + 'w';

    @AfterEach
    void tearDown() {
        SolimAssert.setMainThread(null);
    }

    @Test
    void checkMainThreadIsNoOpWhenMainThreadUnset() {
        SolimAssert.setMainThread(null);
        assertDoesNotThrow(SolimAssert::checkMainThread);
    }

    @Test
    void checkMainThreadSucceedsOnRegisteredMainThread() {
        SolimAssert.setMainThread(Thread.currentThread());
        assertDoesNotThrow(SolimAssert::checkMainThread);
    }

    @Test
    void checkMainThreadAlwaysThrowsOffMainThread() {
        SolimAssert.setMainThread(Thread.currentThread());
        AtomicRefHolder holder = new AtomicRefHolder();
        Thread worker = new Thread(() -> {
            try {
                SolimAssert.checkMainThread();
            } catch (Throwable t) {
                holder.error = t;
            }
        }, "solim-off-main");
        worker.start();
        join(worker);

        assertInstanceOf(IllegalStateException.class, holder.error,
                "Off-main access must throw: there is no opt-out mode that downgrades this to a log");
    }

    @Test
    void advisoryThreadCheckFlagHasNoRemainingReferences() throws IOException {
        Path root = findRepoRoot();
        Assumptions.assumeTrue(root != null,
                "Repository root is not resolvable from " + Paths.get("").toAbsolutePath());

        List<String> hits = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> sources = new ArrayList<>();
            paths.filter(path -> path.getFileName().toString().endsWith(".java")).forEach(sources::add);
            for (Path source : sources) {

                if (source.getFileName().toString().equals("SolimAssertTest.java") || isExcluded(root, source)) {
                    continue;
                }
                List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains(flagName)) {
                        hits.add(root.relativize(source) + ":" + (i + 1));
                    }
                }
            }
        }

        assertTrue(hits.isEmpty(),
                "The advisory-thread-check flag must not be referenced outside archived history and old/: " + hits);
    }

    private static boolean isExcluded(Path root, Path source) {
        Path relative = root.relativize(source);
        for (Path segment : relative) {
            if (excludedDirs.contains(segment.toString()))
                return true;
        }
        return false;
    }

    private static Path findRepoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("solim-runtime")) && Files.isDirectory(dir.resolve("solim-core"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static final class AtomicRefHolder {
        Throwable error;
    }
}
