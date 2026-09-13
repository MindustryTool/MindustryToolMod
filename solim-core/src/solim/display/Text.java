package solim.display;

import arc.Core;
import arc.graphics.Color;
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
import solim.core.SpacingAware;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/** Display widget for text content. */
public final class Text implements Component, SpacingAware {

    private final Label label;
    private final List<Disposable> bindings = new ArrayList<>();

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

	public Text(String text, Label.LabelStyle style) {
		this.label = style != null
				? new Label(text != null ? text : "", style)
				: (Core.scene != null ? new Label(text != null ? text : "") : new Label(text != null ? text : "", new Label.LabelStyle()));
		this.label.name = "solim-text-label";
	}

    public static Text of(String text) {
        return new Text(text);
    }

    public static Text of(Signal<String> signal) {
        return of((Readable<String>) signal);
    }

    public static Text of(Computed<String> computed) {
        return of((Readable<String>) computed);
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
        label.userObject = "expanding";
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
        label.userObject = "expanding";
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

    public Text size(float width, float height) {
        ElementConfig.size(label, width, height);
        return this;
    }

    public Text size(float size) {
        ElementConfig.size(label, size);
        return this;
    }

    public Text x(float x) {
        ElementConfig.x(label, x);
        return this;
    }

    public Text y(float y) {
        ElementConfig.y(label, y);
        return this;
    }

    public Text position(float x, float y) {
        ElementConfig.position(label, x, y);
        return this;
    }

    public Text visible(boolean visible) {
        ElementConfig.visible(label, visible);
        return this;
    }

    public Text visible(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            Effect e = Effect.of(() -> ElementConfig.visible(label, Boolean.TRUE.equals(signal.get())));
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
    public Element element() {
        applySpacing();
        return label;
    }

    @Override
    public Text name(String name) {
        ElementConfig.name(label, name);
        return this;
    }

    @Override
    public void dispose() {
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }
}
