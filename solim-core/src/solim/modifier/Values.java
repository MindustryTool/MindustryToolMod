package solim.modifier;

import arc.func.Cons;
import arc.func.Cons2;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.HostCapability;
import solim.core.SolimToken;
import solim.display.Badge;
import solim.display.Text;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimTextField;
import solim.layout.GapContainer;
import solim.layout.Scroll;
import solim.overlay.Hud;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;

public final class Values {

    private Values() {
    }

    public static Prop<CellCapability, Boolean> growX() {
        return Prop.cell(CellCapability.class, "growX", Boolean.TRUE, (config, v) -> config.growX = true);
    }

    public static Prop<CellCapability, Boolean> growY() {
        return Prop.cell(CellCapability.class, "growY", Boolean.TRUE, (config, v) -> config.growY = true);
    }

    public static Prop<CellCapability, Boolean> grow() {
        return Prop.cell(CellCapability.class, "grow", Boolean.TRUE, (config, v) -> {
            config.growX = true;
            config.growY = true;
        });
    }

    public static Prop<CellCapability, Float> prefWidth(float v) {
        return Prop.cell(CellCapability.class, "prefWidth", v, (config, val) -> config.prefWidth = Readable.of(val));
    }

    public static Prop<CellCapability, Float> prefWidth(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "prefWidth", v, (config, source) -> config.prefWidth = source);
    }

    public static Prop<CellCapability, Float> prefHeight(float v) {
        return Prop.cell(CellCapability.class, "prefHeight", v, (config, val) -> config.prefHeight = Readable.of(val));
    }

    public static Prop<CellCapability, Float> prefHeight(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "prefHeight", v, (config, source) -> config.prefHeight = source);
    }

    public static Prop<CellCapability, Float> minWidth(float v) {
        return Prop.cell(CellCapability.class, "minWidth", v, (config, val) -> config.minWidth = Readable.of(val));
    }

    public static Prop<CellCapability, Float> minWidth(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "minWidth", v, (config, source) -> config.minWidth = source);
    }

    public static Prop<CellCapability, Float> maxWidth(float v) {
        return Prop.cell(CellCapability.class, "maxWidth", v, (config, val) -> config.maxWidth = Readable.of(val));
    }

    public static Prop<CellCapability, Float> maxWidth(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "maxWidth", v, (config, source) -> config.maxWidth = source);
    }

    public static Prop<CellCapability, Float> minHeight(float v) {
        return Prop.cell(CellCapability.class, "minHeight", v, (config, val) -> config.minHeight = Readable.of(val));
    }

    public static Prop<CellCapability, Float> minHeight(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "minHeight", v, (config, source) -> config.minHeight = source);
    }

    public static Prop<CellCapability, Float> maxHeight(float v) {
        return Prop.cell(CellCapability.class, "maxHeight", v, (config, val) -> config.maxHeight = Readable.of(val));
    }

    public static Prop<CellCapability, Float> maxHeight(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "maxHeight", v, (config, source) -> config.maxHeight = source);
    }

    public static Prop<CellCapability, Float> margin(float v) {
        return margin(v, v, v, v);
    }

    public static Prop<CellCapability, Float> margin(float top, float left, float bottom, float right) {
        return Prop.cell(CellCapability.class, "margin", top, (config, val) -> {
            config.padTop = Readable.of(top);
            config.padLeft = Readable.of(left);
            config.padBottom = Readable.of(bottom);
            config.padRight = Readable.of(right);
        });
    }

    public static Prop<CellCapability, Float> margin(Readable<Float> v) {
        return margin(v, v, v, v);
    }

    public static Prop<CellCapability, Float> margin(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        if (top != null) {
            return Prop.cellSource(CellCapability.class, "margin", top, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (left != null) {
            return Prop.cellSource(CellCapability.class, "margin", left, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (bottom != null) {
            return Prop.cellSource(CellCapability.class, "margin", bottom, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (right != null) {
            return Prop.cellSource(CellCapability.class, "margin", right, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        return Prop.cell(CellCapability.class, "margin", 0f, (config, val) -> {
        });
    }

    public static Prop<CellCapability, Float> marginTop(float v) {
        return Prop.cell(CellCapability.class, "marginTop", v, (config, val) -> config.padTop = Readable.of(val));
    }

    public static Prop<CellCapability, Float> marginTop(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "marginTop", v, (config, source) -> config.padTop = source);
    }

    public static Prop<CellCapability, Float> marginBottom(float v) {
        return Prop.cell(CellCapability.class, "marginBottom", v, (config, val) -> config.padBottom = Readable.of(val));
    }

    public static Prop<CellCapability, Float> marginBottom(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "marginBottom", v, (config, source) -> config.padBottom = source);
    }

    public static Prop<CellCapability, Float> marginLeft(float v) {
        return Prop.cell(CellCapability.class, "marginLeft", v, (config, val) -> config.padLeft = Readable.of(val));
    }

    public static Prop<CellCapability, Float> marginLeft(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "marginLeft", v, (config, source) -> config.padLeft = source);
    }

    public static Prop<CellCapability, Float> marginRight(float v) {
        return Prop.cell(CellCapability.class, "marginRight", v, (config, val) -> config.padRight = Readable.of(val));
    }

    public static Prop<CellCapability, Float> marginRight(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "marginRight", v, (config, source) -> config.padRight = source);
    }

    public static Prop<CellCapability, Float> marginX(float v) {
        return Prop.cell(CellCapability.class, "marginX", v, (config, val) -> {
            config.padLeft = Readable.of(val);
            config.padRight = Readable.of(val);
        });
    }

    public static Prop<CellCapability, Float> marginX(@Nullable Readable<Float> v) {
        if (v == null) {
            return Prop.cell(CellCapability.class, "marginX", 0f, (config, val) -> {
            });
        }
        return Prop.cellSource(CellCapability.class, "marginX", v, (config, source) -> {
            config.padLeft = source;
            config.padRight = source;
        });
    }

    public static Prop<CellCapability, Float> marginY(float v) {
        return Prop.cell(CellCapability.class, "marginY", v, (config, val) -> {
            config.padTop = Readable.of(val);
            config.padBottom = Readable.of(val);
        });
    }

    public static Prop<CellCapability, Float> marginY(@Nullable Readable<Float> v) {
        if (v == null) {
            return Prop.cell(CellCapability.class, "marginY", 0f, (config, val) -> {
            });
        }
        return Prop.cellSource(CellCapability.class, "marginY", v, (config, source) -> {
            config.padTop = source;
            config.padBottom = source;
        });
    }

    public static Prop<AlignmentCapability, Integer> align(int align) {
        return alignProp("align", align, 0);
    }

    public static Prop<AlignmentCapability, Void> top() {
        return alignProp("top", null, Align.top);
    }

    public static Prop<AlignmentCapability, Void> bottom() {
        return alignProp("bottom", null, Align.bottom);
    }

    public static Prop<AlignmentCapability, Void> left() {
        return alignProp("left", null, Align.left);
    }

    public static Prop<AlignmentCapability, Void> right() {
        return alignProp("right", null, Align.right);
    }

    public static Prop<AlignmentCapability, Void> center() {
        return alignProp("center", null, Align.center);
    }

    private static <V> Prop<AlignmentCapability, V> alignProp(String label, @Nullable V value, int named) {
        final @Nullable Integer explicit = value instanceof Integer ? (Integer) value : null;
        return new Prop<>(Prop.Kind.CELL, AlignmentCapability.class, label,
                (view, val) -> view.withCapability(AlignmentCapability.class,
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
            return Prop.tableSource(TableCapability.class, "padding", right, (table, val) -> table.margin(0f, 0f, 0f, val != null ? val : 0f));
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

    public static Prop<CellCapability, Float> width(float v) {
        return Prop.cell(CellCapability.class, "width", Math.max(0f, v), (config, val) -> config.prefWidth = Readable.of(val));
    }

    public static Prop<CellCapability, Float> width(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "width", v, (config, source) -> config.prefWidth = source);
    }

    public static Prop<CellCapability, Float> height(float v) {
        return Prop.cell(CellCapability.class, "height", Math.max(0f, v), (config, val) -> config.prefHeight = Readable.of(val));
    }

    public static Prop<CellCapability, Float> height(Readable<Float> v) {
        return Prop.cellSource(CellCapability.class, "height", v, (config, source) -> config.prefHeight = source);
    }

    public static Prop<CellCapability, Float> size(float v) {
        return size(v, v);
    }

    public static Prop<CellCapability, Float> size(float width, float height) {
        return Prop.cell(CellCapability.class, "size", width, (config, val) -> {
            config.prefWidth = Readable.of(Math.max(0f, width));
            config.prefHeight = Readable.of(Math.max(0f, height));
        });
    }

    public static Prop<CellCapability, Float> size(@Nullable Readable<Float> size) {
        return size(size, size);
    }

    public static Prop<CellCapability, Float> size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        if (width != null && height != null) {
            return Prop.cellSource(CellCapability.class, "size", width, (config, source) -> {
                config.prefWidth = width;
                config.prefHeight = height;
            });
        }
        if (width != null) {
            return Prop.cellSource(CellCapability.class, "size", width, (config, source) -> config.prefWidth = width);
        }
        if (height != null) {
            return Prop.cellSource(CellCapability.class, "size", height, (config, source) -> config.prefHeight = height);
        }
        return Prop.cell(CellCapability.class, "size", 0f, (config, val) -> {
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
            if (host instanceof Hud) {
                ((Hud) host).x(source);
                return;
            }
            if (v != null) {
                element.x = v;
            }
        });
    }

    public static Prop<ElementCapability, Float> y(Readable<Float> source) {
        return hostProp("y", null, source, (element, v, host) -> {
            if (host instanceof Hud) {
                ((Hud) host).y(source);
                return;
            }
            if (v != null) {
                element.y = v;
            }
        });
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
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, null, null, null));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, null, x, y));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, hud, null, null));
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud, @Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Prop.raw(ElementCapability.class, "draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, hud, x, y));
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

    public static Prop<ElementCapability, Cons<SolimButtonStyleBuilder>> buttonStyle(
            @Nullable Cons<SolimButtonStyleBuilder> config) {
        return Prop.raw(ElementCapability.class, "buttonStyle", Prop.Kind.ELEMENT, target -> {
            Component owner = hostOf(target);
            if (owner instanceof Button) {
                ((Button) owner).style(config);
            }
        });
    }

    public static <S> Prop<ElementCapability, S> style(@Nullable S value) {
        return hostProp("style", value, Values::restyle);
    }

    public static <S> Prop<ElementCapability, S> style(@Nullable Readable<S> source) {
        return hostProp("style", null, source, Values::restyle);
    }

    public static Prop<ElementCapability, String> text(@Nullable String value) {
        return hostProp("text", value, (element, v, host) -> {
            if (host instanceof Button) {
                ((Button) host).text(v);
            } else if (host instanceof Text) {
                ((Text) host).text(v);
            }
        });
    }

    public static Prop<ElementCapability, String> text(@Nullable Readable<String> source) {
        return hostOwnedProp("text", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).text(source);
            } else if (host instanceof Text) {
                ((Text) host).text(source);
            }
        });
    }

    public static Prop<ElementCapability, Drawable> icon(@Nullable Drawable value) {
        return hostProp("icon", value, (element, v, host) -> {
            if (host instanceof Button && v != null) {
                ((Button) host).icon(v);
            }
        });
    }

    public static Prop<ElementCapability, Drawable> icon(@Nullable Readable<Drawable> source) {
        return hostOwnedProp("icon", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).icon(source);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> checked(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("checked", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).checked(source);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> enabled(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("enabled", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).enabled(source);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> disabled(boolean value) {
        return hostProp("disabled", value, (element, v, host) -> {
            boolean disabled = Boolean.TRUE.equals(v);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).disabled(disabled);
            } else if (host instanceof Button) {
                ((Button) host).disabled(() -> disabled);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> disabled(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("disabled", target -> {
            Component host = hostOf(target);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).disabled(source);
            } else if (host instanceof Button) {
                ((Button) host).disabled(() -> Boolean.TRUE.equals(source.get()));
            }
        });
    }

    public static Prop<ElementCapability, Boolean> wrap(boolean value) {
        return hostProp("wrap", value, (element, v, host) -> {
            boolean wrap = Boolean.TRUE.equals(v);
            if (host instanceof Text) {
                ((Text) host).wrap(wrap);
            } else if (host instanceof Checkbox) {
                ((Checkbox) host).wrap(wrap);
            }
        });
    }

    public static Prop<ElementCapability, Boolean> wrap() {
        return wrap(true);
    }

    public static Prop<ElementCapability, Boolean> ellipsis(boolean value) {
        return hostProp("ellipsis", value, (element, v, host) -> {
            if (host instanceof Text) {
                ((Text) host).ellipsis(Boolean.TRUE.equals(v));
            }
        });
    }

    public static Prop<ElementCapability, Boolean> ellipsis() {
        return ellipsis(true);
    }

    public static Prop<ElementCapability, Float> fontScale(float value) {
        return hostProp("fontScale", value, (element, v, host) -> {
            if (host instanceof Text && v != null) {
                ((Text) host).fontScale(v);
            }
        });
    }

    public static Prop<ElementCapability, Float> fontScale(@Nullable Readable<Float> source) {
        return hostProp("fontScale", null, source, (element, v, host) -> {
            if (host instanceof Text && v != null) {
                ((Text) host).fontScale(v);
            }
        });
    }

    public static <Req extends HostCapability> Prop<Req, Void> raw(Class<Req> requires, String label, Prop.Kind kind, Cons<Object> mutation) {
        return Prop.raw(requires, label, kind, mutation);
    }

    private static void paint(Element element, @Nullable Color value, Component host) {
        if (value == null) {
            return;
        }
        if (host instanceof Badge) {
            ((Badge) host).color(value);
            return;
        }
        element.setColor(value);
    }

    private static <S> void restyle(Element element, @Nullable S value, Component host) {
        if (value instanceof SolimButtonStyle && host instanceof Button) {
            ((Button) host).style((SolimButtonStyle) value);
        } else if (value instanceof ButtonStyle && host instanceof Button) {
            ((Button) host).style((ButtonStyle) value);
        } else if (value instanceof InputStyle && host instanceof SolimTextField) {
            ((SolimTextField) host).style((InputStyle) value);
        } else if (value instanceof ScrollPaneStyle && host instanceof Scroll) {
            ((Scroll) host).style((ScrollPaneStyle) value);
        } else if (value instanceof LabelStyle && host instanceof Text) {
            ((Text) host).style((LabelStyle) value);
        }
    }

    private static <V> Prop<ElementCapability, V> hostOwnedProp(String label, Cons<Object> action) {
        return Prop.raw(ElementCapability.class, label, Prop.Kind.ELEMENT, action);
    }

    private static @Nullable Component hostOf(@Nullable Object target) {
        return target instanceof Element ? SolimToken.getComponent((Element) target) : null;
    }

    private static <V> Prop<ElementCapability, V> hostProp(String label, @Nullable V value, HostAction<V> action) {
        return hostProp(label, value, null, action);
    }

    private static <V> Prop<ElementCapability, V> hostProp(String label, @Nullable V value, @Nullable Readable<V> source,
            HostAction<V> action) {
        Cons2<Element, V> setter = (element, resolved) -> {
            Component host = SolimToken.getComponent(element);
            if (host != null) {
                action.apply(element, resolved, host);
            }
        };
        return source != null ? Prop.elementSource(ElementCapability.class, label, source, setter)
                : Prop.element(ElementCapability.class, label, value, setter);
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
