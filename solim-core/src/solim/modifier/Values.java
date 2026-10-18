package solim.modifier;

import arc.func.Cons;
import arc.func.Cons2;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Nullable;
import arc.scene.ui.Slider;
import arc.util.Scaling;
import java.util.List;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.HostCapability;
import solim.core.ReactiveSource;
import solim.core.SolimToken;
import solim.display.Badge;
import solim.display.ImageSource;
import solim.display.SolimImage;
import solim.display.Text;
import solim.display.video.SolimVideo;
import solim.display.video.VideoSource;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSelect;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.GapContainer;
import solim.layout.Grid;
import solim.layout.Scroll;
import solim.layout.SolimCollapser;
import solim.layout.Tabs;
import solim.overlay.Hud;
import solim.overlay.Popup;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;

public final class Values {

    private Values() {
    }

    public static Prop<ElementCapability, Boolean> growX() {
        return Prop.cell(ElementCapability.class, "growX", Boolean.TRUE, (config, v) -> config.growX = true);
    }

    public static Prop<ElementCapability, Boolean> growY() {
        return Prop.cell(ElementCapability.class, "growY", Boolean.TRUE, (config, v) -> config.growY = true);
    }

    public static Prop<ElementCapability, Boolean> grow() {
        return Prop.cell(ElementCapability.class, "grow", Boolean.TRUE, (config, v) -> {
            config.growX = true;
            config.growY = true;
        });
    }

