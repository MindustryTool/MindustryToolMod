package solim;

import arc.graphics.Color;
import arc.func.Cons;
import solim.graphics.CircleDrawable;
import solim.graphics.ColoredDrawable;
import solim.graphics.Drawables;
import solim.graphics.RoundedDrawable;
import arc.func.Func;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import arc.util.Scaling;

import java.util.List;
import java.util.Map;
import arc.func.Prov;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.EventsUtil;
import solim.display.Badge;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSelect;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Direction;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.Wrap;
import solim.layout.ReactiveGrid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimCollapser;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Tabs;
import solim.layout.VirtualList;
import solim.layout.ItemHeightProvider;
import solim.overlay.Hud;
import solim.overlay.Popup;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.MapSignal;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signals;
import solim.reactive.Dynamic;
import solim.reactive.ForEach;
import solim.reactive.When;
import solim.reactive.Mutation;
import solim.reactive.Query;
import solim.reactive.QueryKey;
import solim.reactive.QueryView;
import solim.core.Units;
import java.util.concurrent.CompletableFuture;

/**
 * Public entry point and declarative UI facade for Solim. This class exposes
 * all allowed factory and utility methods for user-facing and mod development.
 */
public final class UI {
    static {
        init();
    }

    private UI() {
    }

    /**
     * Initializes Solim runtime hooks, registering the single-frame signal
     * dispatcher with Mindustry. This method is idempotent and safe to call
     * repeatedly.
     */
    public static void init() {
        SignalDispatcher.register();
    }

    // --- Layout ---

    public static Column column() {
        return new Column();
    }

    public static Column column(@Nullable Runnable r) {
        return column().children(r);
    }

    public static Card card() {
        return new Card();
    }

    public static Card card(@Nullable Runnable r) {
        return card().children(r);
    }

    public static Card card(@Nullable Drawable background) {
        return new Card(background);
    }

    public static Card card(@Nullable Drawable background, @Nullable Runnable r) {
        return card(background).children(r);
    }

    public static Row row() {
        return new Row();
    }

    public static Row row(@Nullable Runnable r) {
        return row().children(r);
    }

    public static SolimStack stack() {
        SolimStack s = new SolimStack();
        ParentStack.attachToParent(s.element());
        return s;
    }

    public static Grid grid() {
        return new Grid();
    }

    public static Grid grid(@Nullable Runnable r) {
        return grid().children(r);
    }

    public static Grid grid(int columns) {
        return new Grid(columns);
    }

    public static Grid grid(int columns, @Nullable Runnable r) {
        return grid(columns).children(r);
    }

    public static Grid grid(Readable<Integer> columns) {
        Grid g = new Grid().columns(columns);
        ParentStack.attachToParent(g.element());
        return g;
    }

    public static Grid grid(Readable<Integer> columns, @Nullable Runnable r) {
        return grid(columns).children(r);
    }

    public static Wrap wrap() {
        return new Wrap();
    }

    public static Wrap wrap(@Nullable Runnable r) {
        return wrap().children(r);
    }

    public static Scroll scroll() {
        return new Scroll();
    }

    public static Scroll scroll(@Nullable Runnable r) {
        return scroll().children(r);
    }

    public static SolimCollapser collapser() {
        return new SolimCollapser();
    }

    public static SolimCollapser collapser(@Nullable Runnable r) {
        return collapser().children(r);
    }

    public static SolimCollapser collapser(Readable<Boolean> expanded) {
        return new SolimCollapser().expanded(expanded);
    }

    public static SolimCollapser collapser(Readable<Boolean> expanded, @Nullable Runnable r) {
        return collapser(expanded).children(r);
    }

    public static Divider divider() {
        return divider(Direction.X);
    }

    public static Divider divider(Direction direction) {
        Divider d = new Divider(direction);
        ParentStack.attachToParent(d.element());
        return d;
    }

    public static Spacer spacer() {
        Spacer s = new Spacer();
        ParentStack.attachToParent(s.element());
        return s;
    }

    // --- Display ---

    public static SolimImage image() {
        return image((Drawable) null);
    }

