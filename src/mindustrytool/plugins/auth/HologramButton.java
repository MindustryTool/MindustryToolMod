package mindustrytool.plugins.auth;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class HologramButton extends Button {
    private static final Color HOLOGRAM_PRIMARY = Color.valueOf("00d4ff");
    private static final Color HOLOGRAM_GLOW = Color.valueOf("00ffff");

    public HologramButton(String text, Runnable onClick) {
        super(Styles.defaultb);
        clicked(onClick);

        // Remove default background
        getStyle().up = null;
        getStyle().down = null;
        getStyle().over = null;
        getStyle().disabled = null;

        clearChildren();
        add(text); // Add text centered
    }

    @Override
    public void draw() {
        boolean hovered = hasMouse();
        boolean pressed = isPressed();

        float w = getWidth();
        float h = getHeight();
        float cx = x + w / 2f;
        float cy = y + h / 2f;

        // Animate pulse
        float pulse = (float) Math.sin(Time.time / (hovered ? 10f : 20f)) * 0.1f + 0.9f;
        float alphaScale = pressed ? 0.7f : 1f;

        // Background Glow (faint)
        Draw.color(HOLOGRAM_PRIMARY, (hovered ? 0.3f : 0.1f) * pulse * alphaScale);
        Fill.rect(cx, cy, w, h);

        // Border / Frame
        Draw.color(pressed ? HOLOGRAM_GLOW : HOLOGRAM_PRIMARY, alphaScale);
        Lines.stroke(2f);

        // Bracket style corners
        float len = 10f;
        // Top-Left
        Lines.line(x, y + h, x + len, y + h);
        Lines.line(x, y + h, x, y + h - len);

        // Top-Right
        Lines.line(x + w, y + h, x + w - len, y + h);
        Lines.line(x + w, y + h, x + w, y + h - len);

        // Bottom-Left
        Lines.line(x, y, x + len, y);
        Lines.line(x, y, x, y + len);

        // Bottom-Right
        Lines.line(x + w, y, x + w - len, y);
        Lines.line(x + w, y, x + w, y + len);

        Draw.reset();

        super.draw();
    }
}
