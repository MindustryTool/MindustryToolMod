package mindustrytool.features.social.auth;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Icon;

/**
 * Custom Hologram Profile Widget
 * Renders avatar and name with beautiful neon glow effects
 */
public class HologramProfileWidget extends Table {
    private static final Color HOLOGRAM_PRIMARY = Color.valueOf("00d4ff"); // Cyan
    private static final Color HOLOGRAM_SECONDARY = Color.valueOf("0099cc"); // Darker cyan
    private static final Color HOLOGRAM_GLOW = Color.valueOf("00ffff"); // Bright cyan

    private Image avatarImage;
    private Label nameLabel;
    private float avatarSize = 96f; // Larger avatar
    private float namePadding = 6f; // Reduced spacing

    public HologramProfileWidget() {
        super();
        this.touchable = arc.scene.event.Touchable.enabled;
    }

    public void setAvatar(TextureRegion region) {
        if (avatarImage != null) {
            avatarImage.setDrawable(new TextureRegionDrawable(region));
        }
    }

    public void setName(String name) {
        if (nameLabel != null) {
            nameLabel.setText(name);
        }
    }

    public void build(TextureRegion avatarRegion, String name, Runnable onClick) {
        clear();
        defaults().pad(0);

        // Avatar container
        Table avatarContainer = new Table() {
            @Override
            public void draw() {
                try {
                    // === HOVER EFFECT STATE ===
                    boolean hovered = hasMouse();
                    float hoverScale = hovered ? 1.1f : 1.0f;

                    // Lerp pulse speed: Fast pulse when hovered
                    float pulseSpeed = hovered ? 15f : 30f;
                    float pulse = (float) Math.sin(Time.time / pulseSpeed) * 0.15f + 0.85f;

                    float cx = x + width / 2f;
                    float cy = y + height / 2f;

                    // Outer glow
                    Draw.color(HOLOGRAM_GLOW, 0.1f * pulse * hoverScale);
                    Fill.rect(cx, cy, (width + 24f) * hoverScale, (height + 24f) * hoverScale);

                    // Mid glow
                    Draw.color(HOLOGRAM_PRIMARY, (hovered ? 0.3f : 0.15f) * pulse);
                    Fill.rect(cx, cy, (width + 12f) * hoverScale, (height + 12f) * hoverScale);

                    // Inner glow
                    Draw.color(HOLOGRAM_SECONDARY, (hovered ? 0.5f : 0.25f) * pulse);
                    Fill.rect(cx, cy, (width + 4f) * hoverScale, (height + 4f) * hoverScale);

                    Draw.reset();
                    super.draw();

                    // Draw neon border ABOVE the content
                    Lines.stroke(hovered ? 4f : 2.5f); // Thicker border on hover
                    Draw.color(HOLOGRAM_PRIMARY, (hovered ? 1f : 0.9f) * pulse);

                    // Draw rect scaled around center
                    float bw = width * hoverScale;
                    float bh = height * hoverScale;
                    Lines.rect(cx - bw / 2f - 2f, cy - bh / 2f - 2f, bw + 4f, bh + 4f);

                    // Corner accents
                    float accentLen = 10f * hoverScale;
                    Lines.stroke(hovered ? 4f : 3f);
                    Draw.color(HOLOGRAM_GLOW, pulse);

                    // Calculate corner coords based on scaled size
                    float left = cx - bw / 2f - 4f;
                    float right = cx + bw / 2f + 4f;
                    float bottom = cy - bh / 2f - 4f;
                    float top = cy + bh / 2f + 4f;

                    // Top-left
                    Lines.line(left, top, left + accentLen, top);
                    Lines.line(left, top, left, top - accentLen);

                    // Top-right
                    Lines.line(right, top, right - accentLen, top);
                    Lines.line(right, top, right, top - accentLen);

                    // Bottom-left
                    Lines.line(left, bottom, left + accentLen, bottom);
                    Lines.line(left, bottom, left, bottom + accentLen);

                    // Bottom-right
                    Lines.line(right, bottom, right - accentLen, bottom);
                    Lines.line(right, bottom, right, bottom + accentLen);

                    // === CRT SCANLINE EFFECT ===
                    // Multiple thin lines scrolling down (TV effect)
                    float lineHeight = 1f;
                    float spacing = 4f;
                    float scrollSpeed = 0.2f; // Pixels per frame (Slower)
                    float offset = (Time.time * scrollSpeed) % spacing;

                    Draw.color(HOLOGRAM_GLOW, 0.1f); // Very transparent base

                    // Draw lines across the entire height
                    for (float ly = y; ly < y + height; ly += spacing) {
                        float drawY = ly + offset;
                        // Wrap around if it goes past the top (visually keeps pattern consistent)
                        if (drawY > y + height)
                            drawY -= height;

                        // Only draw if within bounds
                        if (drawY >= y && drawY <= y + height) {
                            Fill.rect(cx, drawY, width, lineHeight);
                        }
                    }

                    // Sweeping bar (Classic CRT Scanline)
                    float sweepSpeed = 200f; // Much slower
                    float sweepY = y + height - ((Time.time % sweepSpeed) / sweepSpeed) * height;
                    Draw.color(HOLOGRAM_PRIMARY, 0.1f); // Subtle
                    Fill.rect(cx, sweepY, width, 8f);

                    // === PARTICLE GRAIN NOISE ===
                    // Small random static particles (Subtle & Slow)
                    long seed = (long) (Time.time / 6f);
                    java.util.Random rng = new java.util.Random(seed);

                    int particleCount = 10;
                    for (int i = 0; i < particleCount; i++) {
                        float px = x + rng.nextFloat() * width;
                        float py = y + rng.nextFloat() * height;

                        float pSize = 1f + rng.nextFloat();
                        float pAlpha = 0.05f + rng.nextFloat() * 0.15f;

                        Draw.color(HOLOGRAM_GLOW, pAlpha);
                        Fill.square(px, py, pSize / 2f);
                    }

                    // === DATA PARTICLES ===
                    updateAndDrawParticles(x, y, width, height);

                    Draw.reset();
                } catch (Exception e) {
                    // Log error to console only once to avoid spam
                    if (Time.time % 60 == 0)
                        e.printStackTrace();
                    Draw.reset();
                    super.draw(); // Fallback
                }
            }
        };

        if (avatarRegion != null) {
            avatarImage = avatarContainer.image(new TextureRegionDrawable(avatarRegion)).size(avatarSize).get();
        } else {
            avatarImage = avatarContainer.image(Icon.players).size(avatarSize).get();
        }
        avatarContainer.defaults().pad(8f);

        add(avatarContainer).padBottom(namePadding).row();

        // Name plate with hologram effect
        Table namePlate = new Table() {
            @Override
            public void draw() {
                float cx = x + width / 2f;
                float cy = y + height / 2f;
                float pulse = (float) Math.sin(Time.time / 25f) * 0.1f + 0.9f;

                // Glow behind text
                Draw.color(HOLOGRAM_PRIMARY, 0.2f * pulse);
                Fill.rect(cx, cy, width + 16f, height + 8f);

                // Semi-transparent background
                Draw.color(0f, 0f, 0f, 0.6f);
                Fill.rect(cx, cy, width, height);

                Draw.reset();
                super.draw();

                // Corner Bracket Style (same as HologramButton)
                Draw.color(HOLOGRAM_PRIMARY, pulse);
                Lines.stroke(2f);

                float len = 10f;
                // Top-Left
                Lines.line(x, y + height, x + len, y + height);
                Lines.line(x, y + height, x, y + height - len);

                // Top-Right
                Lines.line(x + width, y + height, x + width - len, y + height);
                Lines.line(x + width, y + height, x + width, y + height - len);

                // Bottom-Left
                Lines.line(x, y, x + len, y);
                Lines.line(x, y, x, y + len);

                // Bottom-Right
                Lines.line(x + width, y, x + width - len, y);
                Lines.line(x + width, y, x + width, y + len);

                Draw.reset();
            }
        };

        nameLabel = namePlate.add(name).color(HOLOGRAM_GLOW).padLeft(16f).padRight(16f).get();
        nameLabel.setFontScale(1.3f);
        namePlate.defaults().pad(6f);

        // Match LOGIN button size (180f x 60f)
        add(namePlate).size(180f, 60f).row();

        // Click handler
        if (onClick != null) {
            clicked(onClick);
        }
    }

