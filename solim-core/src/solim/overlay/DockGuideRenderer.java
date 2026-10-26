package solim.overlay;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import arc.scene.event.Touchable;
import mindustry.graphics.Pal;

public class DockGuideRenderer extends Element {

    private static final float GHOST_ALPHA = 0.25f;
    private static final float SEAM_THICKNESS = 2f;

    private boolean attached;
    private boolean shown;

    private float ghostX;
    private float ghostY;
    private float ghostWidth;
    private float ghostHeight;

    private float seamX;
    private float seamY;
    private float seamWidth;
    private float seamHeight;

    private float secondSeamX;
    private float secondSeamY;
    private float secondSeamWidth;
    private float secondSeamHeight;

    public DockGuideRenderer() {
        touchable = Touchable.disabled;
        visible = false;
    }

    public boolean isShown() {
        return shown;
    }

    public void show(DockCandidate candidate, float hudWidth, float hudHeight) {
        if (candidate == null || hudWidth <= 0f || hudHeight <= 0f) {
            hide();
            return;
        }
        shown = true;
        visible = true;
        ghostX = candidate.x();
        ghostY = candidate.y();
        ghostWidth = hudWidth;
        ghostHeight = hudHeight;
        computeSeams(candidate, hudWidth, hudHeight);
        attach();
    }

    public void hide() {
        shown = false;
        visible = false;
        detach();
    }

    @Override
    public void draw() {
        if (!shown) {
            return;
        }
        Draw.color(Pal.accent, GHOST_ALPHA);
        Draw.rect(Core.atlas.white(), ghostX + ghostWidth / 2f, ghostY + ghostHeight / 2f, ghostWidth, ghostHeight);
        Draw.color(Pal.accent);
        Draw.rect(Core.atlas.white(), seamX + seamWidth / 2f, seamY + seamHeight / 2f, seamWidth, seamHeight);
        if (secondSeamWidth > 0f && secondSeamHeight > 0f) {
            Draw.rect(Core.atlas.white(), secondSeamX + secondSeamWidth / 2f,
                    secondSeamY + secondSeamHeight / 2f, secondSeamWidth, secondSeamHeight);
        }
        Draw.reset();
    }

    private void computeSeams(DockCandidate candidate, float hudWidth, float hudHeight) {
        float x = candidate.x();
        float y = candidate.y();
        clearSeams();

        switch (candidate.side()) {
            case TOP:
                horizontalSeam(x, y, hudWidth);
                break;
            case BOTTOM:
                horizontalSeam(x, y + hudHeight - SEAM_THICKNESS, hudWidth);
                break;
            case LEFT:
                verticalSeam(x + hudWidth - SEAM_THICKNESS, y, hudHeight);
                break;
            case RIGHT:
                verticalSeam(x, y, hudHeight);
                break;
            case TOP_LEFT:
                horizontalSeam(x, y, hudWidth);
                verticalSeam(x + hudWidth - SEAM_THICKNESS, y, hudHeight);
                break;
            case TOP_RIGHT:
                horizontalSeam(x, y, hudWidth);
                verticalSeam(x, y, hudHeight);
                break;
            case BOTTOM_LEFT:
                horizontalSeam(x, y + hudHeight - SEAM_THICKNESS, hudWidth);
                verticalSeam(x + hudWidth - SEAM_THICKNESS, y, hudHeight);
                break;
            case BOTTOM_RIGHT:
                horizontalSeam(x, y + hudHeight - SEAM_THICKNESS, hudWidth);
                verticalSeam(x, y, hudHeight);
                break;
            case INSIDE:
                horizontalSeam(x, y + hudHeight - SEAM_THICKNESS, hudWidth);
                break;
            default:
                break;
        }
    }

    private void clearSeams() {
        seamWidth = 0f;
        seamHeight = 0f;
        secondSeamWidth = 0f;
        secondSeamHeight = 0f;
    }

    private void horizontalSeam(float x, float y, float width) {
        if (seamWidth > 0f && seamHeight > 0f) {
            secondSeamX = x;
            secondSeamY = y;
            secondSeamWidth = width;
            secondSeamHeight = SEAM_THICKNESS;
            return;
        }
        seamX = x;
        seamY = y;
        seamWidth = width;
        seamHeight = SEAM_THICKNESS;
    }

    private void verticalSeam(float x, float y, float height) {
        if (seamWidth > 0f && seamHeight > 0f) {
            secondSeamX = x;
            secondSeamY = y;
            secondSeamWidth = SEAM_THICKNESS;
            secondSeamHeight = height;
            return;
        }
        seamX = x;
        seamY = y;
        seamWidth = SEAM_THICKNESS;
        seamHeight = height;
    }

    private void attach() {
        if (attached) {
            toFront();
            return;
        }
        if (Core.scene == null || Core.scene.root == null) {
            return;
        }
        Core.scene.root.addChild(this);
        attached = true;
        toFront();
    }

    private void detach() {
        if (!attached) {
            return;
        }
        attached = false;
        remove();
    }
}
