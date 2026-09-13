package solim.modifier;

import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.graphics.RoundedDrawable;
import solim.overlay.Hud;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Category A — Self / Element modifiers.
 *
 * <p>Static utility methods that modify the Arc {@link arc.scene.Element} itself: its size,
 * position, visibility, color, name, and so on. These affect the element directly and do NOT
 * configure the element's parent cell or container child defaults.
 *
 * <p>Modifier targets:
 * <ul>
 *   <li>{@code width/height/size} — element's size (sets on the element directly)</li>
 *   <li>{@code x/y/position} — element's position in local coordinates</li>
 *   <li>{@code visible/opacity/alpha} — element's visibility and transparency</li>
 *   <li>{@code name} — element's debug name</li>
 *   <li>{@code align} — element's internal content alignment</li>
 *   <li>{@code gap} — spacing between children inside a Table-based container</li>
 *   <li>{@code margin/pad} — padding on the element (container-internal defaults)</li>
 * </ul>
 *
 * <p>Contrast with {@link solim.layout.LayoutModifiers}, which is a Category B interface that
 * configures how the element behaves inside its <em>parent</em> layout cell (grow, margin as
 * parent-cell padding, alignment in parent, etc.).
 */
public final class ElementModifiers {

    private ElementModifiers() {
    }

