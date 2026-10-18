package solim.overlay;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.*;

import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class PopupEdgePinTest extends SolimEnv {

    @Test
    void defaultEdgeIsUnset() {
        Popup<String> menu = new Popup<>();
        try {
            assertNull(menu.pinnedEdge(), "Pinning must be opt-in so existing popups behave exactly as today");
        } finally {
            disposeQuietly(menu);
        }
    }

    @Test
    void pureTopPinGrowthExtendsDownward() {
        Vec2 pos = Popup.pinnedPosition(Popup.Edge.TOP, 50f, 200f, 100f, 80f, 100f, 160f, 800f, 600f);
        assertEquals(50f, pos.x, 0.001f);
        assertEquals(120f, pos.y, 0.001f, "Top edge 280 must stay fixed");
    }

    @Test
    void pureBottomPinGrowthExtendsUpward() {
        Vec2 pos = Popup.pinnedPosition(Popup.Edge.BOTTOM, 50f, 200f, 100f, 80f, 100f, 160f, 800f, 600f);
        assertEquals(50f, pos.x, 0.001f);
        assertEquals(200f, pos.y, 0.001f, "Bottom edge must stay fixed");
    }

    @Test
    void pureShrinkRetractsTowardPinnedEdge() {
        Vec2 top = Popup.pinnedPosition(Popup.Edge.TOP, 50f, 120f, 100f, 160f, 100f, 80f, 800f, 600f);
        assertEquals(200f, top.y, 0.001f, "Top edge 280 must stay fixed while shrinking");
        Vec2 bottom = Popup.pinnedPosition(Popup.Edge.BOTTOM, 50f, 120f, 100f, 160f, 100f, 80f, 800f, 600f);
        assertEquals(120f, bottom.y, 0.001f, "Bottom edge must stay fixed while shrinking");
    }

    @Test
    void pureClampWinsOverPin() {
        Vec2 pos = Popup.pinnedPosition(Popup.Edge.TOP, 50f, 500f, 100f, 80f, 100f, 200f, 800f, 500f);
        assertEquals(300f, pos.y, 0.001f, "Top edge 580 cannot be preserved inside 500px stage with 200px height");
    }

    @Test
    void pureNullEdgeReturnsPositionUnchanged() {
        Vec2 pos = Popup.pinnedPosition(null, 50f, 200f, 100f, 80f, 100f, 160f, 800f, 600f);
        assertEquals(50f, pos.x, 0.001f);
        assertEquals(200f, pos.y, 0.001f);
    }

    @Test
    void tableTopPinPreservesTopEdgeOnGrowth() {
        Popup<String> menu = new Popup<>();
        try {
            setTableContent(menu, 100f, 80f);
            menu.table().pack();
            menu.table().setPosition(50f, 200f);
            menu.props(pinnedEdge(Popup.Edge.TOP));
            assertSame(Popup.Edge.TOP, menu.pinnedEdge());

            swapTableContent(menu, 100f, 160f);
            menu.table().validate();

            assertEquals(160f, menu.table().getHeight(), 0.5f);
            assertEquals(120f, menu.table().y, 0.5f, "Top edge 280 must stay fixed");

            menu.table().validate();
            assertEquals(120f, menu.table().y, 0.5f, "Repeated validates must be position-stable");
        } finally {
            disposeQuietly(menu);
        }
    }

    @Test
    void tableBottomPinPreservesBottomEdgeOnGrowth() {
        Popup<String> menu = new Popup<>();
        try {
            setTableContent(menu, 100f, 80f);
            menu.table().pack();
            menu.table().setPosition(50f, 200f);
            menu.props(pinnedEdge(Popup.Edge.BOTTOM));

            swapTableContent(menu, 100f, 160f);
            menu.table().validate();

            assertEquals(160f, menu.table().getHeight(), 0.5f);
            assertEquals(200f, menu.table().y, 0.5f, "Bottom edge must stay fixed");
        } finally {
            disposeQuietly(menu);
        }
    }

    @Test
    void tableWithoutPinKeepsLegacyPosition() {
        Popup<String> menu = new Popup<>();
        try {
            setTableContent(menu, 100f, 80f);
            menu.table().pack();
            menu.table().setPosition(50f, 200f);

            swapTableContent(menu, 100f, 160f);
            menu.table().validate();

            assertEquals(200f, menu.table().y, 0.5f, "Unpinned popups must keep legacy fixed-corner behavior");
        } finally {
            disposeQuietly(menu);
        }
    }

    private static void setTableContent(Popup<String> menu, float width, float height) {
        menu.table().clearChildren();
        Table leaf = new Table();
        menu.table().add(leaf).size(width, height);
    }

    private static void swapTableContent(Popup<String> menu, float width, float height) {
        setTableContent(menu, width, height);
        menu.table().invalidateHierarchy();
    }

    private static void disposeQuietly(Popup<String> menu) {
        try {
            menu.dispose();
        } catch (Throwable ignored) {
        }
    }
}
