package solim.style;

import arc.graphics.g2d.Font;
import arc.scene.ui.Button.ButtonStyle;
import arc.util.Nullable;

public final class SolimButtonStyle {

    private final ButtonStyle style;
    private final @Nullable Float padding;
    private final @Nullable Float margin;
    private final @Nullable Float gap;
    private final @Nullable Font font;

    public SolimButtonStyle(ButtonStyle style, @Nullable Float padding,
            @Nullable Float margin, @Nullable Float gap, @Nullable Font font) {
        this.style = style != null ? style : new ButtonStyle();
        this.padding = padding;
        this.margin = margin;
        this.gap = gap;
        this.font = font;
    }

    public ButtonStyle style() {
        return style;
    }

    public @Nullable Float padding() {
        return padding;
    }

    public @Nullable Float margin() {
        return margin;
    }

    public @Nullable Float gap() {
        return gap;
    }

    public @Nullable Font font() {
        return font;
    }
}