    public static void width(@Nullable Element element, float width) {
        if (element == null)
            return;
        float val = Math.max(0f, width);
        element.setWidth(val);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.width(val);
            }
        }
        element.invalidateHierarchy();
    }

    public static void height(@Nullable Element element, float height) {
        if (element == null)
            return;
        float val = Math.max(0f, height);
        element.setHeight(val);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.height(val);
            }
        }
        element.invalidateHierarchy();
    }

    public static void size(@Nullable Element element, float width, float height) {
        width(element, width);
        height(element, height);
    }

    public static void size(@Nullable Element element, float size) {
        size(element, size, size);
    }

    public static void x(@Nullable Element element, float x) {
        if (element == null)
            return;
        element.x = x;
    }

    public static void y(@Nullable Element element, float y) {
        if (element == null)
            return;
        element.y = y;
    }

    public static void position(@Nullable Element element, float x, float y) {
        if (element == null)
            return;
        element.setPosition(x, y);
    }

    public static void visible(@Nullable Element element, boolean visible) {
        if (element == null)
            return;
        element.visible = visible;
        if (element.parent instanceof Table) {
            solim.layout.GapContainer.respace((Table) element.parent);
        }
    }

    public static void visible(@Nullable Element element, @Nullable Readable<Boolean> visible) {
        if (element == null || visible == null)
            return;
        solim.signal.Effect e = solim.signal.Effect.of(() -> {
            Boolean val = visible.get();
            if (val != null) {
                element.visible = val;
                if (element.parent instanceof Table) {
                    solim.layout.GapContainer.respace((Table) element.parent);
                }
                element.invalidateHierarchy();
            }
        });
        solim.runtime.ComponentContext.register(e);
    }

    public static void opacity(@Nullable Element element, float opacity) {
        if (element == null)
            return;
        element.color.a = Math.max(0f, Math.min(1f, opacity));
    }

    public static void opacity(@Nullable Element element, @Nullable Readable<Float> opacity) {
        if (element == null || opacity == null)
            return;
        solim.signal.Effect e = solim.signal.Effect.of(() -> {
            Float val = opacity.get();
            if (val != null) {
                opacity(element, val);
            }
        });
        solim.runtime.ComponentContext.register(e);
    }

    public static void alpha(@Nullable Element element, float alpha) {
        opacity(element, alpha);
    }

    public static void alpha(@Nullable Element element, @Nullable Readable<Float> alpha) {
        opacity(element, alpha);
    }

    public static void name(@Nullable Element element, @Nullable String name) {
        if (element == null)
            return;
        element.name = name;
    }

    public static void align(@Nullable Table table, int align) {
        if (table == null)
            return;
        table.align(align);
    }

    public static void top(@Nullable Table table) {
        if (table == null)
            return;
        table.top();
    }

    public static void bottom(@Nullable Table table) {
        if (table == null)
            return;
        table.bottom();
    }

    public static void left(@Nullable Table table) {
        if (table == null)
            return;
        table.left();
    }

    public static void right(@Nullable Table table) {
        if (table == null)
            return;
        table.right();
    }

    public static void center(@Nullable Table table) {
        if (table == null)
            return;
        table.center();
    }

    public static void margin(@Nullable Table table, float margin) {
        if (table == null)
            return;
        table.margin(margin);
    }

    public static void margin(@Nullable Table table, float top, float left, float bottom, float right) {
        if (table == null)
            return;
        table.margin(top, left, bottom, right);
    }

    public static void marginTop(@Nullable Table table, float top) {
        if (table == null)
            return;
        table.marginTop(top);
    }

    public static void marginBottom(@Nullable Table table, float bottom) {
        if (table == null)
            return;
        table.marginBottom(bottom);
    }

    public static void marginLeft(@Nullable Table table, float left) {
        if (table == null)
            return;
        table.marginLeft(left);
    }

    public static void marginRight(@Nullable Table table, float right) {
        if (table == null)
            return;
        table.marginRight(right);
    }

    public static void margin(@Nullable Element element, float margin) {
        if (element == null)
            return;
        if (element instanceof Table) {
            margin((Table) element, margin);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.pad(margin);
            }
        }
    }

    public static void margin(@Nullable Element element, float top, float left, float bottom, float right) {
        if (element == null)
            return;
        if (element instanceof Table) {
            margin((Table) element, top, left, bottom, right);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.pad(top, left, bottom, right);
            }
        }
    }

    public static void marginTop(@Nullable Element element, float top) {
        if (element == null)
            return;
        if (element instanceof Table) {
            marginTop((Table) element, top);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padTop(top);
            }
        }
    }

    public static void marginBottom(@Nullable Element element, float bottom) {
        if (element == null)
            return;
        if (element instanceof Table) {
            marginBottom((Table) element, bottom);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padBottom(bottom);
            }
        }
    }

    public static void marginLeft(@Nullable Element element, float left) {
        if (element == null)
            return;
        if (element instanceof Table) {
            marginLeft((Table) element, left);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padLeft(left);
            }
        }
    }

    public static void marginRight(@Nullable Element element, float right) {
        if (element == null)
            return;
        if (element instanceof Table) {
            marginRight((Table) element, right);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padRight(right);
            }
        }
    }

    public static void pad(@Nullable Table table, float pad) {
        margin(table, pad);
    }

    public static void pad(@Nullable Table table, float top, float left, float bottom, float right) {
        margin(table, top, left, bottom, right);
    }

    public static void padTop(@Nullable Table table, float top) {
        marginTop(table, top);
    }

    public static void padBottom(@Nullable Table table, float bottom) {
        marginBottom(table, bottom);
    }

    public static void padLeft(@Nullable Table table, float left) {
        marginLeft(table, left);
    }

    public static void padRight(@Nullable Table table, float right) {
        marginRight(table, right);
    }

    public static void padding(@Nullable Table table, float padding) {
        margin(table, padding);
    }

    public static void padding(@Nullable Table table, float top, float left, float bottom, float right) {
        margin(table, top, left, bottom, right);
    }

    public static void paddingTop(@Nullable Table table, float top) {
        marginTop(table, top);
    }

    public static void paddingBottom(@Nullable Table table, float bottom) {
        marginBottom(table, bottom);
    }

    public static void paddingLeft(@Nullable Table table, float left) {
        marginLeft(table, left);
    }

    public static void paddingRight(@Nullable Table table, float right) {
        marginRight(table, right);
    }

    public static void padding(@Nullable Element element, float padding) {
        if (element == null)
            return;
        if (element instanceof Table) {
            padding((Table) element, padding);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.pad(padding);
            }
        }
    }

    public static void padding(@Nullable Element element, float top, float left, float bottom, float right) {
        if (element == null)
            return;
        if (element instanceof Table) {
            padding((Table) element, top, left, bottom, right);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.pad(top, left, bottom, right);
            }
        }
    }

    public static void paddingTop(@Nullable Element element, float top) {
        if (element == null)
            return;
        if (element instanceof Table) {
            paddingTop((Table) element, top);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padTop(top);
            }
        }
    }

    public static void paddingBottom(@Nullable Element element, float bottom) {
        if (element == null)
            return;
        if (element instanceof Table) {
            paddingBottom((Table) element, bottom);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padBottom(bottom);
            }
        }
    }

    public static void paddingLeft(@Nullable Element element, float left) {
        if (element == null)
            return;
        if (element instanceof Table) {
            paddingLeft((Table) element, left);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padLeft(left);
            }
        }
    }

    public static void paddingRight(@Nullable Element element, float right) {
        if (element == null)
            return;
        if (element instanceof Table) {
            paddingRight((Table) element, right);
        } else if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.padRight(right);
            }
        }
    }

    public static void respace(@Nullable Table table) {
        solim.layout.GapContainer.respace(table);
    }

    public static void gap(@Nullable Table table, float gap) {
        if (table == null)
            return;
        if (table.userObject instanceof solim.layout.GapContainer) {
            solim.layout.GapContainer gc = (solim.layout.GapContainer) table.userObject;
            if (gc instanceof solim.layout.Row) {
                ((solim.layout.Row) gc).gap(gap);
            } else if (gc instanceof solim.layout.Column) {
                ((solim.layout.Column) gc).gap(gap);
            } else if (gc instanceof solim.layout.Grid) {
                ((solim.layout.Grid) gc).gap(gap);
            } else if (gc instanceof solim.layout.Wrap) {
                ((solim.layout.Wrap) gc).gap(gap);
            } else if (gc instanceof solim.layout.Card) {
                ((solim.layout.Card) gc).gap(gap);
            } else if (gc instanceof solim.input.Button) {
                ((solim.input.Button) gc).gap(gap);
            } else if (gc instanceof GenericGapContainer) {
                ((GenericGapContainer) gc).setGap(gap);
            } else {
                gc.respace();
            }
        } else {
            GenericGapContainer ggc = new GenericGapContainer(table, gap);
            table.userObject = ggc;
            ggc.respace();
        }
        table.invalidateHierarchy();
    }

    public static void gap(@Nullable Element element, float gap) {
        if (element instanceof Table) {
            gap((Table) element, gap);
        }
    }

    private static final class GenericGapContainer implements solim.layout.GapContainer {
        private final Table table;
        private float gap;

        GenericGapContainer(Table table, float gap) {
            this.table = table;
            this.gap = gap;
        }

        void setGap(float gap) {
            this.gap = gap;
            respace();
        }

        @Override
        public solim.layout.Direction direction() {
            return solim.layout.Direction.HORIZONTAL;
        }

        @Override
        public float gap() {
            return gap;
        }

        @Override
        public void respace() {
            solim.layout.GapContainer.applySpacing(table, solim.layout.Direction.HORIZONTAL, gap);
        }
    }

    public static void draggable(@Nullable Element handle) {
        draggable(handle, null, null, null);
    }

    public static void draggable(@Nullable Element handle, @Nullable Signal<Float> xSignal,
            @Nullable Signal<Float> ySignal) {
        draggable(handle, null, xSignal, ySignal);
    }

    public static void draggable(@Nullable Element handle, @Nullable Hud hud) {
        draggable(handle, hud, null, null);
    }

    public static void draggable(@Nullable Element handle, @Nullable Hud hud, @Nullable Signal<Float> xSignal,
            @Nullable Signal<Float> ySignal) {
        if (handle == null)
            return;
        handle.touchable = Touchable.enabled;
        if (hud != null) {
            if (xSignal != null) hud.bindXSignal(xSignal);
            if (ySignal != null) hud.bindYSignal(ySignal);
        }
        handle.addListener(new InputListener() {
            private float lastStageX;
            private float lastStageY;
            private float lastX;
            private float lastY;
            private boolean useStage = false;

            private @Nullable Hud resolveHud() {
                Hud target = hud != null ? hud : Hud.find(handle);
                if (target != null) {
                    if (xSignal != null) target.bindXSignal(xSignal);
                    if (ySignal != null) target.bindYSignal(ySignal);
                }
                return target;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (event != null && event.listenerActor != null && handle.getScene() == null)
                    return false;
                if (event != null && isInteractiveDescendant(event.targetActor, handle))
                    return false;
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return false;
                lastX = x;
                lastY = y;
                if (event != null && (event.stageX != 0f || event.stageY != 0f)) {
                    lastStageX = event.stageX;
                    lastStageY = event.stageY;
                    useStage = true;
                } else {
                    useStage = false;
                }
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return;
                float dx, dy;
                if (useStage && event != null) {
                    dx = event.stageX - lastStageX;
                    dy = event.stageY - lastStageY;
                    lastStageX = event.stageX;
                    lastStageY = event.stageY;
                } else {
                    dx = x - lastX;
                    dy = y - lastY;
                    lastX = x;
                    lastY = y;
                }
                if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                    for (EventListener l : handle.getListeners()) {
                        if (l instanceof ClickListener) {
                            ((ClickListener) l).cancel();
                        }
                    }
                }
                targetHud.element().moveBy(dx, dy);
                targetHud.keepInScreen();
                if (xSignal != null) {
                    xSignal.set(targetHud.element().x);
                }
                if (ySignal != null) {
                    ySignal.set(targetHud.element().y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return;
                targetHud.keepInScreen();
                if (xSignal != null) {
                    xSignal.set(targetHud.element().x);
                }
                if (ySignal != null) {
                    ySignal.set(targetHud.element().y);
                }
            }
        });
    }

    private static boolean isInteractiveDescendant(@Nullable Element target, Element handle) {
        Element curr = target;
        while (curr != null && curr != handle) {
            if (curr instanceof Button) {
                return true;
            }
            for (EventListener l : curr.getListeners()) {
                if (l instanceof ClickListener) {
                    return true;
                }
            }
            curr = curr.parent;
        }
        return false;
    }

    public static @Nullable RoundedDrawable rounded(@Nullable Element element, int radius) {
        return rounded(element, radius, (Color) null);
    }

    public static @Nullable RoundedDrawable rounded(@Nullable Element element, int radius, @Nullable Color color) {
        if (element == null) return null;
        RoundedDrawable rd = getOrCreateRounded(element, radius);
        rd.radius(radius);
        if (color != null) {
            rd.fillColor(color);
        }
        return rd;
    }

    public static @Nullable RoundedDrawable rounded(@Nullable Element element, int radius, @Nullable Readable<Color> color) {
        if (element == null) return null;
        RoundedDrawable rd = getOrCreateRounded(element, radius);
        rd.radius(radius);
        if (color != null) {
            rd.fillColor(color);
        }
        return rd;
    }

    public static @Nullable RoundedDrawable border(@Nullable Element element, float stroke, @Nullable Color color) {
        if (element == null) return null;
        RoundedDrawable rd = getOrCreateRounded(element, 8);
        rd.border(stroke, color != null ? color : Color.white);
        return rd;
    }

    public static @Nullable RoundedDrawable border(@Nullable Element element, float stroke, @Nullable Readable<Color> color) {
        if (element == null) return null;
        RoundedDrawable rd = getOrCreateRounded(element, 8);
        if (color != null) {
            rd.border(stroke, color);
        } else {
            rd.border(stroke, Color.white);
        }
        return rd;
    }

    private static RoundedDrawable getOrCreateRounded(Element element, int defaultRadius) {
        if (element instanceof Table) {
            Table table = (Table) element;
            if (table.getBackground() instanceof RoundedDrawable) {
                return (RoundedDrawable) table.getBackground();
            }
            RoundedDrawable rd = new RoundedDrawable(defaultRadius);
            table.setBackground(rd);
            return rd;
        }
        return new RoundedDrawable(defaultRadius);
    }
}
