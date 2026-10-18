package solim.graphics;

import arc.graphics.g2d.Fill;
import arc.scene.style.BaseDrawable;

public class CircleDrawable extends BaseDrawable {
    public static final CircleDrawable INSTANCE = new CircleDrawable();

    public CircleDrawable() {

        setMinWidth(1f);
        setMinHeight(1f);
    }

    @Override
    public void draw(float x, float y, float width, float height) {
        float radius = Math.min(width, height) / 2f;
        Fill.circle(x + width / 2f, y + height / 2f, radius);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX,
            float scaleY, float rotation) {
        float radius = (Math.min(width, height) / 2f) * Math.min(scaleX, scaleY);
        Fill.circle(x + width / 2f, y + height / 2f, radius);
    }
}
