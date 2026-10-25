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
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
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
import solim.display.ImageSource;
import solim.display.SolimImage;
import solim.display.Text;
import solim.display.video.SolimVideo;
import solim.display.video.VideoSource;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSelect;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.BlurContainer;
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
import solim.core.HostCapability;
import solim.modifier.ButtonCapability;
import solim.modifier.CheckableCapability;
import solim.modifier.ElementCapability;
import solim.modifier.FieldCapability;
import solim.modifier.ImageCapability;
import solim.modifier.LabelCapability;
import solim.modifier.Prop;
import solim.modifier.ScrollCapability;
import solim.modifier.SelectCapability;
import solim.modifier.SliderCapability;
import solim.modifier.TableCapability;
import solim.modifier.Values;
import solim.modifier.VideoCapability;
import solim.modifier.WrappableCapability;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;
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

    public static BlurContainer blurContainer() {
        return new BlurContainer();
    }

    public static BlurContainer blurContainer(float contentBlurRadius) {
        return new BlurContainer(contentBlurRadius);
    }

    public static BlurContainer blurContainer(@Nullable Runnable r) {
        return blurContainer().children(r);
    }

    public static BlurContainer blurContainer(float contentBlurRadius, @Nullable Runnable r) {
        return blurContainer(contentBlurRadius).children(r);
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
        return new Grid().props(Values.columns(columns));
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
        return new SolimCollapser().props(Values.expanded(expanded));
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

    public static SolimImage image(@Nullable String url) {
        return new SolimImage(url);
    }

    public static SolimImage image(@Nullable ImageSource source) {
        return new SolimImage(source);
    }

    public static SolimImage image(Readable<Drawable> drawable) {
        return new SolimImage(drawable.map(ImageSource::of));
    }

    public static SolimImage icon(Drawable drawable) {
        float iconSize = unit(6);
        return image(Drawables.scalable(drawable))
                .props(Values.scaling(Scaling.fit), Values.size(iconSize), Values.prefWidth(iconSize), Values.prefHeight(iconSize));
    }

    public static SolimImage icon(Readable<Drawable> drawable) {
        float iconSize = unit(6);
        return image(drawable != null ? drawable.map(Drawables::scalable) : null)
                .props(Values.scaling(Scaling.fit), Values.size(iconSize), Values.prefWidth(iconSize), Values.prefHeight(iconSize));
    }

    public static SolimVideo video() {
        return new SolimVideo();
    }

    public static SolimVideo video(@Nullable String url) {
        return new SolimVideo(url);
    }

    public static SolimVideo video(@Nullable VideoSource source) {
        return new SolimVideo(source);
    }

    public static SolimVideo video(@Nullable Readable<String> url) {
        return url == null ? new SolimVideo() : new SolimVideo(url.map(u -> VideoSource.of(u)));
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

    public static RoundedDrawable roundedDrawable(int radius) {
        return new RoundedDrawable(radius);
    }

    public static RoundedDrawable roundedDrawable(int radius, Color color) {
        return RoundedDrawable.of(radius, color);
    }

    public static RoundedDrawable roundedDrawable(int radius, Color color, float stroke, Color borderColor) {
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

    public static Prop<ElementCapability, Boolean> grow() {
        return Values.grow();
    }

    public static Prop<ElementCapability, Boolean> growX() {
        return Values.growX();
    }

    public static Prop<ElementCapability, Boolean> growY() {
        return Values.growY();
    }

    public static Prop<ElementCapability, Void> center() {
        return Values.center();
    }

    public static Prop<ElementCapability, Void> top() {
        return Values.top();
    }

    public static Prop<ElementCapability, Void> bottom() {
        return Values.bottom();
    }

    public static Prop<ElementCapability, Void> left() {
        return Values.left();
    }

    public static Prop<ElementCapability, Void> right() {
        return Values.right();
    }

    public static Prop<ElementCapability, Integer> align(int align) {
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

    public static Prop<ElementCapability, Float> prefWidth(float v) {
        return Values.prefWidth(v);
    }

    public static Prop<ElementCapability, Float> prefWidth(Readable<Float> v) {
        return Values.prefWidth(v);
    }

    public static Prop<ElementCapability, Float> prefHeight(float v) {
        return Values.prefHeight(v);
    }

    public static Prop<ElementCapability, Float> prefHeight(Readable<Float> v) {
        return Values.prefHeight(v);
    }

    public static Prop<ElementCapability, Float> minWidth(float v) {
        return Values.minWidth(v);
    }

    public static Prop<ElementCapability, Float> minWidth(Readable<Float> v) {
        return Values.minWidth(v);
    }

    public static Prop<ElementCapability, Float> minHeight(float v) {
        return Values.minHeight(v);
    }

    public static Prop<ElementCapability, Float> minHeight(Readable<Float> v) {
        return Values.minHeight(v);
    }

    public static Prop<ElementCapability, Float> maxWidth(float v) {
        return Values.maxWidth(v);
    }

    public static Prop<ElementCapability, Float> maxWidth(Readable<Float> v) {
        return Values.maxWidth(v);
    }

    public static Prop<ElementCapability, Float> maxHeight(float v) {
        return Values.maxHeight(v);
    }

    public static Prop<ElementCapability, Float> maxHeight(Readable<Float> v) {
        return Values.maxHeight(v);
    }

    public static Prop<ElementCapability, Float> margin(float v) {
        return Values.margin(v);
    }

    public static Prop<ElementCapability, Float> margin(float top, float left, float bottom, float right) {
        return Values.margin(top, left, bottom, right);
    }

    public static Prop<ElementCapability, Float> margin(Readable<Float> v) {
        return Values.margin(v);
    }

    public static Prop<ElementCapability, Float> margin(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        return Values.margin(top, left, bottom, right);
    }

    public static Prop<ElementCapability, Float> marginTop(float v) {
        return Values.marginTop(v);
    }

    public static Prop<ElementCapability, Float> marginTop(Readable<Float> v) {
        return Values.marginTop(v);
    }

    public static Prop<ElementCapability, Float> marginBottom(float v) {
        return Values.marginBottom(v);
    }

    public static Prop<ElementCapability, Float> marginBottom(Readable<Float> v) {
        return Values.marginBottom(v);
    }

    public static Prop<ElementCapability, Float> marginLeft(float v) {
        return Values.marginLeft(v);
    }

    public static Prop<ElementCapability, Float> marginLeft(Readable<Float> v) {
        return Values.marginLeft(v);
    }

    public static Prop<ElementCapability, Float> marginRight(float v) {
        return Values.marginRight(v);
    }

    public static Prop<ElementCapability, Float> marginRight(Readable<Float> v) {
        return Values.marginRight(v);
    }

    public static Prop<ElementCapability, Float> marginX(float v) {
        return Values.marginX(v);
    }

    public static Prop<ElementCapability, Float> marginX(@Nullable Readable<Float> v) {
        return Values.marginX(v);
    }

    public static Prop<ElementCapability, Float> marginY(float v) {
        return Values.marginY(v);
    }

    public static Prop<ElementCapability, Float> marginY(@Nullable Readable<Float> v) {
        return Values.marginY(v);
    }

    public static Prop<TableCapability, Float> padding(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
        return Values.padding(top, left, bottom, right);
    }

    public static Prop<TableCapability, Float> paddingTop(float v) {
        return Values.paddingTop(v);
    }

    public static Prop<TableCapability, Float> paddingTop(Readable<Float> v) {
        return Values.paddingTop(v);
    }

    public static Prop<TableCapability, Float> paddingBottom(float v) {
        return Values.paddingBottom(v);
    }

    public static Prop<TableCapability, Float> paddingBottom(Readable<Float> v) {
        return Values.paddingBottom(v);
    }

    public static Prop<TableCapability, Float> paddingLeft(float v) {
        return Values.paddingLeft(v);
    }

    public static Prop<TableCapability, Float> paddingLeft(Readable<Float> v) {
        return Values.paddingLeft(v);
    }

    public static Prop<TableCapability, Float> paddingRight(float v) {
        return Values.paddingRight(v);
    }

    public static Prop<TableCapability, Float> paddingRight(Readable<Float> v) {
        return Values.paddingRight(v);
    }

    public static Prop<TableCapability, Integer> rounded(int radius) {
        return Values.rounded(radius);
    }

    public static Prop<TableCapability, Integer> rounded(int radius, @Nullable Color color) {
        return Values.rounded(radius, color);
    }

    public static Prop<TableCapability, Color> rounded(int radius, @Nullable Readable<Color> color) {
        return Values.rounded(radius, color);
    }

    public static Prop<TableCapability, Float> border(float stroke, @Nullable Color color) {
        return Values.border(stroke, color);
    }

    public static Prop<TableCapability, Color> border(float stroke, @Nullable Readable<Color> color) {
        return Values.border(stroke, color);
    }

    public static Prop<TableCapability, Drawable> background(@Nullable Drawable drawable) {
        return Values.background(drawable);
    }

    public static Prop<TableCapability, Drawable> background(@Nullable Readable<Drawable> drawable) {
        return Values.background(drawable);
    }

    public static Prop<TableCapability, Color> background(@Nullable Color color) {
        return Values.background(color);
    }

    public static Prop<TableCapability, Color> backgroundColor(@Nullable Color color) {
        return Values.backgroundColor(color);
    }

    public static Prop<TableCapability, Float> backdropBlur(float radius) {
        return Values.backdropBlur(radius);
    }

    public static Prop<TableCapability, Float> backdropBlur() {
        return Values.backdropBlur();
    }

    public static Prop<TableCapability, Float> contentBlur(float radius) {
        return Values.contentBlur(radius);
    }

    public static Prop<TableCapability, Float> contentBlur() {
        return Values.contentBlur();
    }

    public static Prop<TableCapability, Readable<Color>> backgroundColor(@Nullable Readable<Color> color) {
        return Values.backgroundColor(color);
    }

    public static Prop<ElementCapability, Float> width(float v) {
        return Values.width(v);
    }

    public static Prop<ElementCapability, Float> width(Readable<Float> v) {
        return Values.width(v);
    }

    public static Prop<ElementCapability, Float> height(float v) {
        return Values.height(v);
    }

    public static Prop<ElementCapability, Float> height(Readable<Float> v) {
        return Values.height(v);
    }

    public static Prop<ElementCapability, Float> size(float v) {
        return Values.size(v);
    }

    public static Prop<ElementCapability, Float> size(float width, float height) {
        return Values.size(width, height);
    }

    public static Prop<ElementCapability, Float> size(@Nullable Readable<Float> size) {
        return Values.size(size);
    }

    public static Prop<ElementCapability, Float> size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        return Values.size(width, height);
    }

    public static Prop<ElementCapability, Float> x(float v) {
        return Values.x(v);
    }

    public static Prop<ElementCapability, Float> x(Readable<Float> source) {
        return Values.x(source);
    }

    public static Prop<ElementCapability, Float> y(float v) {
        return Values.y(v);
    }

    public static Prop<ElementCapability, Float> y(Readable<Float> source) {
        return Values.y(source);
    }

    public static Prop<ElementCapability, Void> position(float x, float y) {
        return Values.position(x, y);
    }

    public static Prop<ElementCapability, Float> position(@Nullable Readable<Float> x, @Nullable Readable<Float> y) {
        return Values.position(x, y);
    }

    public static Prop<ElementCapability, Boolean> visible(boolean visible) {
        return Values.visible(visible);
    }

    public static Prop<ElementCapability, Boolean> visible(Readable<Boolean> visible) {
        return Values.visible(visible);
    }

    public static Prop<ElementCapability, Touchable> touchable(Touchable touchable) {
        return Values.touchable(touchable);
    }

    public static Prop<ElementCapability, Touchable> touchable(Readable<Touchable> touchable) {
        return Values.touchable(touchable);
    }

    public static Prop<ElementCapability, Float> opacity(float opacity) {
        return Values.opacity(opacity);
    }

    public static Prop<ElementCapability, Float> opacity(Readable<Float> source) {
        return Values.opacity(source);
    }

    public static Prop<ElementCapability, Float> alpha(float alpha) {
        return Values.alpha(alpha);
    }

    public static Prop<ElementCapability, Float> alpha(Readable<Float> source) {
        return Values.alpha(source);
    }

    public static Prop<ElementCapability, Float> scale(float scale) {
        return Values.scale(scale);
    }

    public static Prop<ElementCapability, Float> scale(Readable<Float> source) {
        return Values.scale(source);
    }

    public static Prop<ElementCapability, String> name(String name) {
        return Values.name(name);
    }

    public static Prop<ElementCapability, Runnable> onClick(Runnable action) {
        return Values.onClick(action);
    }

    public static Prop<ElementCapability, Boolean> stopClickPropagation(boolean stop) {
        return Values.stopClickPropagation(stop);
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable String tip) {
        return Values.tooltip(tip);
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable Readable<String> tip) {
        return Values.tooltip(tip);
    }

    public static Prop<ElementCapability, Void> tooltip(@Nullable Cons<Table> builder) {
        return Values.tooltip(builder);
    }

    public static Prop<ElementCapability, Void> draggable() {
        return Values.draggable();
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Values.draggable(x, y);
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud) {
        return Values.draggable(hud);
    }

    public static Prop<ElementCapability, Void> draggable(@Nullable Hud hud, @Nullable Signal<Float> x, @Nullable Signal<Float> y) {
        return Values.draggable(hud, x, y);
    }

    public static Prop<ElementCapability, Cons<Element>> update(Cons<Element> fn) {
        return Values.update(fn);
    }

    public static Prop<ElementCapability, Integer> origin(int align) {
        return Values.origin(align);
    }

    public static Prop<ElementCapability, Float> rotation(float degree) {
        return Values.rotation(degree);
    }

    public static Prop<ElementCapability, Color> color(@Nullable Color value) {
        return Values.color(value);
    }

    public static Prop<ElementCapability, Color> color(@Nullable Readable<Color> source) {
        return Values.color(source);
    }

    public static Prop<ButtonCapability, Cons<SolimButtonStyleBuilder>> buttonStyle(@Nullable Cons<SolimButtonStyleBuilder> config) {
        return Values.buttonStyle(config);
    }

    public static Prop<LabelCapability, LabelStyle> style(@Nullable LabelStyle value) {
        return Values.style(value);
    }

    public static Prop<ButtonCapability, SolimButtonStyle> style(@Nullable SolimButtonStyle value) {
        return Values.style(value);
    }

    public static Prop<ButtonCapability, Void> style(@Nullable Cons<SolimButtonStyleBuilder> config) {
        return Values.style(config);
    }

    public static Prop<ButtonCapability, ButtonStyle> style(@Nullable ButtonStyle value) {
        return Values.style(value);
    }

    public static Prop<FieldCapability, TextFieldStyle> style(@Nullable TextFieldStyle value) {
        return Values.style(value);
    }

    public static Prop<FieldCapability, InputStyle> style(@Nullable InputStyle value) {
        return Values.style(value);
    }

    public static Prop<ScrollCapability, ScrollPaneStyle> style(@Nullable ScrollPaneStyle value) {
        return Values.style(value);
    }

    public static Prop<SliderCapability, SliderStyle> style(@Nullable SliderStyle value) {
        return Values.style(value);
    }

    public static <S> Prop<ElementCapability, S> style(@Nullable Readable<S> source) {
        return Values.style(source);
    }

    public static Prop<CheckableCapability, Boolean> checked(@Nullable Signal<Boolean> signal) {
        return Values.checked(signal);
    }

    public static Prop<CheckableCapability, Boolean> checked(@Nullable Readable<Boolean> source) {
        return Values.checked(source);
    }

    public static Prop<CheckableCapability, Boolean> checked(boolean value) {
        return Values.checked(value);
    }

    public static Prop<SliderCapability, Float> value(@Nullable Signal<Float> signal) {
        return Values.value(signal);
    }

    public static Prop<SliderCapability, Float> value(float val) {
        return Values.value(val);
    }

    public static Prop<SliderCapability, Float> value(@Nullable Readable<Float> source) {
        return Values.value(source);
    }

    public static <T> Prop<SelectCapability, T> selected(@Nullable Signal<T> signal) {
        return Values.selected(signal);
    }

    public static <T> Prop<SelectCapability, T> selected(@Nullable Readable<T> source) {
        return Values.selected(source);
    }

    public static Prop<TableCapability, Boolean> fillParent(boolean fill) {
        return Values.fillParent(fill);
    }

    public static Prop<TableCapability, Boolean> fillParent() {
        return Values.fillParent();
    }

    public static Prop<TableCapability, Integer> columns(int count) {
        return Values.columns(count);
    }

    public static Prop<TableCapability, Integer> columns(@Nullable Readable<Integer> count) {
        return Values.columns(count);
    }

    public static Prop<TableCapability, Integer> activeTab(@Nullable Signal<Integer> signal) {
        return Values.activeTab(signal);
    }

    public static Prop<TableCapability, Integer> activeTab(int tab) {
        return Values.activeTab(tab);
    }

    public static Prop<TableCapability, Integer> activeTab(@Nullable Readable<Integer> source) {
        return Values.activeTab(source);
    }

    public static Prop<TableCapability, Float> headerGap(float gap) {
        return Values.headerGap(gap);
    }

    public static Prop<TableCapability, ButtonStyle> tabStyle(@Nullable ButtonStyle style) {
        return Values.tabStyle(style);
    }

    public static Prop<TableCapability, Float> progress(@Nullable Readable<Float> source) {
        return Values.progress(source);
    }

    public static Prop<TableCapability, Float> progress(float val) {
        return Values.progress(val);
    }

    public static Prop<TableCapability, Boolean> hideOnZero(boolean hide) {
        return Values.hideOnZero(hide);
    }

    public static Prop<TableCapability, Boolean> hideOnZero() {
        return Values.hideOnZero();
    }

    public static Prop<FieldCapability, String> placeholder(@Nullable String text) {
        return Values.placeholder(text);
    }

    public static Prop<FieldCapability, String> placeholder(@Nullable Readable<String> source) {
        return Values.placeholder(source);
    }

    public static Prop<ImageCapability, Drawable> placeholder(@Nullable Drawable value) {
        return Values.placeholder(value);
    }

    public static Prop<FieldCapability, Integer> maxLength(int length) {
        return Values.maxLength(length);
    }

    public static Prop<ScrollCapability, Void> scrollingDisabled(boolean disableX, boolean disableY) {
        return Values.scrollingDisabled(disableX, disableY);
    }

    public static Prop<ScrollCapability, Boolean> scrollX(boolean scrollX) {
        return Values.scrollX(scrollX);
    }

    public static Prop<ScrollCapability, Boolean> scrollY(boolean scrollY) {
        return Values.scrollY(scrollY);
    }

    public static Prop<TableCapability, Popup.Edge> pinnedEdge(@Nullable Popup.Edge edge) {
        return Values.pinnedEdge(edge);
    }

    public static Prop<TableCapability, Color> textColor(@Nullable Color color) {
        return Values.textColor(color);
    }

    public static Prop<TableCapability, Color> textColor(@Nullable Readable<Color> source) {
        return Values.textColor(source);
    }

    public static Prop<TableCapability, Boolean> collapsed(@Nullable Signal<Boolean> signal) {
        return Values.collapsed(signal);
    }

    public static Prop<TableCapability, Boolean> collapsed(@Nullable Readable<Boolean> source) {
        return Values.collapsed(source);
    }

    public static Prop<TableCapability, Boolean> collapsed(boolean val) {
        return Values.collapsed(val);
    }

    public static Prop<TableCapability, Boolean> expanded(@Nullable Signal<Boolean> signal) {
        return Values.expanded(signal);
    }

    public static Prop<TableCapability, Boolean> expanded(@Nullable Readable<Boolean> source) {
        return Values.expanded(source);
    }

    public static Prop<TableCapability, Boolean> expanded(boolean val) {
        return Values.expanded(val);
    }

    public static Prop<TableCapability, Float> duration(float seconds) {
        return Values.duration(seconds);
    }

    public static Prop<TableCapability, Boolean> animated(boolean animated) {
        return Values.animated(animated);
    }

    public static Prop<VideoCapability, Boolean> loop(boolean loop) {
        return Values.loop(loop);
    }

    public static Prop<VideoCapability, Boolean> loop(@Nullable Readable<Boolean> source) {
        return Values.loop(source);
    }

    public static Prop<VideoCapability, Float> volume(float volume) {
        return Values.volume(volume);
    }

    public static Prop<VideoCapability, Float> volume(@Nullable Readable<Float> source) {
        return Values.volume(source);
    }

    public static Prop<VideoCapability, Boolean> muted(@Nullable Signal<Boolean> signal) {
        return Values.muted(signal);
    }

    public static Prop<VideoCapability, Boolean> muted(@Nullable Readable<Boolean> source) {
        return Values.muted(source);
    }

    public static Prop<VideoCapability, Boolean> muted(boolean val) {
        return Values.muted(val);
    }

    public static Prop<VideoCapability, Boolean> controls(boolean controls) {
        return Values.controls(controls);
    }

    public static Prop<VideoCapability, Boolean> controls(@Nullable Readable<Boolean> source) {
        return Values.controls(source);
    }

    public static Prop<VideoCapability, Drawable> playIcon(@Nullable Drawable icon) {
        return Values.playIcon(icon);
    }

    public static Prop<VideoCapability, Drawable> pauseIcon(@Nullable Drawable icon) {
        return Values.pauseIcon(icon);
    }

    public static Prop<VideoCapability, Drawable> replayIcon(@Nullable Drawable icon) {
        return Values.replayIcon(icon);
    }

    public static Prop<VideoCapability, Drawable> volumeIcon(@Nullable Drawable icon) {
        return Values.volumeIcon(icon);
    }

    public static Prop<VideoCapability, Drawable> muteIcon(@Nullable Drawable icon) {
        return Values.muteIcon(icon);
    }

    public static Prop<ImageCapability, Scaling> scaling(@Nullable Scaling scaling) {
        return Values.scaling(scaling);
    }

    public static Prop<ElementCapability, Boolean> enabled(boolean value) {
        return Values.enabled(value);
    }

    public static Prop<ElementCapability, Boolean> enabled(@Nullable Readable<Boolean> source) {
        return Values.enabled(source);
    }

    public static Prop<ElementCapability, Boolean> disabled(boolean value) {
        return Values.disabled(value);
    }

    public static Prop<ElementCapability, Boolean> disabled(@Nullable Readable<Boolean> source) {
        return Values.disabled(source);
    }

    public static Prop<WrappableCapability, Boolean> textWrap(boolean value) {
        return Values.wrap(value);
    }

    public static Prop<WrappableCapability, Boolean> textWrap() {
        return Values.wrap();
    }

    public static Prop<WrappableCapability, Boolean> ellipsis(boolean value) {
        return Values.ellipsis(value);
    }

    public static Prop<WrappableCapability, Boolean> ellipsis() {
        return Values.ellipsis();
    }

    public static Prop<WrappableCapability, Float> fontScale(float value) {
        return Values.fontScale(value);
    }

    public static Prop<WrappableCapability, Float> fontScale(@Nullable Readable<Float> source) {
        return Values.fontScale(source);
    }

    public static Prop<ImageCapability, String> src(@Nullable String value) {
        return Values.src(value);
    }

    public static Prop<ImageCapability, ImageSource> src(@Nullable ImageSource value) {
        return Values.src(value);
    }

    public static Prop<VideoCapability, VideoSource> src(@Nullable VideoSource value) {
        return Values.src(value);
    }

    public static <T> Prop<ImageCapability, T> src(@Nullable Readable<T> source) {
        return Values.src(source);
    }

    public static Prop<ImageCapability, Drawable> fallback(@Nullable Drawable value) {
        return Values.fallback(value);
    }

    public static Prop<VideoCapability, Boolean> autoplay(boolean value) {
        return Values.autoplay(value);
    }

    public static Prop<ImageCapability, Integer> imageRounded(int radius) {
        return Values.imageRounded(radius);
    }

    public static <Req extends HostCapability> Prop<Req, Void> raw(Class<Req> requires, String label, Prop.Kind kind, Cons<Object> mutation) {
        return Values.raw(requires, label, kind, mutation);
    }
}
