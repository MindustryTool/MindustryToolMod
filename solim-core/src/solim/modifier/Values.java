package solim.modifier;

import arc.func.Cons;
import arc.func.Cons2;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.style.Drawable;
import arc.util.Align;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.display.Badge;
import solim.display.Text;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimTextField;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.GapContainer;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimCollapser;
import solim.layout.Wrap;
import solim.overlay.Hud;
import solim.reactive.Dynamic;
import solim.reactive.Effect;
import solim.reactive.ForEach;
import solim.reactive.QueryView;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.When;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;

public final class Values {
    // TODO: Move prop def into UI.java
    private Values() {
    }

    private static float num(Object value) {
        return ((Number) value).floatValue();
    }

    // Cell

    public static Prop growX() {
        return Prop.cell("growX", Boolean.TRUE, (config, v) -> config.growX = true);
    }

    public static Prop growY() {
        return Prop.cell("growY", Boolean.TRUE, (config, v) -> config.growY = true);
    }

    public static Prop grow() {
        return Prop.cell("grow", Boolean.TRUE, (config, v) -> {
            config.growX = true;
            config.growY = true;
        });
    }

    public static Prop prefWidth(Object v) {
        return Prop.cell("prefWidth", (Object) Readable.of(num(v)), (config, val) -> config.prefWidth = (Readable<Float>) val);
    }

    public static Prop prefWidth(Readable<Float> v) {
        return Prop.cellSource("prefWidth", v, (config, source) -> config.prefWidth = (Readable<Float>) source);
    }

    public static Prop prefHeight(Object v) {
        return Prop.cell("prefHeight", (Object) Readable.of(num(v)), (config, val) -> config.prefHeight = (Readable<Float>) val);
    }

    public static Prop prefHeight(Readable<Float> v) {
        return Prop.cellSource("prefHeight", v, (config, source) -> config.prefHeight = (Readable<Float>) source);
    }

    public static Prop minWidth(Object v) {
        return Prop.cell("minWidth", (Object) Readable.of(num(v)), (config, val) -> config.minWidth = (Readable<Float>) val);
    }

    public static Prop minWidth(Readable<Float> v) {
        return Prop.cellSource("minWidth", v, (config, source) -> config.minWidth = (Readable<Float>) source);
    }

    public static Prop maxWidth(Object v) {
        return Prop.cell("maxWidth", (Object) Readable.of(num(v)), (config, val) -> config.maxWidth = (Readable<Float>) val);
    }

    public static Prop maxWidth(Readable<Float> v) {
        return Prop.cellSource("maxWidth", v, (config, source) -> config.maxWidth = (Readable<Float>) source);
    }

    public static Prop minHeight(Object v) {
        return Prop.cell("minHeight", (Object) Readable.of(num(v)), (config, val) -> config.minHeight = (Readable<Float>) val);
    }

    public static Prop minHeight(Readable<Float> v) {
        return Prop.cellSource("minHeight", v, (config, source) -> config.minHeight = (Readable<Float>) source);
    }

    public static Prop maxHeight(Object v) {
        return Prop.cell("maxHeight", (Object) Readable.of(num(v)), (config, val) -> config.maxHeight = (Readable<Float>) val);
    }

    public static Prop maxHeight(Readable<Float> v) {
        return Prop.cellSource("maxHeight", v, (config, source) -> config.maxHeight = (Readable<Float>) source);
    }

    public static Prop margin(float v) {
        return margin(v, v, v, v);
    }

    public static Prop margin(float top, float left, float bottom, float right) {
        return Prop.cell("margin", Boolean.TRUE, (config, val) -> {
            config.padTop = Readable.of(top);
            config.padLeft = Readable.of(left);
            config.padBottom = Readable.of(bottom);
            config.padRight = Readable.of(right);
        });
    }

    public static Prop margin(Readable<Float> v) {
        return margin(v, v, v, v);
    }

