package mindustrytool.features.chat;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import solim.core.BaseComponent;
import solim.display.NetworkImage;
import solim.layout.Column;
import solim.layout.SolimStack;
import solim.signal.Readable;

/**
 * Fixed-size user avatar: network photo on top of a deterministic
 * initial-letter fallback. Renders with continuous-curvature (Apple-style L4
 * superellipse) rounded corners on both the fallback badge and the network
 * image layer.
 */
public class ChatAvatar extends BaseComponent {

    private static final Color[] PALETTE = {
            Color.valueOf("ef5350"),
            Color.valueOf("ff9800"),
            Color.valueOf("9ccc65"),
            Color.valueOf("26a69a"),
            Color.valueOf("29b6f6"),
            Color.valueOf("7e57c2"),
            Color.valueOf("ec407a"),
            Color.valueOf("9e9d24")
    };

    private final Readable<String> displayName;
    private final Readable<String> avatarUrl;
    private final @Nullable String colorKey;
    private final float size;
    private int cornerRadius;

    public ChatAvatar(String displayName, String avatarUrl, @Nullable String colorKey, float size) {
        this(Readable.of(displayName), Readable.of(avatarUrl), colorKey, size);
    }

    public ChatAvatar(Readable<String> displayName, Readable<String> avatarUrl, @Nullable String colorKey,
            float size) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.colorKey = colorKey;
        this.size = size;
        this.cornerRadius = Math.max(2, Math.round(size * 0.35f));
    }

    public ChatAvatar rounded(int radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    @Override
    protected Element build() {
        Readable<String> initial = displayName.map(ChatAvatar::initialOf);
        Color background = colorFor(colorKey);
        int radius = cornerRadius;

        SolimStack stack = new SolimStack()
                .size(size, size)
                .layer(() -> {
                    Column fallback = column()
                            .size(size, size)
                            .center();

                    (radius > 0 ? fallback.rounded(radius, background) : fallback.background(background))
                            .children(() -> {
                                text(initial)
                                        .color(Color.white)
                                        .fontScale(size >= 32f ? 1.2f : 0.9f);
                            });
                })
                .layer(() -> {
                    NetworkImage img = networkImage(avatarUrl).size(size, size).top().left();
                    if (radius > 0) {
                        img.rounded(radius);
                    }
                });
        Element el = stack.element();
        el.userObject = stack;
        return el;
    }

    static String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String trimmed = name.trim();
        int codePoint = trimmed.codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase();
    }

    static Color colorFor(@Nullable String key) {
        int hash = (key != null ? key.hashCode() : 0) & 0x7fffffff;
        return PALETTE[hash % PALETTE.length];
    }
}
