package solim.ui;

import arc.func.Cons;
import arc.func.Func;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.EventsUtil;
import solim.display.Badge;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.graphics.ColoredDrawable;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Direction;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.ReactiveGrid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Tabs;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.runtime.ParentStack;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;
import arc.graphics.Color;

/** Declarative UI facades for Solim. */
public final class Ui {
    private Ui() {
    }

    private static final float BASE_UNIT = 4;

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

    public static SolimStack stack(@Nullable Runnable r) {
        return stack().children(r);
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

    public static Row wrap() {
        return row();
    }

    public static Row wrap(@Nullable Runnable r) {
        return row().children(r);
    }

    public static Scroll scroll() {
        return new Scroll();
    }

    public static Scroll scroll(@Nullable Runnable r) {
        return scroll().children(r);
    }

    public static boolean isExpanding(Element child) {
        if (child == null)
            return false;
        if ("expanding".equals(child.userObject) || Boolean.TRUE.equals(child.userObject))
            return true;
        if ("solim-spacer-table".equals(child.name) || "spacer".equals(child.name))
            return true;
        if (child.fillParent)
            return true;
        if (child instanceof ScrollPane)
            return true;
        if (child instanceof Table) {
            Table t = (Table) child;
            if (t.getChildren().size > 0 && t.getChildren().first() instanceof ScrollPane) {
                return true;
            }
        }
        return false;
    }

    public static Column container() {
        return column();
    }

    public static Column container(@Nullable Runnable r) {
        return column().children(r);
    }

    public static Divider divider() {
        return divider(Direction.X);
    }

    public static Divider divider(Direction direction) {
        Divider d = new Divider(direction);
        ParentStack.attachToParent(d.element());
        return d;
    }

    public static Divider divider(String direction) {
        if (direction != null && direction.equalsIgnoreCase("y")) {
            return divider(Direction.Y);
        }
        return divider(Direction.X);
    }

    public static Divider divider(char direction) {
        if (direction == 'y' || direction == 'Y') {
            return divider(Direction.Y);
        }
        return divider(Direction.X);
    }

    public static Element spacer() {
        Spacer s = new Spacer();
        ParentStack.attachToParent(s.element());
        return s.element();
    }

    public static SolimImage image() {
        return image((Drawable) null);
    }

    public static SolimImage image(Drawable drawable) {
        SolimImage img = new SolimImage(drawable);
        ParentStack.attachToParent(img.element());
        return img;
    }

    public static SolimImage icon() {
        return image();
    }

    public static SolimImage icon(Drawable drawable) {
        return image(drawable);
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

    public static <T extends Element> T element(T el) {
        if (el != null) {
            ParentStack.attachToParent(el);
        }
        return el;
    }

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
        b.children(() -> text(text));
        return b;
    }

    public static Button button(Readable<String> text, @Nullable Runnable onClick) {
        Button b = button(onClick);
        b.children(() -> text(text));
        return b;
    }

    public static Button button(String text, Drawable icon, @Nullable Runnable onClick) {
        Button b = button(onClick);
        b.children(() -> {
            if (icon != null) {
                image(icon);
            }
            if (text != null) {
                text(text);
            }
        });
        return b;
    }

    public static Text text(String s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
    }

    public static Text text(Signal<String> s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
    }

    public static Text text(Computed<String> s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
    }

    public static Text text(Readable<String> s) {
        Text t = Text.of(s);
        ParentStack.attachToParent(t.label());
        return t;
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

    public static Checkbox checkbox(String label, boolean initial, Consumer<Boolean> onChanged) {
        Checkbox cb = Checkbox.of(label, initial, onChanged);
        ParentStack.attachToParent(cb.checkBox());
        return cb;
    }

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

    public static Hud hud(@Nullable Cons<Hud> content) {
        Hud h = hud();
        h.children(() -> {
            if (content != null) {
                content.get(h);
            }
        });
        return h;
    }

    public static <T> Dynamic<T> dynamic(Readable<T> source, Function<T, Component> factory) {
        Dynamic<T> d = Dynamic.of(source, factory);
        ParentStack.attachToParent(d.element());
        return d;
    }

    public static <T, K> ForEach<T, K> forEach(
            Readable<? extends Iterable<T>> collection,
            Function<T, K> keyExtractor,
            Function<T, Component> itemFactory) {
        ForEach<T, K> fe = ForEach.of(collection, keyExtractor, itemFactory);
        ParentStack.attachToParent(fe.element());
        return fe;
    }

    public static <T, K> ReactiveGrid<T, K> grid(
            Readable<Integer> columnCount,
            Readable<? extends Iterable<T>> items,
            Function<T, K> keyExtractor,
            Function<T, Component> itemFactory) {
        ReactiveGrid<T, K> grid = ReactiveGrid.of(columnCount, items, keyExtractor, itemFactory);
        ParentStack.attachToParent(grid.element());
        return grid;
    }

    public static <T> Disposable listen(Class<T> type, Cons<T> listener) {
        return EventsUtil.listen(type, listener);
    }

    public static <E, T> Signal<T> createSignal(Class<E> eventType, Supplier<T> supplier) {
        return EventsUtil.createSignal(eventType, supplier);
    }

    public static <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
        return EventsUtil.createSignal(eventType, mapper, initial);
    }

    public static <T> Signal<T> createSignal(Function<Runnable, Disposable> callbackRegistrar, Supplier<T> supplier) {
        return EventsUtil.createSignal(callbackRegistrar, supplier);
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

    public static Tabs tabs(Signal<Integer> activeTab) {
        Tabs t = new Tabs(activeTab);
        ParentStack.attachToParent(t.element());
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

    public static Badge badgeCount(Readable<Integer> count) {
        Badge b = Badge.ofCount(count);
        ParentStack.attachToParent(b.element());
        return b;
    }

    public static float unit(float value) {
        return value * BASE_UNIT;
    }

    public static ColoredDrawable colored(Color color, Drawable drawable) {
        return ColoredDrawable.of(color, drawable);
    }

    public static Element add(Object child) {
        return ParentStack.add(child);
    }
}