    public static SolimImage image(Drawable drawable) {
        SolimImage img = new SolimImage(drawable);
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static SolimImage image(Readable<Drawable> drawable) {
        SolimImage img = new SolimImage();
        img.drawable(drawable);
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static SolimImage icon(Drawable drawable) {
        return image(Drawables.scalable(drawable)).scaling(Scaling.fit).size(unit(6));
    }

    public static SolimImage icon(Readable<Drawable> drawable) {
        return image(drawable != null ? drawable.map(Drawables::scalable) : null).scaling(Scaling.fit).size(unit(6));
    }

    public static NetworkImage networkImage() {
        NetworkImage img = new NetworkImage();
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static NetworkImage networkImage(@Nullable String url) {
        NetworkImage img = new NetworkImage(url);
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static NetworkImage networkImage(@Nullable Readable<String> url) {
        NetworkImage img = new NetworkImage(url);
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static Text text(String s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
    }

    public static Text text(Readable<String> s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
    }

    public static Badge badge(String text) {
        Badge b = new Badge(text);
        ParentStack.attachToParent(b.element());
        return b;
    }

    public static Badge badge(Readable<String> text) {
        Badge b = new Badge(text);
        ParentStack.attachToParent(b.element());
        return b;
    }

    public static Badge badge(int count) {
        Badge b = Badge.ofCount(count);
        ParentStack.attachToParent(b.element());
        return b;
    }

    // --- Input ---

    public static Button button() {
        Button b = new Button();
        ParentStack.attachToParent(b.element());
        return b;
    }

    public static Button button(@Nullable Runnable onClick) {
        Button b = new Button(onClick);
        ParentStack.attachToParent(b.element());
        return b;
    }

    public static Button button(String text, @Nullable Runnable onClick) {
        Button b = button(onClick);
        b.text(text);
        return b;
    }

    public static Button button(Readable<String> text, @Nullable Runnable onClick) {
        Button b = button(onClick);
        b.text(text);
        return b;
    }

    public static SolimTextField textField(Signal<String> signal) {
        SolimTextField tf = SolimTextField.of(signal);
        ParentStack.attachToParent(tf.field());
        return tf;
    }

    public static SolimSlider slider(Signal<Float> signal, float min, float max, float step) {
        SolimSlider s = SolimSlider.of(signal, min, max, step);
        ParentStack.attachToParent(s.slider());
        return s;
    }

    public static SolimSlider slider(Signal<Integer> signal, int min, int max, int step) {
        SolimSlider s = SolimSlider.of(signal, min, max, step);
        ParentStack.attachToParent(s.slider());
        return s;
    }

    public static Checkbox checkbox(String label, Signal<Boolean> signal) {
        Checkbox cb = Checkbox.of(label, signal);
        ParentStack.attachToParent(cb.checkBox());
        return cb;
    }

    public static Checkbox checkbox(String label, boolean initial, Cons<Boolean> onChanged) {
        Checkbox cb = Checkbox.of(label, initial, onChanged);
        ParentStack.attachToParent(cb.checkBox());
        return cb;
    }

    public static Switch switchToggle(Signal<Boolean> signal) {
        Switch sw = Switch.of(signal);
        ParentStack.attachToParent(sw.element());
        return sw;
    }

    public static <T> SolimSelect<T> select(Signal<T> signal, List<T> options) {
        SolimSelect<T> s = SolimSelect.of(signal, options);
        ParentStack.attachToParent(s.selectBox());
        return s;
    }

    // --- Overlay ---

    public static SolimDialog dialog(String title) {
        return new SolimDialog(title);
    }

    public static SolimDialog dialog(String title, @Nullable Runnable content) {
        return dialog(title).children(content);
    }

    public static Hud hud() {
        Hud h = new Hud();
        ParentStack.attachToParent(h.element());
        return h;
    }

    public static Hud hud(@Nullable Runnable content) {
        Hud h = hud();
        h.children(content);
        return h;
    }

    public static <T> Popup<T> popup() {
        return new Popup<>();
    }

    public static <T> Popup<T> popup(Func<T, Component> provider) {
        Popup<T> popup = new Popup<>();
        popup.children(provider);
        return popup;
    }

    // --- Reactivity ---

    public static <T> Signal<T> signal() {
        return Signal.of(null);
    }

    public static <T> Signal<T> signal(T initial) {
        return Signal.of(initial);
    }

    public static <T> Computed<T> computed(Prov<T> compute) {
        return new Computed<>(compute);
    }

    public static <K, V> MapSignal<K, V> mapSignal() {
        return MapSignal.of();
    }

    public static <K, V> MapSignal<K, V> mapSignal(@Nullable Map<K, V> initial) {
        return MapSignal.of(initial);
    }

    public static Disposable effect(Runnable effect) {
        return Effect.of(effect);
    }

    public static <E, T> Signal<T> createSignal(Class<E> eventType, Prov<T> Prov) {
        return EventsUtil.createSignal(eventType, Prov);
    }

    public static <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
        return EventsUtil.createSignal(eventType, mapper, initial);
    }

    public static <T> Signal<T> createSignal(Func<Runnable, Disposable> callbackRegistrar, Prov<T> Prov) {
        return EventsUtil.createSignal(callbackRegistrar, Prov);
    }

    public static Readable<Boolean> isPortrait() {
        return Signals.isPortrait();
    }

    public static <T> Query<T> query(QueryKey key, Prov<CompletableFuture<T>> fetcher) {
        return Query.of(key, fetcher);
    }

    public static <T> Query<T> query(QueryKey key, Readable<Boolean> enabled, Prov<CompletableFuture<T>> fetcher) {
        return Query.of(key, enabled, fetcher);
    }

    /**
     * Creates a stateless query under an anonymous key that cannot be targeted
     * by cache invalidation. Use {@code Query.builder()} for keyed or
     * configured queries.
     */
    public static <T> Query<T> query(Prov<CompletableFuture<T>> fetcher) {
        return Query.noKey(fetcher);
    }

    public static <T> QueryView<T> query(Query<T> query) {
        QueryView<T> qv = QueryView.of(query);
        ParentStack.attachToParent(qv.element());
        return qv;
    }

    public static <T, R> Mutation<T, R> mutation(Func<T, CompletableFuture<R>> mutator) {
        return Mutation.of(mutator);
    }

    public static <R> Mutation<Void, R> mutation(Prov<CompletableFuture<R>> mutator) {
        return Mutation.of(mutator);
    }

    // --- Structural & Dynamic ---

    public static <T> Dynamic<T> dynamic(Readable<T> source, Cons<T> factory) {
        Dynamic<T> d = Dynamic.of(source, factory);
        ParentStack.attachToParent(d.element());
        return d;
    }

    /**
     * Creates a boolean conditional with a fluent chaining API. Declare branches
     * with {@code thenDo()} and {@code elseDo()} using void runnables that
     * create components declaratively. Unlike {@link #dynamic}, the component
     * is mounted lazily so chained branches are installed before the first
     * build; post-mount branch updates remount immediately.
     */
    public static When when(Readable<Boolean> condition) {
        return When.of(condition);
    }

    public static <T> ForEach<T> forEach(Readable<? extends Iterable<T>> collection) {
        ForEach<T> fe = ForEach.of(collection);
        ParentStack.attachToParent(fe.element());
        return fe;
    }

    public static <T> ForEach<T> forEach(Iterable<T> collection) {
        return forEach(Readable.of(collection));
    }

    public static <T> ReactiveGrid<T> reactiveGrid(Readable<? extends Iterable<T>> items) {
        ReactiveGrid<T> grid = ReactiveGrid.of(items);
        ParentStack.attachToParent(grid.element());
        return grid;
    }

    public static <T> ReactiveGrid<T> reactiveGrid(Iterable<T> items) {
        return reactiveGrid(Readable.of(items));
    }

    public static <T> VirtualList<T> virtualList(
            Readable<? extends List<T>> collection,
            ItemHeightProvider<T> heightProvider) {
        VirtualList<T> vl = VirtualList.of(collection, heightProvider);
        ParentStack.attachToParent(vl.element());
        return vl;
    }

    public static <T> VirtualList<T> virtualList(
            List<T> items,
            ItemHeightProvider<T> heightProvider) {
        VirtualList<T> vl = VirtualList.of(items, heightProvider);
        ParentStack.attachToParent(vl.element());
        return vl;
    }

    /**
     * Escape hatch for raw Arc elements with no Solim equivalent (e.g.
     * SchematicImage). Prefer Solim primitives such as text() or image() whenever
     * one exists.
     */
    public static <T extends Element> T arc(@Nullable T el) {
        ParentStack.attachToParent(el);
        return el;
    }

    // --- Units ---

    public static float unit(float value) {
        return Units.unit(value);
    }

    public static int unit(int value) {
        return Units.unit(value);
    }

    public static Computed<Float> dvw(float percentage) {
        return Units.dvw(percentage);
    }

    public static Computed<Float> dvh(float percentage) {
        return Units.dvh(percentage);
    }

    public static Computed<Float> dvw(Readable<Float> percentage) {
        return Units.dvw(percentage);
    }

    public static Computed<Float> dvh(Readable<Float> percentage) {
        return Units.dvh(percentage);
    }

    // --- Events & Components ---

    public static <T> Disposable listen(Class<T> type, Cons<T> listener) {
        return EventsUtil.listen(type, listener);
    }

    public static Tabs tabs(Signal<Integer> activeTab) {
        Tabs t = new Tabs(activeTab);
        ParentStack.attachToParent(t.element());
        return t;
    }

    // --- Rounded & Border Styling ---

    public static RoundedDrawable rounded(int radius) {
        return new RoundedDrawable(radius);
    }

    public static RoundedDrawable rounded(int radius, Color color) {
        return RoundedDrawable.of(radius, color);
    }

    public static RoundedDrawable rounded(int radius, Color color, float stroke, Color borderColor) {
        return RoundedDrawable.of(radius, color, stroke, borderColor);
    }

    public static ColoredDrawable colored(Color color, Drawable drawable) {
        return ColoredDrawable.of(color, drawable);
    }

    public static Drawable circle() {
        return CircleDrawable.INSTANCE;
    }

    public static @Nullable Drawable scalable(@Nullable Drawable drawable) {
        return Drawables.scalable(drawable);
    }
}
