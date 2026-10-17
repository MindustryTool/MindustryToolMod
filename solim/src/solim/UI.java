package solim;

import arc.Events;

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
import solim.core.SolimAssert;
import solim.display.Badge;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.display.video.SolimVideo;
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
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Prop;
import solim.modifier.TableCapability;
import solim.modifier.Values;
import solim.overlay.Hud;
import solim.overlay.Popup;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.MapSignal;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
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

public final class UI {
    static {
        init();
    }

    private UI() {
    }

    public static void init() {
        SolimAssert.setMainThread(Thread.currentThread());
        SignalDispatcher.register();
    }

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
        return new SolimStack();
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
        return new Grid().columns(columns);
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
        return new Divider(direction);
    }

    public static Spacer spacer() {
        return new Spacer();
    }

    public static SolimImage image() {
        return image((Drawable) null);
    }

    public static SolimImage image(Drawable drawable) {
        return new SolimImage(drawable);
    }

    public static SolimImage image(Readable<Drawable> drawable) {
        SolimImage img = new SolimImage();
        img.drawable(drawable);
        return img;
    }

    public static SolimImage icon(Drawable drawable) {
        float iconSize = unit(6);
        return image(Drawables.scalable(drawable)).scaling(Scaling.fit)
                .props(Values.size(iconSize), Values.prefWidth(iconSize), Values.prefHeight(iconSize));
    }

    public static SolimImage icon(Readable<Drawable> drawable) {
        float iconSize = unit(6);
        return image(drawable != null ? drawable.map(Drawables::scalable) : null).scaling(Scaling.fit)
                .props(Values.size(iconSize), Values.prefWidth(iconSize), Values.prefHeight(iconSize));
    }

    public static NetworkImage networkImage() {
        return new NetworkImage();
    }

    public static NetworkImage networkImage(@Nullable String url) {
        return new NetworkImage(url);
    }

    public static NetworkImage networkImage(@Nullable Readable<String> url) {
        return new NetworkImage(url);
    }

    public static SolimVideo video() {
        return new SolimVideo();
    }

    public static SolimVideo video(@Nullable String url) {
        return new SolimVideo(url);
    }

    public static SolimVideo video(@Nullable Readable<String> url) {
        return new SolimVideo(url);
    }

    public static Text text(String s) {
        return Text.of(s);
    }

    public static Text text(Readable<String> s) {
        return Text.of(s);
    }

    public static Badge badge(String text) {
        return new Badge(text);
    }

    public static Badge badge(Readable<String> text) {
        return new Badge(text);
    }

    public static Badge badge(int count) {
        return Badge.ofCount(count);
    }

    public static Button button() {
        return new Button();
    }

    public static Button button(@Nullable Runnable onClick) {
        return new Button(onClick);
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
        return SolimTextField.of(signal);
    }

    public static SolimSlider slider(Signal<Float> signal, float min, float max, float step) {
        return SolimSlider.of(signal, min, max, step);
    }

    public static SolimSlider slider(Signal<Integer> signal, int min, int max, int step) {
        return SolimSlider.of(signal, min, max, step);
    }

    public static Checkbox checkbox(String label, Signal<Boolean> signal) {
        return Checkbox.of(label, signal);
    }

    public static Checkbox checkbox(String label, boolean initial, Cons<Boolean> onChanged) {
        return Checkbox.of(label, initial, onChanged);
    }

    public static Switch switchToggle(Signal<Boolean> signal) {
        return Switch.of(signal);
    }

    public static <T> SolimSelect<T> select(Signal<T> signal, List<T> options) {
        return SolimSelect.of(signal, options);
    }

    public static SolimDialog dialog(String title) {
        return new SolimDialog(title);
    }

    public static SolimDialog dialog(String title, @Nullable Runnable content) {
        return dialog(title).children(content);
    }

    public static Hud hud() {
        return new Hud();
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
        popup.items(provider);
        return popup;
    }

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
        Signal<T> signal = Signal.of(Prov.get());
        Events.on(eventType, e -> signal.set(Prov.get()));
        return signal;
    }

    public static <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
        Signal<T> signal = Signal.of(initial);
        Events.on(eventType, e -> signal.set(mapper.get(e)));
        return signal;
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

    public static <T> Query<T> query(Prov<CompletableFuture<T>> fetcher) {
        return Query.noKey(fetcher);
    }

    public static <T> QueryView<T> query(Query<T> query) {
        return QueryView.of(query);
    }

    public static <T, R> Mutation<T, R> mutation(Func<T, CompletableFuture<R>> mutator) {
        return Mutation.of(mutator);
    }

    public static <R> Mutation<Void, R> mutation(Prov<CompletableFuture<R>> mutator) {
        return Mutation.of(mutator);
    }

    public static <T> Dynamic<T> dynamic(Readable<T> source, Cons<T> factory) {
        Dynamic<T> d = Dynamic.of(source, factory);
        return d;
    }

    public static When when(Readable<Boolean> condition) {
        return When.of(condition);
    }

    public static <T> ForEach<T> forEach(Readable<? extends Iterable<T>> collection) {
        ForEach<T> fe = ForEach.of(collection);
        return fe;
    }

    public static <T> ForEach<T> forEach(Iterable<T> collection) {
        return forEach(Readable.of(collection));
    }

    public static <T> ReactiveGrid<T> reactiveGrid(Readable<? extends Iterable<T>> items) {
        ReactiveGrid<T> grid = ReactiveGrid.of(items);
        return grid;
    }

    public static <T> ReactiveGrid<T> reactiveGrid(Iterable<T> items) {
        return reactiveGrid(Readable.of(items));
    }

    public static <T> VirtualList<T> virtualList(
            Readable<? extends List<T>> collection,
            ItemHeightProvider<T> heightProvider) {
        VirtualList<T> vl = VirtualList.of(collection, heightProvider);
        return vl;
    }

    public static <T> VirtualList<T> virtualList(
            List<T> items,
            ItemHeightProvider<T> heightProvider) {
        VirtualList<T> vl = VirtualList.of(items, heightProvider);
        return vl;
    }

    public static <T extends Element> T arc(@Nullable T el) {
        AttachmentStack.attachToParent(el);
        return el;
    }

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

    public static <T> Disposable listen(Class<T> type, Cons<T> listener) {
        return EventsUtil.listen(type, listener);
    }

    public static Tabs tabs(Signal<Integer> activeTab) {
        return new Tabs(activeTab);
    }

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

    public static Prop<CellCapability, Boolean> grow() {
        return Values.grow();
    }

    public static Prop<CellCapability, Boolean> growX() {
        return Values.growX();
    }

    public static Prop<CellCapability, Boolean> growY() {
        return Values.growY();
    }

    public static Prop<AlignmentCapability, Void> center() {
        return Values.center();
    }

    public static Prop<AlignmentCapability, Void> top() {
        return Values.top();
    }

    public static Prop<AlignmentCapability, Void> bottom() {
        return Values.bottom();
    }

    public static Prop<AlignmentCapability, Void> left() {
        return Values.left();
    }

    public static Prop<AlignmentCapability, Void> right() {
        return Values.right();
    }

    public static Prop<AlignmentCapability, Integer> align(int align) {
        return Values.align(align);
    }

    public static Prop<TableCapability, Float> gap(float v) {
        return Values.gap(v);
    }

    public static Prop<TableCapability, Float> gap(Readable<Float> v) {
        return Values.gap(v);
    }

    public static Prop<TableCapability, Float> padding(float p) {
        return Values.padding(p);
    }

    public static Prop<TableCapability, Float> padding(Readable<Float> p) {
        return Values.padding(p);
    }

    public static Prop<TableCapability, Void> padding(float top, float left, float bottom, float right) {
        return Values.padding(top, left, bottom, right);
    }

    public static Prop<TableCapability, Float> paddingX(float v) {
        return Values.paddingX(v);
    }

    public static Prop<TableCapability, Float> paddingX(@Nullable Readable<Float> x) {
        return Values.paddingX(x);
    }

    public static Prop<TableCapability, Float> paddingY(float v) {
        return Values.paddingY(v);
    }

    public static Prop<TableCapability, Float> paddingY(@Nullable Readable<Float> y) {
        return Values.paddingY(y);
    }
}
