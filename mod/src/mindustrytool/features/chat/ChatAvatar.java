package mindustrytool.features.chat;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import solim.core.Component;
import solim.display.SolimImage;
import solim.layout.Column;
import solim.layout.SolimStack;
import solim.reactive.Readable;

public class ChatAvatar extends Component {

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
                .props(size(size, size), prefWidth(size), prefHeight(size))
                .layer(() -> {
                    Column fallback = column()
                            .props(size(size, size), prefWidth(size), prefHeight(size),
                                    radius > 0 ? solim.UI.rounded(radius, background) : background(background), center());

                    return fallback
                            .children(() -> {
                                text(initial)
                                        .props(color(Color.white), fontScale(size >= 32f ? 1.2f : 0.9f));
                            });
                })
                .layer(() -> {
                    SolimImage img = image().props(src(avatarUrl));
                    if (radius > 0) {
                        img.props(size(size, size), top(), left(), imageRounded(radius));
                    } else {
                        img.props(size(size, size), top(), left());
                    }
                    return img;
                });
        return stack.element();
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
