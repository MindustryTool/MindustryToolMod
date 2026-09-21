package solim.display;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.core.SpacingAware;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.reactive.Effect;
import solim.reactive.Readable;

/** Display widget for text content. */
public final class Text implements Component, SpacingAware, ElementConfig<Text>, CellConfig<Text> {

    private final Label label;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;

    private float padTop;
    private float padLeft;
    private float padBottom;
    private float padRight;

    private float marginTop;
    private float marginLeft;
    private float marginBottom;
    private float marginRight;

    public Text() {
        this("");
    }

    public Text(String text) {
        this(text, (Label.LabelStyle) null);
    }

    private static @Nullable Label.LabelStyle defaultHeadlessStyle;

    private static Label.LabelStyle getDefaultStyle() {
        if (defaultHeadlessStyle == null) {
            FontData fontData = new FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }
            };
            Font font = new Font(fontData, new TextureRegion(), false);
            defaultHeadlessStyle = new Label.LabelStyle(font, Color.white);
        }
        return defaultHeadlessStyle;
    }

    public Text(String text, Label.LabelStyle style) {
        this.label = style != null
                ? new Label(text != null ? text : "", style)
                : (Core.scene != null ? new Label(text != null ? text : "")
                        : new Label(text != null ? text : "", getDefaultStyle()));
        this.label.name = "solim-text-label";
        SolimToken.bind(this.label, this, constraints);
        ComponentContext.register(this);
    }

    public static Text of(String text) {
        return new Text(text);
    }

    public static Text of(Readable<String> readable) {
        Text t = new Text();
        t.text(readable);
        return t;
    }

    public Text text(String text) {
        label.setText(text != null ? text : "");
        return this;
    }

    public Text text(Readable<String> text) {
        if (text != null) {
            Effect e = Effect.of(() -> label.setText(text.get() != null ? text.get() : ""));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Text color(Color color) {
        label.setColor(color);
        return this;
    }

    public Text color(Readable<Color> color) {
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    label.setColor(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Text style(Label.LabelStyle style) {
        if (style != null) {
            label.setStyle(style);
        }
        return this;
    }

    private boolean growX;
    private boolean growY;
    private boolean wrap;

    public Text growX() {
        this.growX = true;
        SolimToken.setExpanding(label, true);
        if (label.parent instanceof Table) {
            Cell<?> cell = ((Table) label.parent).getCell(label);
            if (cell != null) {
                cell.growX();
                cell.minWidth(0f);
                ((Table) label.parent).invalidateHierarchy();
            }
        }
        return this;
    }

    public Text growY() {
        this.growY = true;
        SolimToken.setExpanding(label, true);
        if (label.parent instanceof Table) {
            Cell<?> cell = ((Table) label.parent).getCell(label);
            if (cell != null) {
                cell.growY();
                ((Table) label.parent).invalidateHierarchy();
            }
        }
        return this;
    }

    public Text grow() {
        return growX().growY();
    }

    public Text wrap(boolean wrap) {
        this.wrap = wrap;
        label.setWrap(wrap);
        if (wrap) {
            growX();
            if (label.parent instanceof Table) {
                Cell<?> cell = ((Table) label.parent).getCell(label);
                if (cell != null) {
                    cell.minWidth(0f);
                }
            }
        }
        return this;
    }

    public boolean isWrap() {
        return wrap;
    }

    public Text wrap() {
        return wrap(true);
    }

    public Text ellipsis(boolean ellipsis) {
        label.setEllipsis(ellipsis);
        return this;
    }

    public Text ellipsis() {
        return ellipsis(true);
    }

    public Text align(int align) {
        label.setAlignment(align);
        return this;
    }

    public Text left() {
        return align(Align.left);
    }

    public Text center() {
        return align(Align.center);
    }

    public Text right() {
        return align(Align.right);
    }

    public Text padding(float pad) {
        this.padTop = this.padLeft = this.padBottom = this.padRight = pad;
        applySpacing();
        return this;
    }

    public Text padding(float top, float left, float bottom, float right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        applySpacing();
        return this;
    }

    public Text paddingTop(float top) {
        this.padTop = top;
        applySpacing();
        return this;
    }

    public Text paddingBottom(float bottom) {
        this.padBottom = bottom;
        applySpacing();
        return this;
    }

    public Text paddingLeft(float left) {
        this.padLeft = left;
        applySpacing();
        return this;
    }

    public Text paddingRight(float right) {
        this.padRight = right;
        applySpacing();
        return this;
    }

    public Text paddingX(float x) {
        this.padLeft = this.padRight = x;
        applySpacing();
        return this;
    }

    public Text paddingY(float y) {
        this.padTop = this.padBottom = y;
        applySpacing();
        return this;
    }

    public Text margin(float margin) {
        this.marginTop = this.marginLeft = this.marginBottom = this.marginRight = margin;
        applySpacing();
        return this;
    }

    public Text margin(float top, float left, float bottom, float right) {
        this.marginTop = top;
        this.marginLeft = left;
        this.marginBottom = bottom;
        this.marginRight = right;
        applySpacing();
        return this;
    }

    public Text marginTop(float top) {
        this.marginTop = top;
        applySpacing();
        return this;
    }

    public Text marginBottom(float bottom) {
        this.marginBottom = bottom;
        applySpacing();
        return this;
    }

    public Text marginLeft(float left) {
        this.marginLeft = left;
        applySpacing();
        return this;
    }

    public Text marginRight(float right) {
        this.marginRight = right;
        applySpacing();
        return this;
    }

    public Text marginX(float x) {
        this.marginLeft = this.marginRight = x;
        applySpacing();
        return this;
    }

    public Text marginY(float y) {
        this.marginTop = this.marginBottom = y;
        applySpacing();
        return this;
    }

    public void applySpacing() {
        if (label.parent instanceof Table) {
            Cell<?> cell = ((Table) label.parent).getCell(label);
            if (cell != null) {
                cell.pad(padTop + marginTop, padLeft + marginLeft, padBottom + marginBottom, padRight + marginRight);
                if (growX || wrap) {
                    cell.growX();
                    cell.minWidth(0f);
                }
                if (growY) {
                    cell.growY();
                }
            }
        }
    }

    public Text fontScale(float scale) {
        label.setFontScale(scale);
        return this;
    }

    public Text fontScale(Readable<Float> scale) {
        if (scale != null) {
            Effect e = Effect.of(() -> {
                Float s = scale.get();
                if (s != null) {
                    label.setFontScale(s);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Label label() {
        applySpacing();
        return label;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public Element element() {
        applySpacing();
        return label;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public Text self() {
        return this;
    }
}
