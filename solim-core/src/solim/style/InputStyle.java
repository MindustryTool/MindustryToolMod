package solim.style;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.scene.style.Drawable;
import arc.scene.ui.TextField;
import arc.util.Nullable;

/**
 * Immutable textfield chrome preset. Every slot is optional: unset slots
 * fall back to the field's base style when applied, so presets only declare
 * what they change.
 */
public final class InputStyle {

    private final @Nullable Drawable background;
    private final @Nullable Drawable focusedBackground;
    private final @Nullable Drawable disabledBackground;
    private final @Nullable Drawable invalidBackground;
    private final @Nullable Drawable cursor;
    private final @Nullable Drawable selection;
    private final @Nullable Font font;
    private final @Nullable Color fontColor;
    private final @Nullable Color focusedFontColor;
    private final @Nullable Color disabledFontColor;
    private final @Nullable Font messageFont;
    private final @Nullable Color messageFontColor;

    private InputStyle(Builder builder) {
        this.background = builder.background;
        this.focusedBackground = builder.focusedBackground;
        this.disabledBackground = builder.disabledBackground;
        this.invalidBackground = builder.invalidBackground;
        this.cursor = builder.cursor;
        this.selection = builder.selection;
        this.font = builder.font;
        this.fontColor = builder.fontColor;
        this.focusedFontColor = builder.focusedFontColor;
        this.disabledFontColor = builder.disabledFontColor;
        this.messageFont = builder.messageFont;
        this.messageFontColor = builder.messageFontColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public @Nullable Drawable background() {
        return background;
    }

    public @Nullable Drawable focusedBackground() {
        return focusedBackground;
    }

    public @Nullable Drawable disabledBackground() {
        return disabledBackground;
    }

    public @Nullable Drawable invalidBackground() {
        return invalidBackground;
    }

    public @Nullable Drawable cursor() {
        return cursor;
    }

    public @Nullable Drawable selection() {
        return selection;
    }

    public @Nullable Font font() {
        return font;
    }

    public @Nullable Color fontColor() {
        return fontColor;
    }

    public @Nullable Color focusedFontColor() {
        return focusedFontColor;
    }

    public @Nullable Color disabledFontColor() {
        return disabledFontColor;
    }

    public @Nullable Font messageFont() {
        return messageFont;
    }

    public @Nullable Color messageFontColor() {
        return messageFontColor;
    }

    /**
     * Builds a complete per-instance style from the given base: starts from
     * Arc's copy of the base, tops up the slots Arc's copy constructor drops
     * ({@code invalidBackground}), then overrides the declared slots.
     * Unset slots therefore always fall back to base values.
     */
    public TextField.TextFieldStyle appliedTo(@Nullable TextField.TextFieldStyle base) {
        TextField.TextFieldStyle copy = base != null
                ? new TextField.TextFieldStyle(base)
                : new TextField.TextFieldStyle();
        if (base != null && invalidBackground == null) {
            copy.invalidBackground = base.invalidBackground;
        }
        applyTo(copy);
        return copy;
    }

    /**
     * Overrides only the declared slots on the given non-null style, leaving
     * the rest untouched so unset slots keep their base values. Prefer
     * {@link #appliedTo(TextField.TextFieldStyle)} when creating per-instance
     * copies, since Arc's copy constructor drops {@code invalidBackground}.
     */
    public void applyTo(TextField.TextFieldStyle style) {
        if (background != null) {
            style.background = background;
        }
        if (focusedBackground != null) {
            style.focusedBackground = focusedBackground;
        }
        if (disabledBackground != null) {
            style.disabledBackground = disabledBackground;
        }
        if (invalidBackground != null) {
            style.invalidBackground = invalidBackground;
        }
        if (cursor != null) {
            style.cursor = cursor;
        }
        if (selection != null) {
            style.selection = selection;
        }
        if (font != null) {
            style.font = font;
        }
        if (fontColor != null) {
            style.fontColor = fontColor;
        }
        if (focusedFontColor != null) {
            style.focusedFontColor = focusedFontColor;
        }
        if (disabledFontColor != null) {
            style.disabledFontColor = disabledFontColor;
        }
        if (messageFont != null) {
            style.messageFont = messageFont;
        }
        if (messageFontColor != null) {
            style.messageFontColor = messageFontColor;
        }
    }

    public static final class Builder {
        private @Nullable Drawable background;
        private @Nullable Drawable focusedBackground;
        private @Nullable Drawable disabledBackground;
        private @Nullable Drawable invalidBackground;
        private @Nullable Drawable cursor;
        private @Nullable Drawable selection;
        private @Nullable Font font;
        private @Nullable Color fontColor;
        private @Nullable Color focusedFontColor;
        private @Nullable Color disabledFontColor;
        private @Nullable Font messageFont;
        private @Nullable Color messageFontColor;

        private Builder() {
        }

        public Builder background(@Nullable Drawable background) {
            this.background = background;
            return this;
        }

        public Builder focusedBackground(@Nullable Drawable focusedBackground) {
            this.focusedBackground = focusedBackground;
            return this;
        }

        public Builder disabledBackground(@Nullable Drawable disabledBackground) {
            this.disabledBackground = disabledBackground;
            return this;
        }

        public Builder invalidBackground(@Nullable Drawable invalidBackground) {
            this.invalidBackground = invalidBackground;
            return this;
        }

        public Builder cursor(@Nullable Drawable cursor) {
            this.cursor = cursor;
            return this;
        }

        public Builder selection(@Nullable Drawable selection) {
            this.selection = selection;
            return this;
        }

        public Builder font(@Nullable Font font) {
            this.font = font;
            return this;
        }

        public Builder fontColor(@Nullable Color fontColor) {
            this.fontColor = fontColor;
            return this;
        }

        public Builder focusedFontColor(@Nullable Color focusedFontColor) {
            this.focusedFontColor = focusedFontColor;
            return this;
        }

        public Builder disabledFontColor(@Nullable Color disabledFontColor) {
            this.disabledFontColor = disabledFontColor;
            return this;
        }

        public Builder messageFont(@Nullable Font messageFont) {
            this.messageFont = messageFont;
            return this;
        }

        public Builder messageFontColor(@Nullable Color messageFontColor) {
            this.messageFontColor = messageFontColor;
            return this;
        }

        public InputStyle build() {
            return new InputStyle(this);
        }
    }
}