    public static Prop margin(Readable<Float> top, Readable<Float> left, Readable<Float> bottom,
            Readable<Float> right) {
        if (top != null) {
            return Prop.cellSource("margin", top, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (left != null) {
            return Prop.cellSource("margin", left, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (bottom != null) {
            return Prop.cellSource("margin", bottom, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        if (right != null) {
            return Prop.cellSource("margin", right, (config, source) -> {
                config.padTop = top;
                config.padLeft = left;
                config.padBottom = bottom;
                config.padRight = right;
            });
        }
        return Prop.cell("margin", Boolean.TRUE, (config, val) -> {
        });
    }

    public static Prop marginTop(float v) {
        return Prop.cell("marginTop", (Object) Readable.of(v), (config, val) -> config.padTop = (Readable<Float>) val);
    }

    public static Prop marginTop(Readable<Float> v) {
        return Prop.cellSource("marginTop", v, (config, source) -> config.padTop = (Readable<Float>) source);
    }

    public static Prop marginBottom(float v) {
        return Prop.cell("marginBottom", (Object) Readable.of(v), (config, val) -> config.padBottom = (Readable<Float>) val);
    }

    public static Prop marginBottom(Readable<Float> v) {
        return Prop.cellSource("marginBottom", v, (config, source) -> config.padBottom = (Readable<Float>) source);
    }

    public static Prop marginLeft(float v) {
        return Prop.cell("marginLeft", (Object) Readable.of(v), (config, val) -> config.padLeft = (Readable<Float>) val);
    }

    public static Prop marginLeft(Readable<Float> v) {
        return Prop.cellSource("marginLeft", v, (config, source) -> config.padLeft = (Readable<Float>) source);
    }

    public static Prop marginRight(float v) {
        return Prop.cell("marginRight", (Object) Readable.of(v), (config, val) -> config.padRight = (Readable<Float>) val);
    }

    public static Prop marginRight(Readable<Float> v) {
        return Prop.cellSource("marginRight", v, (config, source) -> config.padRight = (Readable<Float>) source);
    }

    public static Prop marginX(float v) {
        return Prop.cell("marginX", Boolean.TRUE, (config, val) -> {
            config.padLeft = Readable.of(v);
            config.padRight = Readable.of(v);
        });
    }

    public static Prop marginX(Readable<Float> v) {
        if (v == null) {
            return Prop.cell("marginX", Boolean.TRUE, (config, val) -> {
            });
        }
        return Prop.cellSource("marginX", v, (config, source) -> {
            config.padLeft = v;
            config.padRight = v;
        });
    }

    public static Prop marginY(float v) {
        return Prop.cell("marginY", Boolean.TRUE, (config, val) -> {
            config.padTop = Readable.of(v);
            config.padBottom = Readable.of(v);
        });
    }

    public static Prop marginY(Readable<Float> v) {
        if (v == null) {
            return Prop.cell("marginY", Boolean.TRUE, (config, val) -> {
            });
        }
        return Prop.cellSource("marginY", v, (config, source) -> {
            config.padTop = v;
            config.padBottom = v;
        });
    }

    // Table

    public static Prop align(int align) {
        return alignProp("align", align, 0);
    }

    public static Prop top() {
        return alignProp("top", Boolean.TRUE, Align.top);
    }

    public static Prop bottom() {
        return alignProp("bottom", Boolean.TRUE, Align.bottom);
    }

    public static Prop left() {
        return alignProp("left", Boolean.TRUE, Align.left);
    }

    public static Prop right() {
        return alignProp("right", Boolean.TRUE, Align.right);
    }

    public static Prop center() {
        return alignProp("center", Boolean.TRUE, Align.center);
    }

    private static Prop alignProp(String label, @Nullable Object value, int named) {
        final @Nullable Integer explicit = value instanceof Integer ? (Integer) value : null;
        return hostProp(label, value, (element, v, host) -> {
            if (alignCoordinated(host, explicit, named)) {
                return;
            }
            if (host instanceof Configurable) {
                PendingCellConfig config = ((Configurable<?>) host).cellConfig();
                if (config != null) {
                    if (explicit != null) {
                        config.align = explicit;
                    } else {
                        config.align(named);
                    }
                    config.applyAlignToParentCell(element);
                }
            }
            if (host instanceof Text) {
                alignLabel((Text) host, explicit, named);
            }
        });
    }

    /**
     * Runs the host's own coordinated alignment, which additionally fans out to the table
     * defaults and the existing cells. Returns false for hosts without one, letting the
     * caller fall back to recording the pending cell configuration.
     */
    private static boolean alignCoordinated(Component host, @Nullable Integer explicit, int named) {
        if (host instanceof Column) {
            alignColumn((Column) host, explicit, named);
        } else if (host instanceof Row) {
            alignRow((Row) host, explicit, named);
        } else if (host instanceof Card) {
            alignCard((Card) host, explicit, named);
        } else if (host instanceof Grid) {
            alignGrid((Grid) host, explicit, named);
        } else if (host instanceof Wrap) {
            alignWrap((Wrap) host, explicit, named);
        } else if (host instanceof Scroll) {
            alignScroll((Scroll) host, explicit, named);
        } else if (host instanceof SolimCollapser) {
            alignCollapser((SolimCollapser) host, explicit, named);
        } else if (host instanceof Button) {
            alignButton((Button) host, explicit, named);
        } else if (host instanceof Dynamic) {
            alignDynamic((Dynamic<?>) host, explicit, named);
        } else if (host instanceof When) {
            alignWhen((When) host, explicit, named);
        } else if (host instanceof ForEach) {
            alignForEach((ForEach<?>) host, explicit, named);
        } else if (host instanceof QueryView) {
            alignQueryView((QueryView<?>) host, explicit, named);
        } else {
            return false;
        }
        return true;
    }

    private static void alignTableContainer(Table table, PendingCellConfig constraints, Element element, @Nullable Integer explicit, int named) {
        int a;
        if (explicit != null) {
            a = explicit;
            constraints.align = explicit;
        } else if (named == Align.center) {
            a = Align.center;
            constraints.alignCenter();
        } else {
            constraints.align(named);
            a = constraints.align != null ? constraints.align : named;
        }
        table.align(a);
        constraints.applyAlignToParentCell(element);
        table.defaults().align(a);
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.align(a);
            }
        }
    }

    private static void alignColumn(Column host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignRow(Row host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignCard(Card host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignGrid(Grid host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignWrap(Wrap host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignScroll(Scroll host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignCollapser(SolimCollapser host, @Nullable Integer explicit, int named) {
        alignTableContainer(host.table(), host.cellConfig(), host.element(), explicit, named);
    }

    private static void alignButton(Button host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignDynamic(Dynamic<?> host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignWhen(When host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignForEach(ForEach<?> host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignQueryView(QueryView<?> host, @Nullable Integer explicit, int named) {
        if (explicit != null) {
            host.align(explicit);
        } else if (named == Align.top) {
            host.top();
        } else if (named == Align.bottom) {
            host.bottom();
        } else if (named == Align.left) {
            host.left();
        } else if (named == Align.right) {
            host.right();
        } else {
            host.center();
        }
    }

    private static void alignLabel(Text host, @Nullable Integer explicit, int named) {
        Label label = host.label();
        if (explicit != null) {
            label.setAlignment(explicit);
            return;
        }
        if (named == Align.center) {
            label.setAlignment(Align.center);
            return;
        }
        int current = label.getLabelAlign();
        int opposite = named == Align.top ? Align.bottom : named == Align.bottom ? Align.top
                : named == Align.left ? Align.right : Align.left;
        label.setAlignment((current | named) & ~opposite);
    }

    public static Prop buttonStyle(@Nullable Cons<SolimButtonStyleBuilder> config) {
        return Prop.raw("buttonStyle", Prop.Kind.ELEMENT, target -> {
            Component owner = SolimToken.getComponent((Element) target);
            if (owner instanceof Button) {
                ((Button) owner).style(config);
            }
        });
    }

    public static Prop padding(float v) {
        return Prop.table("padding", v, (table, val) -> { if (val != null) table.margin(num(val)); });
    }

    public static Prop padding(Readable<Float> v) {
        return Prop.table("padding", v, (table, val) -> { if (val != null) table.margin(num(val)); });
    }

    public static Prop padding(float top, float left, float bottom, float right) {
        return Prop.table("padding", Boolean.TRUE, (table, val) -> table.margin(top, left, bottom, right));
    }

    public static Prop padding(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        if (top != null) {
            return Prop.table("padding", top, (table, val) -> {
                float tv = num(val);
                float lv = left != null && left.get() != null ? left.get() : 0f;
                float bv = bottom != null && bottom.get() != null ? bottom.get() : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(tv, lv, bv, rv);
            });
        }
        if (left != null) {
            return Prop.table("padding", left, (table, val) -> {
                float lv = num(val);
                float bv = bottom != null && bottom.get() != null ? bottom.get() : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(0f, lv, bv, rv);
            });
        }
        if (bottom != null) {
            return Prop.table("padding", bottom, (table, val) -> {
                float bv = num(val);
                float rv = right != null && right.get() != null ? right.get() : 0f;
                table.margin(0f, 0f, bv, rv);
            });
        }
        if (right != null) {
            return Prop.table("padding", right, (table, val) -> {
                table.margin(0f, 0f, 0f, num(val));
            });
        }
        return Prop.table("padding", Boolean.TRUE, (table, val) -> {
        });
    }

    public static Prop paddingTop(float v) {
        return Prop.table("paddingTop", v, (table, val) -> { if (val != null) table.marginTop(num(val)); });
    }

    public static Prop paddingTop(Readable<Float> v) {
        return Prop.table("paddingTop", v, (table, val) -> { if (val != null) table.marginTop(num(val)); });
    }

    public static Prop paddingBottom(float v) {
        return Prop.table("paddingBottom", v, (table, val) -> { if (val != null) table.marginBottom(num(val)); });
    }

    public static Prop paddingBottom(Readable<Float> v) {
        return Prop.table("paddingBottom", v, (table, val) -> { if (val != null) table.marginBottom(num(val)); });
    }

    public static Prop paddingLeft(float v) {
        return Prop.table("paddingLeft", v, (table, val) -> { if (val != null) table.marginLeft(num(val)); });
    }

    public static Prop paddingLeft(Readable<Float> v) {
        return Prop.table("paddingLeft", v, (table, val) -> { if (val != null) table.marginLeft(num(val)); });
    }

    public static Prop paddingRight(float v) {
        return Prop.table("paddingRight", v, (table, val) -> { if (val != null) table.marginRight(num(val)); });
    }

    public static Prop paddingRight(Readable<Float> v) {
        return Prop.table("paddingRight", v, (table, val) -> { if (val != null) table.marginRight(num(val)); });
    }

    public static Prop paddingX(float v) {
        return Prop.table("paddingX", Boolean.TRUE, (table, val) -> {
            table.marginLeft(v);
            table.marginRight(v);
        });
    }

    public static Prop paddingX(@Nullable Readable<Float> x) {
        if (x == null) {
            return Prop.table("paddingX", Boolean.TRUE, (table, val) -> {
            });
        }
        return Prop.table("paddingX", x, (table, val) -> {
            if (val == null) {
                return;
            }
            float v = num(val);
            table.marginLeft(v);
            table.marginRight(v);
        });
    }

    public static Prop paddingY(float v) {
        return Prop.table("paddingY", Boolean.TRUE, (table, val) -> {
            table.marginTop(v);
            table.marginBottom(v);
        });
    }

    public static Prop paddingY(@Nullable Readable<Float> y) {
        if (y == null) {
            return Prop.table("paddingY", Boolean.TRUE, (table, val) -> {
            });
        }
        return Prop.table("paddingY", y, (table, val) -> {
            if (val == null) {
                return;
            }
            float v = num(val);
            table.marginTop(v);
            table.marginBottom(v);
        });
    }

    public static Prop gap(float v) {
        return Prop.table("gap", v, (table, val) -> { if (val != null) GenericGapContainer.setGap(table, num(val)); });
    }

    public static Prop gap(Readable<Float> v) {
        return Prop.table("gap", v, (table, val) -> { if (val != null) GenericGapContainer.setGap(table, num(val)); });
    }

    public static Prop rounded(int radius) {
        return Prop.table("rounded", radius, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, (Integer) val);
            drawable.radius((Integer) val);
        });
    }

    public static Prop rounded(int radius, @Nullable Color color) {
        return Prop.table("rounded", radius, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, (Integer) val);
            drawable.radius((Integer) val);
            if (color != null) {
                drawable.fillColor(color);
            }
        });
    }

    public static Prop rounded(int radius, @Nullable Readable<Color> color) {
        if (color == null) {
            return rounded(radius);
        }
        return Prop.table("rounded", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, radius);
            drawable.radius(radius);
            Color c = (Color) val;
            if (c != null) {
                drawable.fillColor(c);
            }
        });
    }

    public static Prop border(float stroke, @Nullable Color color) {
        return Prop.table("border", stroke, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.border(num(val), color != null ? color : Color.white);
        });
    }

    public static Prop border(float stroke, @Nullable Readable<Color> color) {
        if (color == null) {
            return Prop.table("border", stroke, (table, val) -> {
                RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
                drawable.border(num(val), Color.white);
            });
        }
        return Prop.table("border", color, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            Color c = (Color) val;
            drawable.border(stroke, c != null ? c : Color.white);
        });
    }

    public static Prop background(@Nullable Drawable drawable) {
        return Prop.table("background", drawable, (table, val) -> {
            Drawable d = (Drawable) val;
            if (table.getBackground() instanceof RoundedDrawable) {
                ((RoundedDrawable) table.getBackground()).baseDrawable(d);
            } else {
                table.setBackground(d);
            }
        });
    }

    public static Prop background(@Nullable Readable<Drawable> drawable) {
        return Prop.table("background", drawable, (table, val) -> {
            Drawable d = (Drawable) val;
            if (d == null) {
                return;
            }
            if (table.getBackground() instanceof RoundedDrawable) {
                ((RoundedDrawable) table.getBackground()).baseDrawable(d);
            } else {
                table.setBackground(d);
            }
        });
    }

    public static Prop background(@Nullable Color color) {
        return Prop.table("background", color, (table, val) -> {
            Color c = (Color) val;
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.fillColor(c != null ? c : Color.clear);
        });
    }

    public static Prop backgroundColor(@Nullable Color color) {
        return background(color);
    }

    public static Prop backgroundColor(@Nullable Readable<Color> color) {
        if (color == null) {
            return background((Color) null);
        }
        return Prop.table("backgroundColor", Boolean.TRUE, (table, val) -> {
            RoundedDrawable drawable = RoundedHelper.getOrCreateRounded(table, 8);
            drawable.fillColor(color);
        });
    }

    // Sizing
    // Length values are design units. A cell records them unchanged and Arc applies Scl.scl()
    // when it lays the cell out, so width(232) renders at 232 * Scl.scl() scene pixels (162 at the
    // default 70% UI scale). A host with no parent cell falls back to element.setWidth/Height(
    // Scl.scl(value)). element.getWidth()/getHeight() report the rendered scene pixels, so read
    // those, never the design value, when the actual size matters.

    public static Prop width(Object v) {
        return Prop.cell("width", (Object) Readable.of(Math.max(0f, num(v))),
                (config, val) -> config.prefWidth = (Readable<Float>) val);
    }

    public static Prop width(Readable<Float> v) {
        return Prop.cellSource("width", v, (config, source) -> config.prefWidth = (Readable<Float>) source);
    }

    public static Prop height(Object v) {
        return Prop.cell("height", (Object) Readable.of(Math.max(0f, num(v))),
                (config, val) -> config.prefHeight = (Readable<Float>) val);
    }

    public static Prop height(Readable<Float> v) {
        return Prop.cellSource("height", v, (config, source) -> config.prefHeight = (Readable<Float>) source);
    }

    public static Prop size(Object v) {
        return size(v, v);
    }

    public static Prop size(Object width, Object height) {
        return Prop.cell("size", (Object) Readable.of(Math.max(0f, num(width))), (config, val) -> {
            config.prefWidth = (Readable<Float>) val;
            config.prefHeight = Readable.of(Math.max(0f, num(height)));
        });
    }

    public static Prop size(@Nullable Readable<Float> size) {
        return size(size, size);
    }

    public static Prop size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        if (width != null) {
            return Prop.cellSource("size", width, (config, source) -> {
                config.prefWidth = (Readable<Float>) source;
                config.prefHeight = height;
            });
        }
        if (height != null) {
            return Prop.cellSource("size", height, (config, source) -> config.prefHeight = (Readable<Float>) source);
        }
        return Prop.cell("size", Boolean.TRUE, (config, val) -> {
        });
    }

    public static Prop x(float v) {
        return Prop.element("x", v, (element, val) -> element.x = num(val));
    }

    public static Prop y(float v) {
        return Prop.element("y", v, (element, val) -> element.y = num(val));
    }


    public static Prop position(float x, float y) {
        return Prop.element("position", Boolean.TRUE, (element, val) -> {
            element.x = x;
            element.y = y;
        });
    }

    public static Prop position(@Nullable Readable<Float> x, @Nullable Readable<Float> y) {
        if (x != null) {
            return Prop.element("position", x, (element, val) -> {
                bindHudSignals(element, x, y);
                if (val == null) {
                    return;
                }
                element.x = num(val);
                if (y != null) {
                    Float yv = y.get();
                    if (yv != null) {
                        element.y = yv;
                    }
                }
            });
        }
        if (y != null) {
            return Prop.element("position", y, (element, val) -> {
                bindHudSignals(element, x, y);
                if (val == null) {
                    return;
                }
                element.y = num(val);
            });
        }
        return Prop.element("position", Boolean.TRUE, (element, val) -> {
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

    public static Prop visible(boolean visible) {
        return Prop.element("visible", visible, (element, val) -> {
            element.visible = (Boolean) val;
            respaceParent(element);
        });
    }

    public static Prop visible(Readable<Boolean> visible) {
        return Prop.element("visible", visible, (element, val) -> {
            element.visible = Boolean.TRUE.equals(val);
            respaceParent(element);
        });
    }

    public static Prop opacity(float opacity) {
        return hostProp("opacity", opacity, Values::fade);
    }

    public static Prop opacity(Readable<Float> source) {
        return hostProp("opacity", null, source, Values::fade);
    }

    public static Prop alpha(float alpha) {
        return opacity(alpha);
    }

    public static Prop alpha(Readable<Float> source) {
        return opacity(source);
    }

    public static Prop x(Readable<Float> source) {
        return hostProp("x", null, source, (element, v, host) -> {
            if (host instanceof Hud) {
                ((Hud) host).x(source);
                return;
            }
            if (v != null) {
                element.x = ((Number) v).floatValue();
            }
        });
    }

    public static Prop y(Readable<Float> source) {
        return hostProp("y", null, source, (element, v, host) -> {
            if (host instanceof Hud) {
                ((Hud) host).y(source);
                return;
            }
            if (v != null) {
                element.y = ((Number) v).floatValue();
            }
        });
    }

    private static void fade(Element element, @Nullable Object value, Component host) {
        if (!(value instanceof Number)) {
            return;
        }
        float a = Math.max(0f, Math.min(1f, ((Number) value).floatValue()));
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

    public static Prop name(String name) {
        return Prop.element("name", name, (element, val) -> element.name = (String) val);
    }

    public static Prop onClick(Runnable action) {
        return Prop.element("onClick", action, (element, val) -> {
            ElementClickBinding binding = ElementClickBinding.find(element);
            if (binding == null) {
                binding = new ElementClickBinding();
                element.addListener(binding);
            }
            binding.handler = (Runnable) val;
            element.touchable = Touchable.enabled;
        });
    }

    public static Prop stopClickPropagation(boolean stop) {
        return Prop.element("stopClickPropagation", stop, (element, val) -> {
            ElementClickBinding binding = ElementClickBinding.find(element);
            if (binding != null) {
                binding.stop = (Boolean) val;
            }
        });
    }

    public static Prop tooltip(@Nullable String tip) {
        return Prop.raw("tooltip", Prop.Kind.ELEMENT, target -> {
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

    public static Prop tooltip(@Nullable Readable<String> tip) {
        return Prop.raw("tooltip", Prop.Kind.ELEMENT, target -> {
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

    public static Prop tooltip(@Nullable Cons<Table> builder) {
        return Prop.raw("tooltip", Prop.Kind.ELEMENT, target -> {
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

    public static Prop draggable() {
        return Prop.raw("draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, null, null, null));
    }

    public static Prop draggable(@Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Prop.raw("draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, null, x, y));
    }

    public static Prop draggable(@Nullable Hud hud) {
        return Prop.raw("draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, hud, null, null));
    }

    public static Prop draggable(@Nullable Hud hud, @Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Prop.raw("draggable", Prop.Kind.ELEMENT, target -> Hud.makeDraggable((Element) target, hud, x, y));
    }

    public static Prop update(Cons<Element> fn) {
        return Prop.element("update", fn, (element, val) -> element.update(() -> ((Cons<Element>) val).get(element)));
    }

    public static Prop origin(int align) {
        return Prop.element("origin", align, (element, val) -> element.setOrigin((Integer) val));
    }

    public static Prop rotation(float degree) {
        return Prop.element("rotation", degree, (element, val) -> element.rotation = num(val));
    }

    // Component-specific

    public static Prop color(@Nullable Color value) {
        return hostProp("color", value, Values::paint);
    }

    public static Prop color(@Nullable Readable<Color> source) {
        return hostProp("color", null, source, Values::paint);
    }

    public static Prop style(@Nullable Object value) {
        return hostProp("style", value, Values::restyle);
    }

    public static Prop style(@Nullable Readable<?> source) {
        return hostProp("style", null, source, Values::restyle);
    }

    public static Prop text(@Nullable String value) {
        return hostProp("text", value, (element, v, host) -> {
            if (host instanceof Button) {
                ((Button) host).text((String) v);
            } else if (host instanceof Text) {
                ((Text) host).text((String) v);
            }
        });
    }

    public static Prop text(@Nullable Readable<String> source) {
        return hostOwnedProp("text", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).text(source);
            } else if (host instanceof Text) {
                ((Text) host).text(source);
            }
        });
    }

    public static Prop icon(@Nullable Drawable value) {
        return hostProp("icon", value, (element, v, host) -> {
            if (host instanceof Button && v != null) {
                ((Button) host).icon((Drawable) v);
            }
        });
    }

    public static Prop icon(@Nullable Readable<Drawable> source) {
        return hostOwnedProp("icon", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).icon(source);
            }
        });
    }

    public static Prop checked(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("checked", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).checked(source);
            }
        });
    }

    public static Prop enabled(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("enabled", target -> {
            Component host = hostOf(target);
            if (host instanceof Button) {
                ((Button) host).enabled(source);
            }
        });
    }

    public static Prop disabled(boolean value) {
        return hostProp("disabled", value, (element, v, host) -> {
            boolean disabled = Boolean.TRUE.equals(v);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).disabled(disabled);
            } else if (host instanceof Button) {
                ((Button) host).disabled(() -> disabled);
            }
        });
    }

    public static Prop disabled(@Nullable Readable<Boolean> source) {
        return hostOwnedProp("disabled", target -> {
            Component host = hostOf(target);
            if (host instanceof SolimTextField) {
                ((SolimTextField) host).disabled(source);
            } else if (host instanceof Button) {
                ((Button) host).disabled(() -> Boolean.TRUE.equals(source.get()));
            }
        });
    }

    public static Prop wrap(boolean value) {
        return hostProp("wrap", value, (element, v, host) -> {
            boolean wrap = Boolean.TRUE.equals(v);
            if (host instanceof Text) {
                ((Text) host).wrap(wrap);
            } else if (host instanceof Checkbox) {
                ((Checkbox) host).wrap(wrap);
            }
        });
    }

    public static Prop wrap() {
        return wrap(true);
    }

    public static Prop ellipsis(boolean value) {
        return hostProp("ellipsis", value, (element, v, host) -> {
            if (host instanceof Text) {
                ((Text) host).ellipsis(Boolean.TRUE.equals(v));
            }
        });
    }

    public static Prop ellipsis() {
        return ellipsis(true);
    }

    public static Prop fontScale(float value) {
        return hostProp("fontScale", value, (element, v, host) -> {
            if (host instanceof Text && v != null) {
                ((Text) host).fontScale(num(v));
            }
        });
    }

    public static Prop fontScale(@Nullable Readable<Float> source) {
        return hostProp("fontScale", null, source, (element, v, host) -> {
            if (host instanceof Text && v != null) {
                ((Text) host).fontScale(((Number) v).floatValue());
            }
        });
    }

    private static void paint(Element element, @Nullable Object value, Component host) {
        Color color = (Color) value;
        if (color == null) {
            return;
        }
        if (host instanceof Badge) {
            ((Badge) host).color(color);
            return;
        }
        element.setColor(color);
    }

    private static void restyle(Element element, @Nullable Object value, Component host) {
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

    private static Prop hostOwnedProp(String label, Cons<Object> action) {
        return Prop.raw(label, Prop.Kind.ELEMENT, action);
    }

    private static @Nullable Component hostOf(@Nullable Object target) {
        return target instanceof Element ? SolimToken.getComponent((Element) target) : null;
    }

    private static Prop hostProp(String label, @Nullable Object value, HostAction action) {
        return hostProp(label, value, null, action);
    }

    private static Prop hostProp(String label, @Nullable Object value, @Nullable Readable<?> source,
            HostAction action) {
        Cons2<Element, Object> setter = (element, resolved) -> {
            Component host = SolimToken.getComponent(element);
            if (host != null) {
                action.apply(element, resolved, host);
            }
        };
        return source != null ? Prop.element(label, source, setter) : Prop.element(label, value, setter);
    }

    @FunctionalInterface
    private interface HostAction {
        void apply(Element element, @Nullable Object value, Component host);
    }

    private static void removeTooltips(Element element) {
        for (int i = element.getListeners().size - 1; i >= 0; i--) {
            if (element.getListeners().get(i) instanceof Tooltip) {
                element.removeListener(element.getListeners().get(i));
            }
        }
    }
}
