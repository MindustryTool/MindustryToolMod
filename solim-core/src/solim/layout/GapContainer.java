package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableCapability;

public interface GapContainer extends GapHost, TableCapability {

    Direction direction();

    float gap();

    void respace();

    static @Nullable GapContainer find(@Nullable Table table) {
        if (table == null)
            return null;
        Component comp = SolimToken.getComponent(table);
        return comp instanceof GapContainer ? (GapContainer) comp : null;
    }

    static boolean isGapContainer(@Nullable Table table) {
        return find(table) != null;
    }

    static void respace(@Nullable Table table) {
        GapContainer gc = find(table);
        if (gc != null) {
            gc.respace();
        }
    }

    static void spaceAttachedCell(@Nullable Table table, @Nullable Cell<?> cell, Direction direction, float gap) {
        if (table == null || cell == null || gap <= 0f)
            return;
        Element el = cell.get();
        if (el == null || !el.visible)
            return;
        // Arc's Table.getCells() is declared as Seq<Cell>, with a raw Cell argument that cannot be recovered from that API.
        @SuppressWarnings("rawtypes")
        Seq<Cell> cells = table.getCells();
        if (cells == null || cells.size <= 1)
            return;

        boolean hasPriorVisible = false;
        for (int i = 0; i < cells.size - 1; i++) {
            Cell<?> c = cells.get(i);
            if (c != null && c.get() != null && c.get().visible) {
                hasPriorVisible = true;
                break;
            }
        }

        if (hasPriorVisible) {
            if (direction == Direction.HORIZONTAL || direction == Direction.X) {
                cell.padLeft(resolveMarginLeft(el) + gap);
            } else {
                cell.padTop(resolveMarginTop(el) + gap);
            }
        }
    }

    // Arc's Table.getCells() is declared as Seq<Cell>, with a raw Cell argument that cannot be recovered from that API.
    @SuppressWarnings("rawtypes")
    static void applySpacing(@Nullable Table table, Direction direction, float gap) {
        if (table == null)
            return;
        Seq<Cell> cells = table.getCells();
        if (cells == null || cells.size == 0)
            return;

        boolean isFirstVisible = true;
        for (int i = 0; i < cells.size; i++) {
            Cell<?> cell = cells.get(i);
            if (cell == null)
                continue;
            Element el = cell.get();
            if (el == null)
                continue;

            if (!el.visible) {
                cell.padTop(0f).padBottom(0f).padLeft(0f).padRight(0f);
                continue;
            }

            float gapOffset = isFirstVisible ? 0f : Math.max(0f, gap);
            isFirstVisible = false;

            float marginLeft = resolveMarginLeft(el);
            float marginRight = resolveMarginRight(el);
            float marginTop = resolveMarginTop(el);
            float marginBottom = resolveMarginBottom(el);

            if (direction == Direction.HORIZONTAL || direction == Direction.X) {
                cell.padTop(marginTop);
                cell.padLeft(marginLeft + gapOffset);
                cell.padBottom(marginBottom);
                cell.padRight(marginRight);
            } else {
                cell.padTop(marginTop + gapOffset);
                cell.padLeft(marginLeft);
                cell.padBottom(marginBottom);
                cell.padRight(marginRight);
            }
        }
        table.invalidate();
    }

    // Arc's Table.getCells() is declared as Seq<Cell>, with a raw Cell argument that cannot be recovered from that API.
    @SuppressWarnings("rawtypes")
    static void applyGridSpacing(@Nullable Table table, int columns, float gap) {
        if (table == null)
            return;
        Seq<Cell> cells = table.getCells();
        if (cells == null || cells.size == 0)
            return;

        int visibleIndex = 0;
        int cols = Math.max(1, columns);
        for (int i = 0; i < cells.size; i++) {
            Cell<?> cell = cells.get(i);
            if (cell == null)
                continue;
            Element el = cell.get();
            if (el == null)
                continue;

            if (!el.visible) {
                cell.padTop(0f).padBottom(0f).padLeft(0f).padRight(0f);
                continue;
            }

            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            visibleIndex++;

            float gapLeft = col > 0 ? Math.max(0f, gap) : 0f;
            float gapTop = row > 0 ? Math.max(0f, gap) : 0f;

            float marginLeft = resolveMarginLeft(el);
            float marginRight = resolveMarginRight(el);
            float marginTop = resolveMarginTop(el);
            float marginBottom = resolveMarginBottom(el);

            cell.padTop(marginTop + gapTop);
            cell.padLeft(marginLeft + gapLeft);
            cell.padBottom(marginBottom);
            cell.padRight(marginRight);
        }
        table.invalidate();
    }

    static float resolveMarginLeft(Element el) {
        PendingCellConfig sc = PendingCellConfig.find(el);
        if (sc != null && sc.padLeft != null && sc.padLeft.get() != null) {
            return Math.max(0f, sc.padLeft.get());
        }
        return 0f;
    }

    static float resolveMarginRight(Element el) {
        PendingCellConfig sc = PendingCellConfig.find(el);
        if (sc != null && sc.padRight != null && sc.padRight.get() != null) {
            return Math.max(0f, sc.padRight.get());
        }
        return 0f;
    }

    static float resolveMarginTop(Element el) {
        PendingCellConfig sc = PendingCellConfig.find(el);
        if (sc != null && sc.padTop != null && sc.padTop.get() != null) {
            return Math.max(0f, sc.padTop.get());
        }
        return 0f;
    }

    static float resolveMarginBottom(Element el) {
        PendingCellConfig sc = PendingCellConfig.find(el);
        if (sc != null && sc.padBottom != null && sc.padBottom.get() != null) {
            return Math.max(0f, sc.padBottom.get());
        }
        return 0f;
    }
}
