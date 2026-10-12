package solim.layout;

import solim.modifier.CellConfig;

import arc.func.Cons;

import java.util.ArrayList;
import java.util.List;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.modifier.PendingCellConfig;

/** Scroll container wrapping a Table in a ScrollPane. */
public final class Scroll extends Component implements CellConfig<Scroll>, ElementConfig<Scroll>, TableConfig<Scroll> {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        if (SolimToken.isExpandingChild(child)) {
            cell.growY();
        }
        cell.row();
        return cell;
    };

    private final Table outer;
    private final Table content;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final ScrollPane pane;
    private boolean centered = false;
    private boolean disableX = true;
    private boolean disableY = false;
    private final List<Runnable> reachTopListeners = new ArrayList<>();
    private final List<Runnable> reachBottomListeners = new ArrayList<>();
    private float topThreshold = 100f;
    private float bottomThreshold = 100f;
    private boolean inTopZone = false;
    private boolean inBottomZone = false;
    private boolean updateHooked = false;

    public Scroll() {
        this(new Table());
    }

    private Scroll(Table outer) {
        super(outer);
        this.outer = outer;
        SolimToken.bind(this.outer, this, constraints);
        this.outer.name = "solim-scroll-pane-outer";
        this.outer.top().left();
        this.content = new Table();
        this.content.top().left();

        this.content.name = "solim-scroll-pane-content";
        if (Core.scene != null) {
            this.pane = outer.pane(content).grow().scrollX(false).scrollY(true).get();
            this.pane.name = "solim-scroll-pane";
            this.pane.setScrollingDisabled(disableX, disableY);
            this.pane.update(() -> {
                if (pane.hasScroll()) {
                    Element hover = Core.scene != null ? Core.scene.getHoverElement() : null;
                    if (hover == null || !hover.isDescendantOf(pane) || (!pane.isScrollX() && !pane.isScrollY())) {
                        Core.scene.setScrollFocus(null);
                    }
                }
            });
            this.pane.addCaptureListener(new InputListener() {
                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                    if (pane.isScrollingDisabledY() && !pane.isScrollingDisabledX() && amountY != 0f && amountX == 0f) {
                        float wheelX = Math.min(pane.getScrollWidth(), pane.getScrollWidth() * 0.9f / 4f);
                        if (wheelX <= 0f)
                            wheelX = 20f;
                        pane.setScrollX(pane.getScrollX() + wheelX * amountY);
                        return true;
                    }
                    return false;
                }
            });
        } else {
            this.pane = null;
            outer.add(content).grow();
        }
    }

    public Table outer() {
        return outer;
    }

    public Scroll style(ScrollPaneStyle style) {
        if (this.pane != null) {
            this.pane.setStyle(style);
        }
        return this;
    }

    public Scroll outer(Cons<Table> Cons) {
        Cons.get(outer);
        return this;
    }

    public Table content() {
        return content;
    }

    public Scroll content(Cons<Table> Cons) {
        Cons.get(content);
        return this;
    }

    public ScrollPane pane() {
        return pane;
    }

    public Scroll pane(Cons<ScrollPane> Cons) {
        Cons.get(pane);
        return this;
    }

    public Scroll scrollingDisabled(boolean disableX, boolean disableY) {
        this.disableX = disableX;
        this.disableY = disableY;
        if (pane != null) {
            pane.setScrollingDisabled(disableX, disableY);
        }
        return this;
    }

    public Scroll scrollX(boolean scrollX) {
        return scrollingDisabled(!scrollX, this.disableY);
    }

    public Scroll scrollY(boolean scrollY) {
        return scrollingDisabled(this.disableX, !scrollY);
    }

    public boolean isScrollingDisabledX() {
        return disableX;
    }

    public boolean isScrollingDisabledY() {
        return disableY;
    }

    public Scroll scrollPercentY(float percent) {
        if (pane != null) {
            pane.setScrollPercentY(percent);
        }
        return this;
    }

    public Scroll scrollPercentX(float percent) {
        if (pane != null) {
            pane.setScrollPercentX(percent);
        }
        return this;
    }

    public Scroll onReachTop(float thresholdPx, Runnable callback) {
        this.topThreshold = thresholdPx;
        if (callback != null) {
            this.reachTopListeners.add(callback);
            ensureUpdateHook();
        }
        return this;
    }

    public Scroll onReachTop(Runnable callback) {
        return onReachTop(100f, callback);
    }

    public Scroll onReachBottom(float thresholdPx, Runnable callback) {
        this.bottomThreshold = thresholdPx;
        if (callback != null) {
            this.reachBottomListeners.add(callback);
            ensureUpdateHook();
        }
        return this;
    }

    public Scroll onReachBottom(Runnable callback) {
        return onReachBottom(100f, callback);
    }

    public void checkScrollBoundary(float scrollY, float maxY) {
        if (maxY > topThreshold) {
            if (scrollY <= topThreshold) {
                if (!inTopZone) {
                    inTopZone = true;
                    for (Runnable r : reachTopListeners) {
                        r.run();
                    }
                }
            } else {
                inTopZone = false;
            }
        }

        if (maxY > bottomThreshold) {
            if (maxY - scrollY <= bottomThreshold) {
                if (!inBottomZone) {
                    inBottomZone = true;
                    for (Runnable r : reachBottomListeners) {
                        r.run();
                    }
                }
            } else {
                inBottomZone = false;
            }
        }
    }

    private void ensureUpdateHook() {
        if (!updateHooked && pane != null) {
            updateHooked = true;
            pane.update(() -> checkScrollBoundary(pane.getScrollY(), pane.getMaxY()));
        }
    }

    public Scroll scrollToTop() {
        if (pane != null) {
            pane.setScrollPercentY(0f);
        }
        return this;
    }

    public Scroll scrollToBottom() {
        if (pane != null) {
            pane.setScrollPercentY(1f);
        }
        return this;
    }

    @Override
    public Scroll align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        outer.align(align);
        content.align(align);
        content.defaults().align(align);
        for (Cell<?> cell : content.getCells()) {
            cell.align(align);
        }
        return this;
    }

    @Override
    public Scroll top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        outer.top();
        content.top();
        content.defaults().top();
        for (Cell<?> cell : content.getCells()) {
            cell.top();
        }
        return this;
    }

    @Override
    public Scroll bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        outer.bottom();
        content.bottom();
        content.defaults().bottom();
        for (Cell<?> cell : content.getCells()) {
            cell.bottom();
        }
        return this;
    }

    @Override
    public Scroll center() {
        this.centered = true;
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        outer.center();
        content.center();
        content.defaults().center();
        for (Cell<?> cell : content.getCells()) {
            cell.center();
        }
        return this;
    }

    @Override
    public Scroll left() {
        this.centered = false;
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        outer.left();
        content.left();
        content.defaults().left();
        for (Cell<?> cell : content.getCells()) {
            cell.left();
        }
        return this;
    }

    @Override
    public Scroll right() {
        this.centered = false;
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        outer.right();
        content.right();
        content.defaults().right();
        for (Cell<?> cell : content.getCells()) {
            cell.right();
        }
        return this;
    }


    @Override
    public Table table() {
        return outer;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public Scroll x(float x) {
        Element el = element();
        if (el != null)
            el.x = x;
        return this;
    }

    @Override
    public Scroll y(float y) {
        Element el = element();
        if (el != null)
            el.y = y;
        return this;
    }

    @Override
    public Scroll position(float x, float y) {
        Element el = element();
        if (el != null)
            el.setPosition(x, y);
        return this;
    }

    @Override
    public Scroll visible(boolean visible) {
        Element el = element();
        if (el == null)
            return this;
        el.visible = visible;
        if (el.parent instanceof Table) {
            GapContainer.respace((Table) el.parent);
        }
        return this;
    }

    public Scroll children(@Nullable Runnable r) {
        AttachmentStack.Attacher attacher = (table, child) -> {
            Cell<?> cell = table.add(child);
            if (centered) {
                cell.center().top();
            } else {
                cell.top().left();
            }
            if (SolimToken.isExpandingChild(child)) {
                cell.growY();
            }
            cell.row();
            return cell;
        };
        OwnershipContext.push(this);
        AttachmentStack.push(content, attacher);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        AttachmentStack.attachToParent(outer);
        return this;
    }

    public Scroll add(Element child) {
        Cell<?> cell = content.add(child);
        if (centered) {
            cell.center().top();
        } else {
            cell.top().left();
        }
        cell.row();
        return this;
    }

    @Override
    protected void onDispose() {
        reachTopListeners.clear();
        reachBottomListeners.clear();
    }

    @Override
    public Scroll name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Scroll self() {
        return this;
    }

}
