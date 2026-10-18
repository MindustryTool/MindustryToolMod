package solim.display;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.growY;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.marginBottom;
import static solim.modifier.Values.marginLeft;
import static solim.modifier.Values.marginRight;
import static solim.modifier.Values.marginTop;
import static solim.modifier.Values.marginX;
import static solim.modifier.Values.marginY;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.maxWidth;
import static solim.modifier.Values.minHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.opacity;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Scaling;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class NetworkImageComponentTest extends SolimEnv {

    @BeforeEach
    void setup() {
        SignalDispatcher.resetForTests();
        NetworkImage.clearCache();
        AttachmentStack.clear();
    }

    @Test
    void networkImageElementIsBackedImage() {
        NetworkImage img = new NetworkImage();
        assertSame(img.image(), img.element());
        assertSame(img, SolimToken.getComponent(img.image()));
        img.dispose();
    }

    @Test
    void staticSizingSetsElementDimensionsAndCellPrefSize() {
        NetworkImage img = new NetworkImage()
                .props(size(64f, 48f))
                .props(scaling(Scaling.fill));

        assertEquals(64f, img.image().getWidth(), 0.01f);
        assertEquals(48f, img.image().getHeight(), 0.01f);
        assertEquals(64f, img.cellConfig().prefWidth.get(), 0.01f);
        assertEquals(48f, img.cellConfig().prefHeight.get(), 0.01f);
        assertEquals(Scaling.fill, img.getScaling());

        img.props(size(32f));
        assertEquals(32f, img.image().getWidth(), 0.01f);
        assertEquals(32f, img.image().getHeight(), 0.01f);

        img.props(width(20f), prefWidth(20f));
        assertEquals(20f, img.image().getWidth(), 0.01f);
        assertEquals(20f, img.cellConfig().prefWidth.get(), 0.01f);

        img.props(height(25f), prefHeight(25f));
        assertEquals(25f, img.image().getHeight(), 0.01f);
        assertEquals(25f, img.cellConfig().prefHeight.get(), 0.01f);

        img.dispose();
    }

    @Test
    void reactiveSizingUpdatesDimensionsAndStopsOnDispose() {
        Signal<Float> w = Signal.of(40f);
        Signal<Float> h = Signal.of(30f);

        NetworkImage img = new NetworkImage().props(size(w, h), prefWidth(w), prefHeight(h));
        assertEquals(40f, img.image().getWidth(), 0.01f);
        assertEquals(30f, img.image().getHeight(), 0.01f);

        w.set(80f);
        h.set(60f);
        SignalDispatcher.flush();

        assertEquals(80f, img.image().getWidth(), 0.01f);
        assertEquals(60f, img.image().getHeight(), 0.01f);

        img.dispose();

        w.set(120f);
        h.set(90f);
        SignalDispatcher.flush();

        assertEquals(80f, img.image().getWidth(), 0.01f);
        assertEquals(60f, img.image().getHeight(), 0.01f);
    }

    @Test
    void scalingAndOriginModifiers() {
        NetworkImage img = new NetworkImage()
                .props(size(50f, 50f))
                .props(scaling(Scaling.fit), origin(Align.center));

        assertEquals(Scaling.fit, img.getScaling());
        assertEquals(25f, img.image().originX, 0.01f);
        assertEquals(25f, img.image().originY, 0.01f);

        img.dispose();
    }

    @Test
    void positionVisibilityAndOpacity() {
        Signal<Boolean> vis = Signal.of(true);
        Signal<Float> alpha = Signal.of(1f);

        NetworkImage img = new NetworkImage()
                .props(position(15f, 25f), visible(vis), opacity(alpha));

        assertEquals(15f, img.image().x, 0.01f);
        assertEquals(25f, img.image().y, 0.01f);
        assertTrue(img.image().visible);
        assertEquals(1f, img.image().color.a, 0.01f);

        vis.set(false);
        alpha.set(0.4f);
        SignalDispatcher.flush();

        assertFalse(img.image().visible);
        assertEquals(0.4f, img.image().color.a, 0.01f);

        img.dispose();

        vis.set(true);
        alpha.set(0.9f);
        SignalDispatcher.flush();

        assertFalse(img.image().visible);
        assertEquals(0.4f, img.image().color.a, 0.01f);
    }

    @Test
    void tableCellSizeAndBoundsConstraints() {
        Table table = new Table();
        NetworkImage img = new NetworkImage()
                .props(minWidth(10f), maxWidth(100f), minHeight(15f), maxHeight(80f));

        Cell<?> cell = table.add(img.element());
        img.cellConfig().applyToCell(cell);

        assertEquals(10f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(100f, CellAccess.maxWidth(cell), 0.01f);
        assertEquals(15f, CellAccess.minHeight(cell), 0.01f);
        assertEquals(80f, CellAccess.maxHeight(cell), 0.01f);

        img.props(minWidth(45f), maxWidth(45f), minHeight(35f), maxHeight(35f));
        assertEquals(45f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(35f, CellAccess.minHeight(cell), 0.01f);
        assertEquals(45f, CellAccess.maxWidth(cell), 0.01f,
                "size(w, h) must fix the cell width, not merely bound it");
        assertEquals(35f, CellAccess.maxHeight(cell), 0.01f,
                "size(w, h) must fix the cell height, not merely bound it");

        img.dispose();
    }

    @Test
    void tableCellGrowBehavior() {
        Table table = new Table();
        NetworkImage img = new NetworkImage();
        Cell<?> cell = table.add(img.element());

        img.props(growX());
        assertEquals(1, CellAccess.expandX(cell));
        assertEquals(1f, CellAccess.fillX(cell), 0.01f);
        assertEquals(0, CellAccess.expandY(cell));

        img.props(growY());
        assertEquals(1, CellAccess.expandY(cell));
        assertEquals(1f, CellAccess.fillY(cell), 0.01f);

        img.dispose();
    }

    @Test
    void tableCellMargins() {
        Table table = new Table();
        NetworkImage img = new NetworkImage()
                .props(margin(10f));

        Cell<?> cell = table.add(img.element());
        img.cellConfig().applyToCell(cell);

        assertEquals(10f, CellAccess.padTop(cell), 0.01f);
        assertEquals(10f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(10f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(10f, CellAccess.padRight(cell), 0.01f);

        img.props(margin(2f, 4f, 6f, 8f));
        assertEquals(2f, CellAccess.padTop(cell), 0.01f);
        assertEquals(4f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(6f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(8f, CellAccess.padRight(cell), 0.01f);

        img.props(marginTop(12f));
        assertEquals(12f, CellAccess.padTop(cell), 0.01f);

        img.props(marginLeft(14f));
        assertEquals(14f, CellAccess.padLeft(cell), 0.01f);

        img.props(marginBottom(16f));
        assertEquals(16f, CellAccess.padBottom(cell), 0.01f);

        img.props(marginRight(18f));
        assertEquals(18f, CellAccess.padRight(cell), 0.01f);

        img.props(marginX(20f));
        assertEquals(20f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(20f, CellAccess.padRight(cell), 0.01f);

        img.props(marginY(22f));
        assertEquals(22f, CellAccess.padTop(cell), 0.01f);
        assertEquals(22f, CellAccess.padBottom(cell), 0.01f);

        img.dispose();
    }

    @Test
    void tableCellReactiveMargins() {
        Table table = new Table();
        Signal<Float> topPad = Signal.of(5f);

        NetworkImage img = new NetworkImage().props(marginTop(topPad));
        Cell<?> cell = table.add(img.element());
        List<Disposable> cellDisposables = img.cellConfig().applyToCell(cell);

        assertEquals(5f, CellAccess.padTop(cell), 0.01f);

        topPad.set(25f);
        SignalDispatcher.flush();
        assertEquals(25f, CellAccess.padTop(cell), 0.01f);

        img.dispose();
        for (Disposable d : cellDisposables) {
            d.dispose();
        }

        topPad.set(50f);
        SignalDispatcher.flush();

        assertEquals(25f, CellAccess.padTop(cell), 0.01f);
    }

    @Test
    void declarativeAndImperativeAlignment() {
        Table table = new Table();

        NetworkImage declarativeImg = new NetworkImage().props(top(), left());
        Cell<?> cell1 = table.add(declarativeImg.element());
        declarativeImg.cellConfig().applyToCell(cell1);

        assertEquals(Align.top | Align.left, CellAccess.align(cell1));

        NetworkImage centerImg = new NetworkImage().props(center());
        Cell<?> cell2 = table.add(centerImg.element());
        centerImg.cellConfig().applyToCell(cell2);

        assertEquals(Align.center, CellAccess.align(cell2));

        NetworkImage imperativeImg = new NetworkImage();
        Cell<?> cell3 = table.add(imperativeImg.element());
        imperativeImg.props(bottom(), right());

        assertEquals(Align.bottom | Align.right, CellAccess.align(cell3));

        declarativeImg.dispose();
        centerImg.dispose();
        imperativeImg.dispose();
    }

    @Test
    void parentStackMountingIntegration() {
        Table root = new Table();
        AttachmentStack.push(root);

        NetworkImage img = new NetworkImage()
                .props(size(42f, 38f))
                .props(margin(8f), growX());

        AttachmentStack.add(img);
        AttachmentStack.pop();

        assertEquals(1, root.getChildren().size);
        assertSame(img.element(), root.getChildren().first());

        Cell<?> cell = root.getCell(img.element());
        assertNotNull(cell);
        assertEquals(42f, img.element().getWidth(), 0.01f);
        assertEquals(38f, img.element().getHeight(), 0.01f);
        assertEquals(8f, CellAccess.padTop(cell), 0.01f);
        assertEquals(8f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(8f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(8f, CellAccess.padRight(cell), 0.01f);
        assertEquals(1, CellAccess.expandX(cell));
        assertEquals(1f, CellAccess.fillX(cell), 0.01f);

        img.dispose();
    }

    @Test
    void networkImageRoundedRadiusTracking() {
        NetworkImage img = new NetworkImage().props(imageRounded(12));
        assertEquals(12, img.getCornerRadius());
        img.dispose();
    }

    @Test
    void networkImageRoundedCacheSeparation() {
        TextureRegion regionSquare = new TextureRegion();
        TextureRegion regionRounded = new TextureRegion();

        NetworkImage.setImageLoader((url, radius, targetW, targetH, onSuccess, onError) -> {
            if (radius > 0) {
                onSuccess.get(regionRounded);
            } else {
                onSuccess.get(regionSquare);
            }
        });

        String url = "https://example.com/user.png";
        NetworkImage img1 = new NetworkImage(url);
        NetworkImage img2 = new NetworkImage(url).props(imageRounded(8));

        assertTrue(NetworkImage.isCached(url, 0));
        assertTrue(NetworkImage.isCached(url, 8));
        assertSame(regionSquare, NetworkImage.getCached(url, 0).getRegion());
        assertSame(regionRounded, NetworkImage.getCached(url, 8).getRegion());

        img1.dispose();
        img2.dispose();
    }

    @Test
    void networkImageApplyRoundedMask() {
        int width = 30;
        int height = 30;
        int radius = 8;
        Pixmap pixmap = new Pixmap(width, height);
        int opaqueWhite = Color.rgba8888(1f, 1f, 1f, 1f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.set(x, y, opaqueWhite);
            }
        }

        NetworkImage.applyRoundedMask(pixmap, radius);

        assertEquals(0, pixmap.get(0, 0) & 0xFF, "Top-left tip must be transparent");
        assertEquals(0, pixmap.get(width - 1, 0) & 0xFF, "Top-right tip must be transparent");
        assertEquals(0, pixmap.get(0, height - 1) & 0xFF, "Bottom-left tip must be transparent");
        assertEquals(0, pixmap.get(width - 1, height - 1) & 0xFF, "Bottom-right tip must be transparent");

        assertEquals(255, pixmap.get(width / 2, height / 2) & 0xFF, "Center must remain fully opaque");

        assertEquals(255, pixmap.get(width / 2, 0) & 0xFF, "Top middle edge must remain opaque");
        assertEquals(255, pixmap.get(0, height / 2) & 0xFF, "Left middle edge must remain opaque");

        int cornerInteriorAlpha = pixmap.get(radius - 1, radius - 1) & 0xFF;
        assertTrue(cornerInteriorAlpha > 0, "Inside corner should preserve opacity");

        pixmap.dispose();
    }

    @Test
    void networkImageApplyRoundedMaskWithTargetScaling() {

        int width = 60;
        int height = 60;
        int displayRadius = 8;
        Pixmap pixmap = new Pixmap(width, height);
        int opaqueWhite = Color.rgba8888(1f, 1f, 1f, 1f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.set(x, y, opaqueWhite);
            }
        }

        NetworkImage.applyRoundedMask(pixmap, displayRadius, 30f, 30f);

        assertEquals(0, pixmap.get(0, 0) & 0xFF);

        int scaledCornerAlpha = pixmap.get(2, 2) & 0xFF;
        assertTrue(scaledCornerAlpha < 255, "Should be within the scaled corner curvature");

        assertEquals(255, pixmap.get(30, 30) & 0xFF);

        pixmap.dispose();
    }
}