    public static Prop<ElementCapability, Float> prefWidth(float v) {
        return Prop.cell(ElementCapability.class, "prefWidth", v, (config, val) -> config.prefWidth = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> prefWidth(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "prefWidth", v, (config, source) -> config.prefWidth = source);
    }

    public static Prop<ElementCapability, Float> prefHeight(float v) {
        return Prop.cell(ElementCapability.class, "prefHeight", v, (config, val) -> config.prefHeight = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> prefHeight(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "prefHeight", v, (config, source) -> config.prefHeight = source);
    }

    public static Prop<ElementCapability, Float> minWidth(float v) {
        return Prop.cell(ElementCapability.class, "minWidth", v, (config, val) -> config.minWidth = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> minWidth(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "minWidth", v, (config, source) -> config.minWidth = source);
    }

    public static Prop<ElementCapability, Float> maxWidth(float v) {
        return Prop.cell(ElementCapability.class, "maxWidth", v, (config, val) -> config.maxWidth = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> maxWidth(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "maxWidth", v, (config, source) -> config.maxWidth = source);
    }

    public static Prop<ElementCapability, Float> minHeight(float v) {
        return Prop.cell(ElementCapability.class, "minHeight", v, (config, val) -> config.minHeight = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> minHeight(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "minHeight", v, (config, source) -> config.minHeight = source);
    }

    public static Prop<ElementCapability, Float> maxHeight(float v) {
        return Prop.cell(ElementCapability.class, "maxHeight", v, (config, val) -> config.maxHeight = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> maxHeight(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "maxHeight", v, (config, source) -> config.maxHeight = source);
    }

    public static Prop<ElementCapability, Float> margin(float v) {
        return margin(v, v, v, v);
    }

    public static Prop<ElementCapability, Float> margin(float top, float left, float bottom, float right) {
        return Prop.cell(ElementCapability.class, "margin", top, (config, val) -> {
            config.padTop = Readable.of(top);
            config.padLeft = Readable.of(left);
            config.padBottom = Readable.of(bottom);
            config.padRight = Readable.of(right);
        });
    }

    public static Prop<ElementCapability, Float> margin(Readable<Float> v) {
        return margin(v, v, v, v);
    }

    public static Prop<ElementCapability, Float> margin(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        if (top != null) {
            return Prop.cellSource(ElementCapability.class, "margin", top, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (left != null) {
            return Prop.cellSource(ElementCapability.class, "margin", left, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (bottom != null) {
            return Prop.cellSource(ElementCapability.class, "margin", bottom, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (right != null) {
            return Prop.cellSource(ElementCapability.class, "margin", right, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        return Prop.cell(ElementCapability.class, "margin", 0f, (config, val) -> {
        });
    }

    public static Prop<ElementCapability, Float> marginTop(float v) {
        return Prop.cell(ElementCapability.class, "marginTop", v, (config, val) -> config.padTop = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> marginTop(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "marginTop", v, (config, source) -> config.padTop = source);
    }

    public static Prop<ElementCapability, Float> marginBottom(float v) {
        return Prop.cell(ElementCapability.class, "marginBottom", v, (config, val) -> config.padBottom = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> marginBottom(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "marginBottom", v, (config, source) -> config.padBottom = source);
    }

    public static Prop<ElementCapability, Float> marginLeft(float v) {
        return Prop.cell(ElementCapability.class, "marginLeft", v, (config, val) -> config.padLeft = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> marginLeft(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "marginLeft", v, (config, source) -> config.padLeft = source);
    }

    public static Prop<ElementCapability, Float> marginRight(float v) {
        return Prop.cell(ElementCapability.class, "marginRight", v, (config, val) -> config.padRight = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> marginRight(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "marginRight", v, (config, source) -> config.padRight = source);
    }

    public static Prop<ElementCapability, Float> marginX(float v) {
        return Prop.cell(ElementCapability.class, "marginX", v, (config, val) -> {
            config.padLeft = Readable.of(val);
            config.padRight = Readable.of(val);
        });
    }

    public static Prop<ElementCapability, Float> marginX(@Nullable Readable<Float> v) {
        if (v == null) {
            return Prop.cell(ElementCapability.class, "marginX", 0f, (config, val) -> {
            });
        }
        return Prop.cellSource(ElementCapability.class, "marginX", v, (config, source) -> {
            config.padLeft = source;
            config.padRight = source;
        });
    }

    public static Prop<ElementCapability, Float> marginY(float v) {
        return Prop.cell(ElementCapability.class, "marginY", v, (config, val) -> {
            config.padTop = Readable.of(val);
            config.padBottom = Readable.of(val);
        });
    }

    public static Prop<ElementCapability, Float> marginY(@Nullable Readable<Float> v) {
        if (v == null) {
            return Prop.cell(ElementCapability.class, "marginY", 0f, (config, val) -> {
            });
        }
        return Prop.cellSource(ElementCapability.class, "marginY", v, (config, source) -> {
            config.padTop = source;
            config.padBottom = source;
        });
    }

    public static Prop<ElementCapability, Integer> align(int align) {
        return alignProp("align", align, 0);
    }

    public static Prop<ElementCapability, Void> top() {
        return alignProp("top", null, Align.top);
    }

    public static Prop<ElementCapability, Void> bottom() {
        return alignProp("bottom", null, Align.bottom);
    }

    public static Prop<ElementCapability, Void> left() {
        return alignProp("left", null, Align.left);
    }

    public static Prop<ElementCapability, Void> right() {
        return alignProp("right", null, Align.right);
    }

    public static Prop<ElementCapability, Void> center() {
        return alignProp("center", null, Align.center);
    }

    private static <V> Prop<ElementCapability, V> alignProp(String label, @Nullable V value, int named) {
        final @Nullable Integer explicit = value instanceof Integer ? (Integer) value : null;
        return new Prop<>(Prop.Kind.CELL, ElementCapability.class, label,
                (view, val) -> view.withCapability(ElementCapability.class,
                        alignable -> alignable.alignCoordinated(explicit, named)),
                value, null, value == null);
    }

    public static Prop<TableCapability, Float> padding(float v) {
        return Prop.table(TableCapability.class, "padding", v, (table, val) -> {
            if (val != null) {
                table.margin(val);
            }
        });
    }

    public static Prop<TableCapability, Float> padding(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "padding", v, (table, val) -> {
            if (val != null) {
                table.margin(val);
            }
        });
    }

    public static Prop<TableCapability, Void> padding(float top, float left, float bottom, float right) {
        return Prop.table(TableCapability.class, "padding", (table) -> table.margin(top, left, bottom, right));
    }

    public static Prop<TableCapability, Float> padding(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        if (top != null) {
            return Prop.tableSource(TableCapability.class, "padding", top, (table, val) -> {
                float tv = val != null ? val : 0f;
                float lv = left != null && left.get() != null ? left.get() : 0f;
                float bv = bottom != null && bottom.get() != null ? bottom.get() : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(tv, lv, bv, rv);
            });
        }
        if (left != null) {
            return Prop.tableSource(TableCapability.class, "padding", left, (table, val) -> {
                float lv = val != null ? val : 0f;
                float bv = bottom != null && bottom.get() != null ? bottom.get() : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(0f, lv, bv, rv);
            });
        }
        if (bottom != null) {
            return Prop.tableSource(TableCapability.class, "padding", bottom, (table, val) -> {
                float bv = val != null ? val : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(0f, 0f, bv, rv);
            });
        }
        if (right != null) {
            return Prop.tableSource(TableCapability.class, "padding", right,
                    (table, val) -> table.margin(0f, 0f, 0f, val != null ? val : 0f));
        }
        return Prop.table(TableCapability.class, "padding", 0f, (table, val) -> {
        });
    }

    public static Prop<TableCapability, Float> paddingTop(float v) {
        return Prop.table(TableCapability.class, "paddingTop", v, (table, val) -> {
            if (val != null) {
                table.marginTop(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingTop(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "paddingTop", v, (table, val) -> {
            if (val != null) {
                table.marginTop(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingBottom(float v) {
        return Prop.table(TableCapability.class, "paddingBottom", v, (table, val) -> {
            if (val != null) {
                table.marginBottom(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingBottom(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "paddingBottom", v, (table, val) -> {
            if (val != null) {
                table.marginBottom(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingLeft(float v) {
        return Prop.table(TableCapability.class, "paddingLeft", v, (table, val) -> {
            if (val != null) {
                table.marginLeft(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingLeft(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "paddingLeft", v, (table, val) -> {
            if (val != null) {
                table.marginLeft(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingRight(float v) {
        return Prop.table(TableCapability.class, "paddingRight", v, (table, val) -> {
            if (val != null) {
                table.marginRight(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingRight(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "paddingRight", v, (table, val) -> {
            if (val != null) {
                table.marginRight(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingX(float v) {
        return Prop.table(TableCapability.class, "paddingX", v, (table, val) -> {
            if (val != null) {
                table.marginLeft(val);
                table.marginRight(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingX(@Nullable Readable<Float> x) {
        if (x == null) {
            return Prop.table(TableCapability.class, "paddingX", 0f, (table, val) -> {
            });
        }
        return Prop.tableSource(TableCapability.class, "paddingX", x, (table, val) -> {
            if (val != null) {
                table.marginLeft(val);
                table.marginRight(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingY(float v) {
        return Prop.table(TableCapability.class, "paddingY", v, (table, val) -> {
            if (val != null) {
                table.marginTop(val);
                table.marginBottom(val);
            }
        });
    }

    public static Prop<TableCapability, Float> paddingY(@Nullable Readable<Float> y) {
        if (y == null) {
            return Prop.table(TableCapability.class, "paddingY", 0f, (table, val) -> {
            });
        }
        return Prop.tableSource(TableCapability.class, "paddingY", y, (table, val) -> {
            if (val != null) {
                table.marginTop(val);
                table.marginBottom(val);
            }
        });
    }

    public static Prop<TableCapability, Float> gap(float v) {
        return Prop.table(TableCapability.class, "gap", v, (table, val) -> {
            if (val != null) {
                GenericGapContainer.setGap(table, val);
            }
        });
    }

    public static Prop<TableCapability, Float> gap(Readable<Float> v) {
        return Prop.tableSource(TableCapability.class, "gap", v, (table, val) -> {
            if (val != null) {
                GenericGapContainer.setGap(table, val);
            }
        });
    }

    public static Prop<TableCapability, Integer> rounded(int radius) {
        return Prop.table(TableCapability.class, "rounded", radius, (table, val) -> {
            if (val != null) {
                RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, val);
                drawable.radius(val);
            }
        });
    }

    public static Prop<TableCapability, Integer> rounded(int radius, @Nullable Color color) {
        return Prop.table(TableCapability.class, "rounded", radius, (table, val) -> {
            if (val != null) {
                RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, val);
                drawable.radius(val);
                if (color != null) {
                    drawable.fillColor(color);
                }
            }
        });
    }

    public static Prop<TableCapability, Color> rounded(int radius, @Nullable Readable<Color> color) {
        if (color == null) {
            return Prop.table(TableCapability.class, "rounded", (Color) null, (table, val) -> {
                RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, radius);
                drawable.radius(radius);
            });
        }
        return Prop.tableSource(TableCapability.class, "rounded", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, radius);
            drawable.radius(radius);
            if (val != null) {
                drawable.fillColor(val);
            }
        });
    }

    public static Prop<TableCapability, Float> border(float stroke, @Nullable Color color) {
        return Prop.table(TableCapability.class, "border", stroke, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.border(val != null ? val : stroke, color != null ? color : Color.white);
        });
    }

    public static Prop<TableCapability, Color> border(float stroke, @Nullable Readable<Color> color) {
        if (color == null) {
            return Prop.table(TableCapability.class, "border", (Color) null, (table, val) -> {
                RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
                drawable.border(stroke, Color.white);
            });
        }
        return Prop.tableSource(TableCapability.class, "border", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.border(stroke, val != null ? val : Color.white);
        });
    }

    public static Prop<TableCapability, Drawable> background(@Nullable Drawable drawable) {
        return Prop.table(TableCapability.class, "background", drawable, (table, val) -> {
            if (table.getBackground() instanceof RoundedDrawable) {
                ((RoundedDrawable) table.getBackground()).baseDrawable(val);
            } else {
                table.setBackground(val);
            }
        });
    }

    public static Prop<TableCapability, Drawable> background(@Nullable Readable<Drawable> drawable) {
        if (drawable == null) {
            return Prop.table(TableCapability.class, "background", (Drawable) null, (table, val) -> {
            });
        }
        return Prop.tableSource(TableCapability.class, "background", drawable, (table, val) -> {
            if (val == null) {
                return;
            }
            if (table.getBackground() instanceof RoundedDrawable) {
                ((RoundedDrawable) table.getBackground()).baseDrawable(val);
            } else {
                table.setBackground(val);
            }
        });
    }

    public static Prop<TableCapability, Color> background(@Nullable Color color) {
        return Prop.table(TableCapability.class, "background", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.fillColor(val != null ? val : Color.clear);
        });
    }

    public static Prop<TableCapability, Color> backgroundColor(@Nullable Color color) {
        return background(color);
    }

    public static Prop<TableCapability, Readable<Color>> backgroundColor(@Nullable Readable<Color> color) {
        if (color == null) {
            return Prop.table(TableCapability.class, "backgroundColor", (Readable<Color>) null, (table, val) -> {
            });
        }
        return Prop.table(TableCapability.class, "backgroundColor", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.fillColor(val);
        });
    }

    public static Prop<ElementCapability, Float> width(float v) {
        return Prop.cell(ElementCapability.class, "width", Math.max(0f, v),
                (config, val) -> config.prefWidth = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> width(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "width", v, (config, source) -> config.prefWidth = source);
    }

    public static Prop<ElementCapability, Float> height(float v) {
        return Prop.cell(ElementCapability.class, "height", Math.max(0f, v),
                (config, val) -> config.prefHeight = Readable.of(val));
    }

    public static Prop<ElementCapability, Float> height(Readable<Float> v) {
        return Prop.cellSource(ElementCapability.class, "height", v, (config, source) -> config.prefHeight = source);
    }

    public static Prop<ElementCapability, Float> size(float v) {
        return size(v, v);
    }

    public static Prop<ElementCapability, Float> size(float width, float height) {
        return Prop.cell(ElementCapability.class, "size", width, (config, val) -> {
            config.prefWidth = Readable.of(Math.max(0f, width));
            config.prefHeight = Readable.of(Math.max(0f, height));
        });
    }

    public static Prop<ElementCapability, Float> size(@Nullable Readable<Float> size) {
        return size(size, size);
    }

    public static Prop<ElementCapability, Float> size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        if (width != null && height != null) {
            return Prop.cellSource(ElementCapability.class, "size", width, (config, source) -> {
                config.prefWidth = width;
                config.prefHeight = height;
            });
        }
        if (width != null) {
            return Prop.cellSource(ElementCapability.class, "size", width, (config, source) -> config.prefWidth = width);
        }
        if (height != null) {
            return Prop.cellSource(ElementCapability.class, "size", height,
                    (config, source) -> config.prefHeight = height);
        }
        return Prop.cell(ElementCapability.class, "size", 0f, (config, val) -> {
        });
    }

    public static Prop<ElementCapability, Float> x(float v) {
        return Prop.element(ElementCapability.class, "x", v, (element, val) -> {
            if (val != null) {
                element.x = val;
            }
        });
    }

    public static Prop<ElementCapability, Float> y(float v) {
        return Prop.element(ElementCapability.class, "y", v, (element, val) -> {
            if (val != null) {
                element.y = val;
            }
        });
    }

    public static Prop<ElementCapability, Void> position(float x, float y) {
        return Prop.element(ElementCapability.class, "position", (element) -> {
            element.x = x;
            element.y = y;
        });
    }

    public static Prop<ElementCapability, Float> position(@Nullable Readable<Float> x, @Nullable Readable<Float> y) {
        if (x != null) {
            return Prop.elementSource(ElementCapability.class, "position", x, (element, val) -> {
                bindHudSignals(element, x, y);
                if (val == null) {
                    return;
                }
                element.x = val;
                if (y != null) {
                    Float yv = y.get();
                    if (yv != null) {
                        element.y = yv;
                    }
                }
            });
        }
        if (y != null) {
            return Prop.elementSource(ElementCapability.class, "position", y, (element, val) -> {
                bindHudSignals(element, x, y);
                if (val == null) {
                    return;
                }
                element.y = val;
            });
        }
        return Prop.element(ElementCapability.class, "position", 0f, (element, val) -> {
        });
    }

    private static void bindHudSignals(Element element, @Nullable Readable<Float> x,
            @Nullable Readable<Float> y) {
        Component host = hostOf(element);
        if (!(host instanceof Hud)) {
            return;
        }
        if (x instanceof Signal) {
            ((Hud) host).bindXSignal((Signal<Float>) x);
        }
        if (y instanceof Signal) {
            ((Hud) host).bindYSignal((Signal<Float>) y);
        }
    }

    public static Prop<ElementCapability, Boolean> visible(boolean visible) {
        return Prop.element(ElementCapability.class, "visible", visible, (element, val) -> {
            element.visible = Boolean.TRUE.equals(val);
            respaceParent(element);
        });
    }

    public static Prop<ElementCapability, Boolean> visible(Readable<Boolean> visible) {
        return Prop.elementSource(ElementCapability.class, "visible", visible, (element, val) -> {
            element.visible = Boolean.TRUE.equals(val);
            respaceParent(element);
        });
    }

    public static Prop<ElementCapability, Touchable> touchable(Touchable touchable) {
        return Prop.element(ElementCapability.class, "touchable", touchable, (element, val) -> {
            if (val != null) {
                element.touchable = val;
            }
        });
    }

    public static Prop<ElementCapability, Touchable> touchable(Readable<Touchable> touchable) {
        return Prop.elementSource(ElementCapability.class, "touchable", touchable, (element, val) -> {
            if (val != null) {
                element.touchable = val;
            }
        });
    }

    public static Prop<ElementCapability, Float> opacity(float opacity) {
        return hostProp("opacity", opacity, Values::fade);
    }

    public static Prop<ElementCapability, Float> opacity(Readable<Float> source) {
        return hostProp("opacity", null, source, Values::fade);
    }

    public static Prop<ElementCapability, Float> alpha(float alpha) {
        return opacity(alpha);
    }

    public static Prop<ElementCapability, Float> alpha(Readable<Float> source) {
        return opacity(source);
    }

    public static Prop<ElementCapability, Float> x(Readable<Float> source) {
        return hostProp("x", null, source, (element, v, host) -> {
            if (host instanceof Hud && source instanceof Signal) {
                ((Hud) host).bindXSignal((Signal<Float>) source);
            }
            if (v != null) {
                element.x = v;
            }
        });
    }

    public static Prop<ElementCapability, Float> y(Readable<Float> source) {
        return hostProp("y", null, source, (element, v, host) -> {
            if (host instanceof Hud && source instanceof Signal) {
                ((Hud) host).bindYSignal((Signal<Float>) source);
            }
            if (v != null) {
                element.y = v;
            }
        });
    }

    public static Prop<ElementCapability, Float> scale(float scale) {
        return hostProp("scale", scale, Values::applyScale);
    }

    public static Prop<ElementCapability, Float> scale(Readable<Float> source) {
        return hostProp("scale", null, source, Values::applyScale);
    }

    private static void applyScale(Element element, @Nullable Float value, Component host) {
        if (value == null) {
            return;
        }
        if (host instanceof Hud) {
            Hud hud = (Hud) host;
            hud.container().setScale(value);
            hud.pack();
        } else {
            element.setScale(value);
        }
    }

    private static void fade(Element element, @Nullable Float value, Component host) {
        if (value == null) {
            return;
        }
        float a = Math.max(0f, Math.min(1f, value));
        element.color.a = a;
        if (host instanceof Hud) {
            ((Hud) host).container().color.a = a;
        }
    }

    private static void respaceParent(Element element) {
        if (element.parent instanceof Table) {
            GapContainer.respace((Table) element.parent);
        }
    }

    public static Prop<ElementCapability, String> name(String name) {
        return Prop.element(ElementCapability.class, "name", name, (element, val) -> element.name = val);
    }

    public static Prop<ElementCapability, Runnable> onClick(Runnable action) {
        return Prop.element(ElementCapability.class, "onClick", action, (element, val) -> {
            ElementClickBinding binding = ElementClickBinding.find(element);
            if (binding == null) {
                binding = new ElementClickBinding();
                element.addListener(binding);
            }
            binding.handler = val;
            element.touchable = Touchable.enabled;
        });
    }

    public static Prop<ElementCapability, Boolean> stopClickPropagation(boolean stop) {
        return Prop.element(ElementCapability.class, "stopClickPropagation", stop, (element, val) -> {
            ElementClickBinding binding = ElementClickBinding.find(element);
            if (binding != null) {
                binding.stop = Boolean.TRUE.equals(val);
            }
        });
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable String tip) {
        return Prop.raw(ElementCapability.class, "tooltip", Prop.Kind.ELEMENT, target -> {
            Element element = (Element) target;
            removeTooltips(element);
            if (tip == null || tip.isEmpty()) {
                return;
            }
            try {
                element.addListener(new Tooltip(t -> t.add(tip)));
            } catch (Throwable ignored) {
            }
        });
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable Readable<String> tip) {
        return Prop.raw(ElementCapability.class, "tooltip", Prop.Kind.ELEMENT, target -> {
            Element element = (Element) target;
            removeTooltips(element);
            if (tip == null) {
                return;
            }
            try {
                element.addListener(new Tooltip(t -> {
                    Label label = new Label("");
                    t.add(label);
                    Effect.of(() -> {
                        String text = tip.get();
                        label.setText(text != null ? text : "");
                    });
                }));
            } catch (Throwable ignored) {
            }
        });
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable Cons<Table> builder) {
        return Prop.raw(ElementCapability.class, "tooltip", Prop.Kind.ELEMENT, target -> {
            Element element = (Element) target;
            removeTooltips(element);
            if (builder == null) {
                return;
            }
            try {
                element.addListener(new Tooltip(builder));
            } catch (Throwable ignored) {
            }
        });
    }

    public static Prop<ElementCapability, Void> draggable() {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT,
                target -> Hud.makeDraggable((Element) target, null, null, null));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT,
                target -> Hud.makeDraggable((Element) target, null, x, y));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT,
                target -> Hud.makeDraggable((Element) target, hud, null, null));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud, @Nullable Signal<Float> x,
            @Nullable Signal<Float> y) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT,
                target -> Hud.makeDraggable((Element) target, hud, x, y));
    }

    public static Prop<ElementCapability, Cons<Element>> update(Cons<Element> fn) {
        return Prop.element(ElementCapability.class, "update", fn, (element, val) -> {
            if (val != null) {
                element.update(() -> val.get(element));
            }
        });
    }

    public static Prop<ElementCapability, Integer> origin(int align) {
        return Prop.element(ElementCapability.class, "origin", align, (element, val) -> {
            if (val != null) {
                element.setOrigin(val);
            }
        });
    }

    public static Prop<ElementCapability, Float> rotation(float degree) {
        return Prop.element(ElementCapability.class, "rotation", degree, (element, val) -> {
            if (val != null) {
                element.rotation = val;
            }
        });
    }

    public static Prop<ElementCapability, Color> color(@Nullable Color value) {
        return hostProp("color", value, Values::paint);
    }

    public static Prop<ElementCapability, Color> color(@Nullable Readable<Color> source) {
        return hostProp("color", null, source, Values::paint);
    }

    public static Prop<ButtonCapability, Cons<SolimButtonStyleBuilder>> buttonStyle(
            @Nullable Cons<SolimButtonStyleBuilder> config) {
        return Prop.raw(ButtonCapability.class, "buttonStyle", Prop.Kind.ELEMENT, target -> {
            if (config == null) {
                return;
            }
            Component owner = hostOf(target);
            if (owner instanceof Button) {
                ((Button) owner).setStyle(config);
            } else if (owner != null) {
                throw new IllegalArgumentException("Host " + owner.getClass().getName() + " cannot accept buttonStyle");
            }
        });
    }

    public static Prop<LabelCapability, LabelStyle> style(@Nullable LabelStyle value) {
        return hostProp(LabelCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Text) {
                ((Text) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<ButtonCapability, SolimButtonStyle> style(@Nullable SolimButtonStyle value) {
        return hostProp(ButtonCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Button) {
                ((Button) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<ButtonCapability, Void> style(@Nullable Cons<SolimButtonStyleBuilder> config) {
        return Prop.element(ButtonCapability.class, "style", element -> {
            if (config == null) {
                return;
            }
            Component host = SolimToken.getComponent(element);
            if (host instanceof Button) {
                ((Button) host).setStyle(config);
            } else {
                throw new IllegalArgumentException("Host " + (host != null ? host.getClass().getName() : "null")
                        + " cannot accept SolimButtonStyleBuilder config");
            }
        });
    }

    public static Prop<ButtonCapability, ButtonStyle> style(@Nullable ButtonStyle value) {
        return hostProp(ButtonCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Button) {
                ((Button) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<FieldCapability, TextFieldStyle> style(@Nullable TextFieldStyle value) {
        return hostProp(FieldCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<FieldCapability, InputStyle> style(@Nullable InputStyle value) {
        return hostProp(FieldCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<ScrollCapability, ScrollPaneStyle> style(@Nullable ScrollPaneStyle value) {
        return hostProp(ScrollCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Scroll) {
                ((Scroll) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static Prop<SliderCapability, SliderStyle> style(@Nullable SliderStyle value) {
        return hostProp(SliderCapability.class, "style", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimSlider) {
                ((SolimSlider) host).setStyle(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept " + v.getClass().getName());
            }
        });
    }

    public static <S> Prop<ElementCapability, S> style(@Nullable Readable<S> source) {
        return hostProp(ElementCapability.class, "style", null, source, Values::restyle);
    }

    public static Prop<TextCapability, String> text(@Nullable String value) {
        return hostProp(TextCapability.class, "text", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Button) {
                ((Button) host).text(v);
            } else if (host instanceof Text) {
                ((Text) host).setText(v);
            } else if (host instanceof SolimTextField) {
                ((SolimTextField) host).setPlaceholder(v);
            } else if (host instanceof Badge) {
                ((Badge) host).setText(v);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept text prop");
            }
        });
    }

    public static Prop<TextCapability, String> text(@Nullable Readable<String> source) {
        return hostOwnedProp(TextCapability.class, "text", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).text(source);
            } else if (host instanceof Text) {
                Text text = (Text) host;
                Effect.of(() -> {
                    String value = source.get();
                    text.setText(value != null ? value : "");
                });
            } else if (host instanceof SolimTextField) {
                Effect.of(() -> ((SolimTextField) host).setPlaceholder(source.get()));
            } else if (host instanceof Badge) {
                Badge badge = (Badge) host;
                Effect.of(() -> {
                    String value = source.get();
                    badge.setText(value != null ? value : "");
                });
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept text prop");
            }
        });
    }

    public static Prop<IconCapability, Drawable> icon(@Nullable Drawable value) {
        return hostProp(IconCapability.class, "icon", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Button) {
                ((Button) host).icon(v);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept icon prop");
            }
        });
    }

    public static Prop<IconCapability, Drawable> icon(@Nullable Readable<Drawable> source) {
        return hostOwnedProp(IconCapability.class, "icon", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).icon(source);
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept icon prop");
            }
        });
    }

    public static Prop<CheckableCapability, Boolean> checked(@Nullable Signal<Boolean> signal) {
        return hostOwnedProp(CheckableCapability.class, "checked", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Button) {
                Button btn = (Button) host;
                warnCheckedWithoutVisual(btn);
                Effect.of(() -> btn.button().setChecked(Boolean.TRUE.equals(signal.get())));
                btn.button().addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        signal.set(btn.button().isChecked());
                    }
                });
            } else if (host instanceof Checkbox) {
                Checkbox cb = (Checkbox) host;
                new TwoWayBinding<>(
                        signal,
                        () -> cb.checkBox().isChecked(),
                        val -> cb.checkBox().setChecked(Boolean.TRUE.equals(val)),
                        onChange -> {
                            cb.checkBox().changed(onChange::run);
                            return DisposableAction.empty();
                        });
            } else if (host instanceof Switch) {
                Switch sw = (Switch) host;
                new TwoWayBinding<>(
                        signal,
                        sw::isChecked,
                        val -> {
                            boolean state = Boolean.TRUE.equals(val);
                            sw.button().setText(state ? "ON" : "OFF");
                            sw.button().setChecked(state);
                        },
                        onChange -> {
                            sw.button().changed(onChange::run);
                            return DisposableAction.empty();
                        });
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept checked prop");
            }
        });
    }

    public static Prop<CheckableCapability, Boolean> checked(@Nullable Readable<Boolean> source) {
        return hostOwnedProp(CheckableCapability.class, "checked", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Button) {
                Button btn = (Button) host;
                warnCheckedWithoutVisual(btn);
                if (source instanceof ReactiveSource) {
                    btn.button().addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            btn.button().setChecked(Boolean.TRUE.equals(source.peek()));
                        }
                    });
                }
                Effect.of(() -> btn.button().setChecked(Boolean.TRUE.equals(source.get())));
            } else if (host instanceof Checkbox) {
                Checkbox cb = (Checkbox) host;
                Effect.of(() -> cb.checkBox().setChecked(Boolean.TRUE.equals(source.get())));
            } else if (host instanceof Switch) {
                Switch sw = (Switch) host;
                Effect.of(() -> sw.setChecked(Boolean.TRUE.equals(source.get())));
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept checked prop");
            }
        });
    }

    private static boolean checkedWarningLogged;

    private static void warnCheckedWithoutVisual(Button button) {
        ButtonStyle style = button.button().getStyle();
        if (style == null || style.checked != null || checkedWarningLogged) {
            return;
        }
        checkedWarningLogged = true;
        Log.warn(
                "[Solim] .checked(...) bound to a style with no checked visual — the selected state will not render. Use a style that declares a checked state; action-chip styles deliberately have none.");
    }

    public static Prop<CheckableCapability, Boolean> checked(boolean value) {
        return checked(Readable.of(value));
    }

    public static Prop<SliderCapability, Float> value(@Nullable Signal<Float> signal) {
        return hostOwnedProp(SliderCapability.class, "value", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimSlider) {
                SolimSlider s = (SolimSlider) host;
                Slider slider = s.slider();
                new TwoWayBinding<>(
                        signal,
                        slider::getValue,
                        slider::setValue,
                        onChange -> {
                            slider.changed(onChange::run);
                            return DisposableAction.empty();
                        },
                        (a, b) -> a != null && b != null && Math.abs(a - b) <= 0.0001f);
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept value prop");
            }
        });
    }

    public static Prop<SliderCapability, Float> value(float val) {
        return value(Readable.of(val));
    }

    public static Prop<SliderCapability, Float> value(@Nullable Readable<Float> source) {
        return hostOwnedProp(SliderCapability.class, "value", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimSlider) {
                SolimSlider s = (SolimSlider) host;
                Effect.of(() -> {
                    Float v = source.get();
                    if (v != null) {
                        s.slider().setValue(v);
                    }
                });
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept value prop");
            }
        });
    }

    public static <T> Prop<SelectCapability, T> selected(@Nullable Signal<T> signal) {
        return hostOwnedProp(SelectCapability.class, "selected", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimSelect<?>) {
                SolimSelect<?> sel = (SolimSelect<?>) host;
                Effect.of(() -> {
                    T v = signal.get();
                    if (v != null) {
                        sel.selectBox().setText(String.valueOf(v));
                    }
                });
            } else if (host != null) {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept selected prop");
            }
        });
    }

    public static <T> Prop<SelectCapability, T> selected(@Nullable Readable<T> source) {
        return hostOwnedProp(SelectCapability.class, "selected", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimSelect<?>) {
                SolimSelect<?> sel = (SolimSelect<?>) host;
                Effect.of(() -> {
                    T v = source.get();
                    if (v != null) {
                        sel.selectBox().setText(String.valueOf(v));
                    }
                });
            } else if (host != null) {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept selected prop");
            }
        });
    }

    public static Prop<TableCapability, Boolean> fillParent(boolean fill) {
        return Prop.table(TableCapability.class, "fillParent", fill, (table, v) -> {
            if (v != null) {
                table.setFillParent(v);
            }
        });
    }

    public static Prop<TableCapability, Boolean> fillParent() {
        return fillParent(true);
    }

    public static Prop<TableCapability, Integer> columns(int count) {
        return hostProp(TableCapability.class, "columns", count, (element, v, host) -> {
            if (v != null && host instanceof Grid) {
                ((Grid) host).setColumns(v);
            }
        });
    }

    public static Prop<TableCapability, Integer> columns(@Nullable Readable<Integer> count) {
        return hostProp(TableCapability.class, "columns", null, count, (element, v, host) -> {
            if (v != null && host instanceof Grid) {
                ((Grid) host).setColumns(v);
            }
        });
    }

    public static Prop<TableCapability, Integer> activeTab(@Nullable Signal<Integer> signal) {
        return hostOwnedProp(TableCapability.class, "activeTab", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Tabs) {
                Tabs tabs = (Tabs) host;
                Effect.of(() -> {
                    Integer idx = signal.get();
                    if (idx != null) {
                        List<Button> buttons = tabs.buttons();
                        for (int i = 0; i < buttons.size(); i++) {
                            buttons.get(i).button().setChecked(i == idx);
                        }
                    }
                });
            }
        });
    }

    public static Prop<TableCapability, Integer> activeTab(int tab) {
        return activeTab(Readable.of(tab));
    }

    public static Prop<TableCapability, Integer> activeTab(@Nullable Readable<Integer> source) {
        return hostOwnedProp(TableCapability.class, "activeTab", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Tabs) {
                Tabs tabs = (Tabs) host;
                Effect.of(() -> {
                    Integer idx = source.get();
                    if (idx != null) {
                        List<Button> buttons = tabs.buttons();
                        for (int i = 0; i < buttons.size(); i++) {
                            buttons.get(i).button().setChecked(i == idx);
                        }
                    }
                });
            }
        });
    }

    public static Prop<TableCapability, Float> headerGap(float gap) {
        return hostProp(TableCapability.class, "headerGap", gap, (element, v, host) -> {
            if (v != null && host instanceof Tabs) {
                ((Tabs) host).setHeaderBarGap(v);
            }
        });
    }

    public static Prop<TableCapability, ButtonStyle> tabStyle(@Nullable ButtonStyle style) {
        return hostProp(TableCapability.class, "tabStyle", style, (element, v, host) -> {
            if (host instanceof Tabs) {
                ((Tabs) host).setTabButtonStyle(v);
            }
        });
    }

    public static Prop<TableCapability, Float> progress(@Nullable Readable<Float> source) {
        return hostOwnedProp(TableCapability.class, "progress", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimVideo) {
                Effect.of(() -> {
                    Float p = source.get();
                    if (p != null) {
                        ((SolimVideo) host).progressSignal().set(p);
                    }
                });
            }
        });
    }

    public static Prop<TableCapability, Float> progress(float val) {
        return progress(Readable.of(val));
    }

    public static Prop<TableCapability, Boolean> hideOnZero(boolean hide) {
        return hostProp(TableCapability.class, "hideOnZero", hide, (element, v, host) -> {
            if (v != null && host instanceof Badge) {
                ((Badge) host).setHideOnZero(v);
            }
        });
    }

    public static Prop<TableCapability, Boolean> hideOnZero() {
        return hideOnZero(true);
    }

    public static Prop<FieldCapability, String> placeholder(@Nullable String text) {
        return hostProp(FieldCapability.class, "placeholder", text, (element, v, host) -> {
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).setPlaceholder(v != null ? v : "");
            }
        });
    }

    public static Prop<FieldCapability, String> placeholder(@Nullable Readable<String> source) {
        return hostOwnedProp(FieldCapability.class, "placeholder", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimTextField) {
                Effect.of(() -> ((SolimTextField) host).setPlaceholder(source.get()));
            }
        });
    }

    public static Prop<FieldCapability, Integer> maxLength(int length) {
        return hostProp(FieldCapability.class, "maxLength", length, (element, v, host) -> {
            if (v != null && host instanceof SolimTextField) {
                ((SolimTextField) host).field().setMaxLength(Math.max(0, v));
            }
        });
    }

    public static Prop<ScrollCapability, Void> scrollingDisabled(boolean disableX, boolean disableY) {
        return Prop.element(ScrollCapability.class, "scrollingDisabled", element -> {
            Component host = SolimToken.getComponent(element);
            if (host instanceof Scroll) {
                ((Scroll) host).setScrollingDisabled(disableX, disableY);
            }
        });
    }

    public static Prop<ScrollCapability, Boolean> scrollX(boolean scrollX) {
        return hostProp(ScrollCapability.class, "scrollX", scrollX, (element, v, host) -> {
            if (v != null && host instanceof Scroll) {
                ((Scroll) host).setScrollX(v);
            }
        });
    }

    public static Prop<ScrollCapability, Boolean> scrollY(boolean scrollY) {
        return hostProp(ScrollCapability.class, "scrollY", scrollY, (element, v, host) -> {
            if (v != null && host instanceof Scroll) {
                ((Scroll) host).setScrollY(v);
            }
        });
    }

    public static Prop<TableCapability, Popup.Edge> pinnedEdge(@Nullable Popup.Edge edge) {
        return hostProp(TableCapability.class, "pinnedEdge", edge, (element, v, host) -> {
            if (host instanceof Popup) {
                ((Popup<?>) host).setPinnedEdge(v);
            }
        });
    }

    public static Prop<TableCapability, Color> textColor(@Nullable Color color) {
        return hostProp(TableCapability.class, "textColor", color, (element, v, host) -> {
            if (v != null && host instanceof Badge) {
                ((Badge) host).setTextColor(v);
            }
        });
    }

    public static Prop<TableCapability, Color> textColor(@Nullable Readable<Color> source) {
        return hostOwnedProp(TableCapability.class, "textColor", target -> {
            if (source == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof Badge) {
                Effect.of(() -> {
                    Color c = source.get();
                    if (c != null) {
                        ((Badge) host).setTextColor(c);
                    }
                });
            }
        });
    }

    public static Prop<TableCapability, Boolean> collapsed(@Nullable Signal<Boolean> signal) {
        return hostOwnedProp(TableCapability.class, "collapsed", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimCollapser) {
                SolimCollapser c = (SolimCollapser) host;
                c.bindCollapsedSignal(signal);
            }
        });
    }

    public static Prop<TableCapability, Boolean> collapsed(@Nullable Readable<Boolean> source) {
        return hostProp(TableCapability.class, "collapsed", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimCollapser) {
                ((SolimCollapser) host).setCollapsed(v);
            }
        });
    }

    public static Prop<TableCapability, Boolean> collapsed(boolean val) {
        return collapsed(Readable.of(val));
    }

    public static Prop<TableCapability, Boolean> expanded(@Nullable Signal<Boolean> signal) {
        return hostOwnedProp(TableCapability.class, "expanded", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimCollapser) {
                SolimCollapser c = (SolimCollapser) host;
                new TwoWayBinding<>(
                        signal,
                        () -> !c.isCollapsed(),
                        val -> c.setExpanded(Boolean.TRUE.equals(val)),
                        onChange -> DisposableAction.empty());
            }
        });
    }

    public static Prop<TableCapability, Boolean> expanded(@Nullable Readable<Boolean> source) {
        return hostProp(TableCapability.class, "expanded", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimCollapser) {
                ((SolimCollapser) host).setExpanded(v);
            }
        });
    }

    public static Prop<TableCapability, Boolean> expanded(boolean val) {
        return expanded(Readable.of(val));
    }

    public static Prop<TableCapability, Float> duration(float seconds) {
        return hostProp(TableCapability.class, "duration", seconds, (element, v, host) -> {
            if (v != null && host instanceof SolimCollapser) {
                ((SolimCollapser) host).setDuration(v);
            }
        });
    }

    public static Prop<TableCapability, Boolean> animated(boolean animated) {
        return hostProp(TableCapability.class, "animated", animated, (element, v, host) -> {
            if (v != null && host instanceof SolimCollapser) {
                ((SolimCollapser) host).setAnimated(v);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> loop(boolean loop) {
        return hostProp(VideoCapability.class, "loop", loop, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setLoop(v);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> loop(@Nullable Readable<Boolean> source) {
        return hostProp(VideoCapability.class, "loop", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setLoop(v);
            }
        });
    }

    public static Prop<VideoCapability, Float> volume(float volume) {
        return hostProp(VideoCapability.class, "volume", volume, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setVolume(v);
            }
        });
    }

    public static Prop<VideoCapability, Float> volume(@Nullable Readable<Float> source) {
        return hostProp(VideoCapability.class, "volume", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setVolume(v);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> muted(@Nullable Signal<Boolean> signal) {
        return hostOwnedProp(VideoCapability.class, "muted", target -> {
            if (signal == null) {
                return;
            }
            Component host = hostOf(target);
            if (host instanceof SolimVideo) {
                SolimVideo vid = (SolimVideo) host;
                vid.bindMutedSignal(signal);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> muted(@Nullable Readable<Boolean> source) {
        return hostProp(VideoCapability.class, "muted", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setMuted(v);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> muted(boolean val) {
        return muted(Readable.of(val));
    }

    public static Prop<VideoCapability, Boolean> controls(boolean controls) {
        return hostProp(VideoCapability.class, "controls", controls, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setControls(v);
            }
        });
    }

    public static Prop<VideoCapability, Boolean> controls(@Nullable Readable<Boolean> source) {
        return hostProp(VideoCapability.class, "controls", null, source, (element, v, host) -> {
            if (v != null && host instanceof SolimVideo) {
                ((SolimVideo) host).setControls(v);
            }
        });
    }

    public static Prop<VideoCapability, Drawable> playIcon(@Nullable Drawable icon) {
        return hostProp(VideoCapability.class, "playIcon", icon, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setPlayIcon(v);
            }
        });
    }

    public static Prop<VideoCapability, Drawable> pauseIcon(@Nullable Drawable icon) {
        return hostProp(VideoCapability.class, "pauseIcon", icon, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setPauseIcon(v);
            }
        });
    }

    public static Prop<VideoCapability, Drawable> replayIcon(@Nullable Drawable icon) {
        return hostProp(VideoCapability.class, "replayIcon", icon, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setReplayIcon(v);
            }
        });
    }

    public static Prop<VideoCapability, Drawable> volumeIcon(@Nullable Drawable icon) {
        return hostProp(VideoCapability.class, "volumeIcon", icon, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setVolumeIcon(v);
            }
        });
    }

    public static Prop<VideoCapability, Drawable> muteIcon(@Nullable Drawable icon) {
        return hostProp(VideoCapability.class, "muteIcon", icon, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setMuteIcon(v);
            }
        });
    }

    public static Prop<ImageCapability, Scaling> scaling(@Nullable Scaling scaling) {
        return hostProp(ImageCapability.class, "scaling", scaling, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimImage) {
                ((SolimImage) host).setScaling(v);
            } else if (host instanceof SolimVideo) {
                ((SolimVideo) host).setScaling(v);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> enabled(boolean value) {
        return enabled(Readable.of(value));
    }

    public static Prop<ElementCapability, Boolean> enabled(@Nullable Readable<Boolean> source) {
        return hostProp(ElementCapability.class, "enabled", null, source, (element, v, host) -> {
            if (v != null && host instanceof Button) {
                boolean disabled = !Boolean.TRUE.equals(v);
                ((Button) host).setDisabled(disabled);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> disabled(boolean value) {
        return hostProp("disabled", value, (element, v, host) -> {
            boolean disabled = Boolean.TRUE.equals(v);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).setDisabled(disabled);
            } else if (host instanceof Button) {
                ((Button) host).setDisabled(disabled);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> disabled(@Nullable Readable<Boolean> source) {
        return hostProp("disabled", null, source, (element, v, host) -> {
            boolean disabled = Boolean.TRUE.equals(v);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).setDisabled(disabled);
            } else if (host instanceof Button) {
                ((Button) host).setDisabled(disabled);
            }
        });
    }

    public static Prop<WrappableCapability, Boolean> wrap(boolean value) {
        return hostProp(WrappableCapability.class, "wrap", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            boolean wrap = v;
            if (host instanceof Text) {
                ((Text) host).setWrap(wrap);
            } else if (host instanceof Checkbox) {
                ((Checkbox) host).setWrap(wrap);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept wrap prop");
            }
        });
    }

    public static Prop<WrappableCapability, Boolean> wrap() {
        return wrap(true);
    }

    public static Prop<WrappableCapability, Boolean> ellipsis(boolean value) {
        return hostProp(WrappableCapability.class, "ellipsis", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Text) {
                ((Text) host).setEllipsis(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept ellipsis prop");
            }
        });
    }

    public static Prop<WrappableCapability, Boolean> ellipsis() {
        return ellipsis(true);
    }

    public static Prop<WrappableCapability, Float> fontScale(float value) {
        return hostProp(WrappableCapability.class, "fontScale", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Text) {
                ((Text) host).setFontScale(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept fontScale prop");
            }
        });
    }

    public static Prop<WrappableCapability, Float> fontScale(@Nullable Readable<Float> source) {
        return hostProp(WrappableCapability.class, "fontScale", null, source, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof Text) {
                ((Text) host).setFontScale(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept fontScale prop");
            }
        });
    }

    public static Prop<ImageCapability, String> src(@Nullable String value) {
        return hostProp(ImageCapability.class, "src", value, (element, v, host) -> {
            if (host instanceof SolimImage) {
                ((SolimImage) host).src(v);
            } else if (host instanceof SolimVideo) {
                ((SolimVideo) host).src(v);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept src prop");
            }
        });
    }

    public static Prop<ImageCapability, ImageSource> src(@Nullable ImageSource value) {
        return hostProp(ImageCapability.class, "src", value, (element, v, host) -> {
            if (host instanceof SolimImage) {
                ((SolimImage) host).src(v);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept src prop");
            }
        });
    }

    public static Prop<VideoCapability, VideoSource> src(@Nullable VideoSource value) {
        return hostProp(VideoCapability.class, "src", value, (element, v, host) -> {
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).src(v);
            } else {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept src prop");
            }
        });
    }

    public static <T> Prop<ImageCapability, T> src(@Nullable Readable<T> source) {
        return Prop.elementSource(ImageCapability.class, "src", source, (element, v) -> {
            Component host = SolimToken.getComponent(element);
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).loadSourceIfChanged(toVideoSource(v));
            } else if (host instanceof SolimImage) {
                ((SolimImage) host).applySourceIfChanged(toImageSource(v));
            } else if (host != null) {
                throw new IllegalArgumentException("Host " + host.getClass().getName() + " cannot accept src prop");
            }
        });
    }

    private static VideoSource toVideoSource(@Nullable Object value) {
        if (value instanceof VideoSource) {
            return (VideoSource) value;
        }
        if (value instanceof Fi) {
            return VideoSource.of((Fi) value);
        }
        return VideoSource.of(value != null ? value.toString() : null);
    }

    private static ImageSource toImageSource(@Nullable Object value) {
        if (value instanceof ImageSource) {
            return (ImageSource) value;
        }
        if (value instanceof Drawable) {
            return ImageSource.of((Drawable) value);
        }
        if (value instanceof TextureRegion) {
            return ImageSource.of((TextureRegion) value);
        }
        if (value instanceof Fi) {
            return ImageSource.of((Fi) value);
        }
        return ImageSource.of(value != null ? value.toString() : null);
    }

    public static Prop<ImageCapability, Drawable> placeholder(@Nullable Drawable value) {
        return hostProp(ImageCapability.class, "placeholder", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimImage) {
                ((SolimImage) host).placeholder(v);
            } else if (host instanceof SolimVideo) {
                ((SolimVideo) host).setPlaceholder(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept placeholder prop");
            }
        });
    }

    public static Prop<ImageCapability, Drawable> fallback(@Nullable Drawable value) {
        return hostProp(ImageCapability.class, "fallback", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimImage) {
                ((SolimImage) host).fallback(v);
            } else if (host instanceof SolimVideo) {
                ((SolimVideo) host).setFallback(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept fallback prop");
            }
        });
    }

    public static Prop<VideoCapability, Boolean> autoplay(boolean value) {
        return hostProp(VideoCapability.class, "autoplay", value, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimVideo) {
                ((SolimVideo) host).setAutoplay(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept autoplay prop");
            }
        });
    }

    public static Prop<ImageCapability, Integer> imageRounded(int radius) {
        return hostProp(ImageCapability.class, "rounded", radius, (element, v, host) -> {
            if (v == null) {
                return;
            }
            if (host instanceof SolimImage) {
                ((SolimImage) host).setRounded(v);
            } else if (host instanceof SolimVideo) {
                ((SolimVideo) host).setRounded(v);
            } else {
                throw new IllegalArgumentException(
                        "Host " + host.getClass().getName() + " cannot accept imageRounded prop");
            }
        });
    }

    public static <Req extends HostCapability> Prop<Req, Void> raw(Class<Req> requires, String label, Prop.Kind kind,
            Cons<Object> mutation) {
        return Prop.raw(requires, label, kind, mutation);
    }

    private static void paint(Element element, @Nullable Color value, Component host) {
        if (value == null) {
            return;
        }
        if (host instanceof Badge) {
            ((Badge) host).setBadgeColor(value);
            return;
        }
        element.setColor(value);
    }

    private static <S> void restyle(Element element, @Nullable S value, Component host) {
        if (value == null) {
            return;
        }
        if (value instanceof SolimButtonStyle && host instanceof Button) {
            ((Button) host).setStyle((SolimButtonStyle) value);
        } else if (value instanceof ButtonStyle && host instanceof Button) {
            ((Button) host).setStyle((ButtonStyle) value);
        } else if (value instanceof InputStyle && host instanceof SolimTextField) {
            ((SolimTextField) host).setStyle((InputStyle) value);
        } else if (value instanceof TextFieldStyle && host instanceof SolimTextField) {
            ((SolimTextField) host).setStyle((TextFieldStyle) value);
        } else if (value instanceof ScrollPaneStyle && host instanceof Scroll) {
            ((Scroll) host).setStyle((ScrollPaneStyle) value);
        } else if (value instanceof SliderStyle && host instanceof SolimSlider) {
            ((SolimSlider) host).setStyle((SliderStyle) value);
        } else if (value instanceof LabelStyle && host instanceof Text) {
            ((Text) host).setStyle((LabelStyle) value);
        } else {
            throw new IllegalArgumentException("Host " + host.getClass().getName()
                    + " cannot accept style value of type " + value.getClass().getName());
        }
    }

    private static <Req extends HostCapability, V> Prop<Req, V> hostOwnedProp(Class<Req> requires, String label,
            Cons<Object> action) {
        return Prop.raw(requires, label, Prop.Kind.ELEMENT, action);
    }

    private static @Nullable Component hostOf(@Nullable Object target) {
        return target instanceof Element ? SolimToken.getComponent((Element) target) : null;
    }

    private static <Req extends HostCapability, V> Prop<Req, V> hostProp(Class<Req> requires, String label,
            @Nullable V value, HostAction<V> action) {
        return hostProp(requires, label, value, null, action);
    }

    private static <Req extends HostCapability, V> Prop<Req, V> hostProp(Class<Req> requires, String label,
            @Nullable V value, @Nullable Readable<V> source,
            HostAction<V> action) {
        Cons2<Element, V> setter = (element, resolved) -> {
            Component host = SolimToken.getComponent(element);
            if (host != null) {
                action.apply(element, resolved, host);
            }
        };
        return source != null ? Prop.elementSource(requires, label, source, setter)
                : Prop.element(requires, label, value, setter);
    }

    private static <V> Prop<ElementCapability, V> hostProp(String label, @Nullable V value, HostAction<V> action) {
        return hostProp(ElementCapability.class, label, value, null, action);
    }

    private static <V> Prop<ElementCapability, V> hostProp(String label, @Nullable V value,
            @Nullable Readable<V> source,
            HostAction<V> action) {
        return hostProp(ElementCapability.class, label, value, source, action);
    }

    @FunctionalInterface
    private interface HostAction<V> {
        void apply(Element element, @Nullable V value, Component host);
    }

    private static void removeTooltips(Element element) {
        for (int i = element.getListeners().size - 1; i >= 0; i--) {
            if (element.getListeners().get(i) instanceof Tooltip) {
                element.removeListener(element.getListeners().get(i));
            }
        }
    }
}
