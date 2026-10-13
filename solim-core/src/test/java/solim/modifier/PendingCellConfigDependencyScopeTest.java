package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Readable;
import solim.test.SolimCoreEnv;

class PendingCellConfigDependencyScopeTest extends SolimCoreEnv {

    static final class CountingReadable implements Readable<Float> {
        final Readable<Float> delegate;
        int gets;

        CountingReadable(float value) {
            this.delegate = Readable.of(value);
        }

        @Override
        public Float get() {
            gets++;
            return delegate.get();
        }
    }

    private Table parent;
    private Element child;

    @AfterEach
    void tearDown() {
        if (parent != null) {
            parent.clear();
        }
    }

    private Element attach() {
        parent = new Table();
        child = new Element();
        parent.add(child);
        assertNotNull(parent.getCell(child), "Test setup requires the child to have a cell");
        return child;
    }

    @Test
    void maxHeightApplierReadsOnlyMaxHeight() {
        CountingReadable minWidth = new CountingReadable(120f);
        CountingReadable maxHeight = new CountingReadable(60f);

        PendingCellConfig config = new PendingCellConfig();
        config.minWidth = minWidth;
        config.maxHeight = maxHeight;

        config.applyMaxHeight(attach());

        assertEquals(1, maxHeight.gets);
        assertEquals(0, minWidth.gets, "applyMaxHeight must not read minWidth");
    }

    @Test
    void minWidthApplierReadsOnlyMinWidth() {
        CountingReadable minWidth = new CountingReadable(120f);
        CountingReadable maxHeight = new CountingReadable(60f);

        PendingCellConfig config = new PendingCellConfig();
        config.minWidth = minWidth;
        config.maxHeight = maxHeight;

        config.applyMinWidth(attach());

        assertEquals(1, minWidth.gets);
        assertEquals(0, maxHeight.gets, "applyMinWidth must not read maxHeight");
    }

    @Test
    void prefSizeAppliersReadOnlyTheirOwnConstraint() {
        CountingReadable prefWidth = new CountingReadable(200f);
        CountingReadable prefHeight = new CountingReadable(100f);

        PendingCellConfig config = new PendingCellConfig();
        config.prefWidth = prefWidth;
        config.prefHeight = prefHeight;

        config.applyPrefWidth(attach());
        assertEquals(1, prefWidth.gets);
        assertEquals(0, prefHeight.gets, "applyPrefWidth must not read prefHeight");

        config.applyPrefHeight(child);
        assertEquals(1, prefHeight.gets);
        assertEquals(1, prefWidth.gets, "applyPrefHeight must not read prefWidth");
    }

    @Test
    void padTopApplierReadsOnlyPadTop() {
        CountingReadable padTop = new CountingReadable(4f);
        CountingReadable padLeft = new CountingReadable(8f);

        PendingCellConfig config = new PendingCellConfig();
        config.padTop = padTop;
        config.padLeft = padLeft;

        config.applyPadTop(attach());

        assertEquals(1, padTop.gets);
        assertEquals(0, padLeft.gets, "applyPadTop must not read padLeft");
    }

    @Test
    void padLeftApplierReadsOnlyPadLeft() {
        CountingReadable padTop = new CountingReadable(4f);
        CountingReadable padLeft = new CountingReadable(8f);

        PendingCellConfig config = new PendingCellConfig();
        config.padTop = padTop;
        config.padLeft = padLeft;

        config.applyPadLeft(attach());

        assertEquals(1, padLeft.gets);
        assertEquals(0, padTop.gets, "applyPadLeft must not read padTop");
    }

    @Test
    void aggregateAppliersStillReadEveryConstraint() {
        CountingReadable minWidth = new CountingReadable(120f);
        CountingReadable maxHeight = new CountingReadable(60f);
        CountingReadable prefWidth = new CountingReadable(200f);
        CountingReadable prefHeight = new CountingReadable(100f);
        CountingReadable minHeight = new CountingReadable(20f);
        CountingReadable maxWidth = new CountingReadable(400f);

        PendingCellConfig config = new PendingCellConfig();
        config.prefWidth = prefWidth;
        config.prefHeight = prefHeight;
        config.minWidth = minWidth;
        config.minHeight = minHeight;
        config.maxWidth = maxWidth;
        config.maxHeight = maxHeight;

        config.applySizeToParentCell(attach());

        assertEquals(1, prefWidth.gets);
        assertEquals(1, prefHeight.gets);
        assertEquals(1, minWidth.gets);
        assertEquals(1, minHeight.gets);
        assertEquals(1, maxWidth.gets);
        assertEquals(1, maxHeight.gets);
    }

    @Test
    void anUnsetConstraintIsNotReadAtAll() {
        CountingReadable padTop = new CountingReadable(4f);

        PendingCellConfig config = new PendingCellConfig();
        config.padTop = padTop;

        config.applyPadLeft(attach());

        assertEquals(0, padTop.gets, "An applier for an unset constraint must read nothing");
    }
}
