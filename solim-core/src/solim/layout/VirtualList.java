package solim.layout;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.runtime.StructuralReconciler;

/**
 * High-performance virtualized vertical list component.
 *
 * <p>Only instantiates and mounts items that intersect the visible scroll viewport plus
 * a configurable overscan buffer.
 *
 * @param <T> the item type
 * @param <K> the item key type
 */
public final class VirtualList<T, K> extends BaseComponent implements LayoutModifiers<VirtualList<T, K>> {

    private final Table outer = new Table();
    private final SizeConstraints constraints = new SizeConstraints();
    private final @Nullable ScrollPane pane;
    private final VirtualContainer content;

    private final Readable<? extends List<T>> collection;
    private final Function<T, K> keyExtractor;
    private final ItemHeightProvider<T> heightProvider;
    private final Function<T, Component> itemFactory;
    private final StructuralReconciler<K, Component> reconciler = new StructuralReconciler<>();

    private int overscan = 3;
    private float gap = 0f;
    private float lastScrollY = -1f;
    private float lastMeasuredWidth = -1f;

    private float[] yOffsets = new float[0];
    private float[] itemHeights = new float[0];
    private float totalHeight = 0f;
    private List<T> currentItems = Collections.emptyList();

    private final List<Runnable> reachTopListeners = new ArrayList<>();
    private final List<Runnable> reachBottomListeners = new ArrayList<>();
    private float topThreshold = 100f;
    private float bottomThreshold = 100f;
    private boolean inTopZone = false;
    private boolean inBottomZone = false;
    private boolean updateHooked = false;

    public VirtualList(
            Readable<? extends List<T>> collection,
            Function<T, K> keyExtractor,
            ItemHeightProvider<T> heightProvider,
            Function<T, Component> itemFactory) {
        this.collection = collection;
        this.keyExtractor = keyExtractor;
        this.heightProvider = heightProvider;
        this.itemFactory = itemFactory;

        this.outer.userObject = this;
        this.outer.name = "solim-virtual-list-outer";
        this.outer.top().left();

        this.content = new VirtualContainer();
        this.content.name = "solim-virtual-list-content";

        if (Core.scene != null) {
            this.pane = outer.pane(content).grow().scrollX(false).scrollY(true).get();
            this.pane.name = "solim-virtual-list-pane";
            this.pane.setScrollingDisabled(true, false);
            ensureUpdateHook();
        } else {
            this.pane = null;
            this.outer.add(content).grow();
        }
    }

    public static <T, K> VirtualList<T, K> of(
            Readable<? extends List<T>> collection,
            Function<T, K> keyExtractor,
            ItemHeightProvider<T> heightProvider,
            Function<T, Component> itemFactory) {
        return new VirtualList<>(collection, keyExtractor, heightProvider, itemFactory);
    }

    public static <T, K> VirtualList<T, K> of(
            List<T> items,
            Function<T, K> keyExtractor,
            ItemHeightProvider<T> heightProvider,
            Function<T, Component> itemFactory) {
        return new VirtualList<>(Signal.of(items), keyExtractor, heightProvider, itemFactory);
    }

    public VirtualList<T, K> overscan(int overscan) {
        this.overscan = Math.max(0, overscan);
        return this;
    }

    public VirtualList<T, K> gap(float gap) {
        this.gap = Math.max(0f, gap);
        recalculateHeights(content.getWidth());
        return this;
    }

    public @Nullable ScrollPane pane() {
        return pane;
    }

    public VirtualList<T, K> pane(Consumer<ScrollPane> consumer) {
        if (pane != null) {
            consumer.accept(pane);
        }
        return this;
    }

    public Table outer() {
        return outer;
    }

    public VirtualContainer content() {
        return content;
    }

    public VirtualList<T, K> onReachTop(float thresholdPx, Runnable callback) {
        this.topThreshold = thresholdPx;
        if (callback != null) {
            this.reachTopListeners.add(callback);
            ensureUpdateHook();
        }
        return this;
    }

    public VirtualList<T, K> onReachTop(Runnable callback) {
        return onReachTop(100f, callback);
    }

    public VirtualList<T, K> onReachBottom(float thresholdPx, Runnable callback) {
        this.bottomThreshold = thresholdPx;
        if (callback != null) {
            this.reachBottomListeners.add(callback);
            ensureUpdateHook();
        }
        return this;
    }

    public VirtualList<T, K> onReachBottom(Runnable callback) {
        return onReachBottom(100f, callback);
    }

    public VirtualList<T, K> scrollToTop() {
        if (pane != null) {
            pane.setScrollPercentY(0f);
        }
        return this;
    }

    public VirtualList<T, K> scrollToBottom() {
        if (pane != null) {
            Core.app.post(() -> {
                if (pane != null) {
                    pane.layout();
                    pane.setScrollPercentY(1f);
                    pane.updateVisualScroll();
                }
            });
        }
        return this;
    }

    public VirtualList<T, K> scrollPercentY(float percent) {
        if (pane != null) {
            pane.setScrollPercentY(percent);
        }
        return this;
    }