    // === DATA PARTICLE SYSTEM ===
    private Seq<DataParticle> particles = new Seq<>();

    private class DataParticle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        float size;

        void reset(float boundsX, float boundsY, float boundsW, float boundsH) {
            // Spawn anywhere around the center, slightly outside or inside
            float angle = Mathf.random(360f);
            float dist = Mathf.random(boundsW / 2f, boundsW / 2f + 20f);

            x = boundsX + boundsW / 2f + Mathf.cosDeg(angle) * dist;
            y = boundsY + boundsH / 2f + Mathf.sinDeg(angle) * dist;

            // Orbit/Float velocity
            vx = Mathf.random(-0.5f, 0.5f);
            vy = Mathf.random(0.2f, 0.8f); // Tendency to float up

            life = 0f;
            maxLife = Mathf.random(60f, 120f);
            size = Mathf.random(1.5f, 3f);
        }
    }

    private void updateAndDrawParticles(float x, float y, float w, float h) {
        // Spawn
        if (Mathf.chance(0.1)) { // 10% chance per frame
            DataParticle p = particles.size > 20 ? particles.first() : new DataParticle();
            if (particles.size > 20)
                particles.remove(0);
            p.reset(x, y, w, h);
            particles.add(p);
        }

        Draw.color(HOLOGRAM_GLOW);

        for (DataParticle p : particles) {
            // Update
            p.x += p.vx;
            p.y += p.vy;
            p.life++;

            // Draw
            float alpha = 1f;
            if (p.life < 20)
                alpha = p.life / 20f; // Fade in
            if (p.life > p.maxLife - 20)
                alpha = (p.maxLife - p.life) / 20f; // Fade out

            if (p.life >= p.maxLife) {
                // Reset dead particles immediately to save allocation?
                // Creating simplified logic for now: just don't draw if dead, cleanup happens
                // via spawn replacement or could use iterator
                continue;
            }

            Draw.alpha(alpha * 0.6f);
            Fill.square(p.x, p.y, p.size / 2f);
        }
        Draw.reset();

        // Cleanup dead
        for (int i = particles.size - 1; i >= 0; i--) {
            if (particles.get(i).life >= particles.get(i).maxLife) {
                particles.remove(i);
            }
        }
    }
}
