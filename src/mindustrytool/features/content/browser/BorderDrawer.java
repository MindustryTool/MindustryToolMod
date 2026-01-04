package mindustrytool.features.content.browser;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.Scl;

public final class BorderDrawer {
    private BorderDrawer() {}

    public static void draw(float x, float y, float w, float h, Color color, float thickness) {
        Draw.color(color);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, w, h);
        Draw.reset();
    }
}