    public float getTotalHeight() {
        return totalHeight;
    }

    public int getMountedCount() {
        return content.getChildren().size;
    }

    @Override
    public SizeConstraints sizeConstraints() {
        return constraints;
    }

    @Override
    protected Element build() {
        Effect.of(this::onCollectionChanged);
        return outer;
    }

    private void onCollectionChanged() {
        List<T> list = collection.get();
        this.currentItems = list != null ? list : Collections.<T>emptyList();
        float width = content.getWidth();
        if (width <= 0 && pane != null) {
            width = pane.getWidth();
        }
        recalculateHeights(width);
        reconcileVisible();
    }

    public void recalculateHeights(float containerWidth) {
        int n = currentItems.size();
        if (n == 0) {
            yOffsets = new float[0];
            itemHeights = new float[0];
            totalHeight = 0f;
            lastMeasuredWidth = containerWidth;
            content.invalidate();
            return;
        }

        yOffsets = new float[n];
        itemHeights = new float[n];
        float curY = 0f;
        float effectiveWidth = containerWidth > 0 ? containerWidth : 300f;

        for (int i = 0; i < n; i++) {
            T item = currentItems.get(i);
            float h = Math.max(0f, heightProvider.getHeight(item, effectiveWidth));
            itemHeights[i] = h;
            yOffsets[i] = curY;
            curY += h;
            if (i < n - 1) {
                curY += gap;
            }
        }
        totalHeight = curY;
        lastMeasuredWidth = containerWidth;
        content.invalidate();
    }

    public void reconcileVisible() {
        int n = currentItems.size();
        if (n == 0) {
            reconciler.reconcile(Collections.emptyList(), keyExtractor, itemFactory);
            content.clearChildren();
            return;
        }

        float scrollY = 0f;
        float viewportHeight = 0f;
        if (pane != null) {
            scrollY = pane.getVisualScrollY();
            viewportHeight = pane.getHeight();
        }
        if (viewportHeight <= 0) {
            viewportHeight = 1000f;
        }

        float visibleTop = scrollY;
        float visibleBottom = scrollY + viewportHeight;

        int firstIndex = findFirstVisible(yOffsets, itemHeights, visibleTop);
        int lastIndex = findLastVisible(yOffsets, visibleBottom);

        int start = Math.max(0, firstIndex - overscan);
        int end = Math.min(n - 1, lastIndex + overscan);

        List<T> visibleItems = new ArrayList<>(Math.max(0, end - start + 1));
        for (int i = start; i <= end; i++) {
            visibleItems.add(currentItems.get(i));
        }

        Map<K, Component> active = reconciler.reconcile(visibleItems, keyExtractor, itemFactory);

        content.clearChildren();
        float contentW = content.getWidth() > 0 ? content.getWidth() : (pane != null ? pane.getWidth() : 300f);

        for (int i = start; i <= end; i++) {
            T item = currentItems.get(i);
            K key = keyExtractor.apply(item);
            Component comp = active.get(key);
            if (comp != null) {
                Element el = comp.element();
                float itemH = itemHeights[i];
                float itemY = totalHeight - yOffsets[i] - itemH;
                el.setBounds(0, itemY, contentW, itemH);
                el.validate();
                content.addChild(el);
            }
        }
    }

    public static int findFirstVisible(float[] yOffsets, float[] heights, float scrollY) {
        if (yOffsets.length == 0) return 0;
        int low = 0;
        int high = yOffsets.length - 1;
        int result = high;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (yOffsets[mid] + heights[mid] > scrollY) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    public static int findLastVisible(float[] yOffsets, float bottom) {
        if (yOffsets.length == 0) return 0;
        int low = 0;
        int high = yOffsets.length - 1;
        int result = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (yOffsets[mid] < bottom) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private void ensureUpdateHook() {
        if (!updateHooked && pane != null) {
            updateHooked = true;
            pane.update(() -> {
                float scrollY = pane.getVisualScrollY();
                if (Math.abs(scrollY - lastScrollY) > 0.5f) {
                    lastScrollY = scrollY;
                    reconcileVisible();
                }
                checkScrollBoundary(pane.getScrollY(), pane.getMaxY());
            });
        }
    }

    private void checkScrollBoundary(float scrollY, float maxY) {
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

    @Override
    protected void onDispose() {
        reconciler.dispose();
        content.clearChildren();
    }

    public class VirtualContainer extends WidgetGroup {
        private boolean inLayout = false;

        @Override
        public float getPrefWidth() {
            return 0f;
        }

        @Override
        public float getPrefHeight() {
            return totalHeight;
        }

        @Override
        public void layout() {
            if (inLayout) {
                super.layout();
                return;
            }
            inLayout = true;
            try {
                float curWidth = getWidth();
                if (curWidth > 0 && Math.abs(curWidth - lastMeasuredWidth) > 1f) {
                    recalculateHeights(curWidth);
                    reconcileVisible();
                }
                super.layout();
            } finally {
                inLayout = false;
            }
        }
    }
}
