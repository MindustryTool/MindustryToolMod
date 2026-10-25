package solim.layout;

import arc.scene.ui.layout.Table;
import solim.graphics.BlurRenderer;

public class BlurTable extends Table {

    private float contentBlurRadius = 0f;

    public BlurTable() {
        super();
    }

    public void contentBlurRadius(float radius) {
        this.contentBlurRadius = Math.max(0f, radius);
    }

    public float contentBlurRadius() {
        return contentBlurRadius;
    }

    @Override
    public void draw() {
        if (contentBlurRadius <= 0.01f || !BlurRenderer.get().isEnabled()) {
            super.draw();
            return;
        }
        BlurRenderer.get().beginContentBlur(this);
        super.draw();
        BlurRenderer.get().endContentBlur(this, contentBlurRadius);
    }
}
