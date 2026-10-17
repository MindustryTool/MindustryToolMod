package solim.modifier;

import arc.func.Cons;
import arc.func.Cons2;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.HostCapability;
import solim.reactive.Readable;

public final class Prop<Requires extends HostCapability, V> {

    public enum Kind {
        ELEMENT,
        TABLE,
        CELL
    }

    private final Kind kind;
    private final String label;
    private final Class<? extends HostCapability> requiredCapability;
    private final Cons2<HostView, V> applier;
    private final @Nullable V value;
    private final @Nullable Readable<V> source;
    private final boolean valueless;

    public Prop(Kind kind, Class<? extends HostCapability> requiredCapability, String label,
            Cons2<HostView, V> applier, @Nullable V value, @Nullable Readable<V> source, boolean valueless) {
        this.kind = kind;
        this.requiredCapability = requiredCapability;
        this.label = label;
        this.applier = applier;
        this.value = value;
        this.source = source;
        this.valueless = valueless;
    }

    public Class<? extends HostCapability> requiredCapability() {
        return requiredCapability;
    }

    public static <Req extends HostCapability, V> Prop<Req, V> element(Class<Req> requires, String label, @Nullable V value,
            Cons2<Element, V> setter) {
        return new Prop<>(Kind.ELEMENT, requires, label, (view, v) -> view.withElement(element -> setter.get(element, v)),
                value, null, false);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> elementSource(Class<Req> requires, String label, @Nullable Readable<V> source,
            Cons2<Element, V> setter) {
        return new Prop<>(Kind.ELEMENT, requires, label, (view, v) -> view.withElement(element -> setter.get(element, v)),
                null, source, false);
    }

    public static <Req extends HostCapability> Prop<Req, Void> element(Class<Req> requires, String label, Cons<Element> mutation) {
        return new Prop<>(Kind.ELEMENT, requires, label, (view, v) -> view.withElement(mutation::get), null, null, true);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> table(Class<Req> requires, String label, @Nullable V value,
            Cons2<Table, V> setter) {
        return new Prop<>(Kind.TABLE, requires, label, (view, v) -> view.withTable(table -> setter.get(table, v)), value, null, false);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> tableSource(Class<Req> requires, String label, @Nullable Readable<V> source,
            Cons2<Table, V> setter) {
        return new Prop<>(Kind.TABLE, requires, label, (view, v) -> view.withTable(table -> setter.get(table, v)), null, source, false);
    }

    public static <Req extends HostCapability> Prop<Req, Void> table(Class<Req> requires, String label, Cons<Table> mutation) {
        return new Prop<>(Kind.TABLE, requires, label, (view, v) -> view.withTable(mutation::get), null, null, true);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> cell(Class<Req> requires, String label, @Nullable V value,
            Cons2<PendingCellConfig, V> setter) {
        return new Prop<>(Kind.CELL, requires, label, (view, v) -> view.withCell(config -> setter.get(config, v)), value, null, false);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> cell(Class<Req> requires, String label, @Nullable Readable<V> source,
            Cons2<PendingCellConfig, V> setter) {
        return new Prop<>(Kind.CELL, requires, label, (view, v) -> view.withCell(config -> setter.get(config, v)), null, source, false);
    }

    public static <Req extends HostCapability> Prop<Req, Void> cell(Class<Req> requires, String label, Cons<PendingCellConfig> mutation) {
        return new Prop<>(Kind.CELL, requires, label, (view, v) -> view.withCell(mutation::get), null, null, true);
    }

    public static <Req extends HostCapability, S> Prop<Req, S> cellSource(Class<Req> requires, String label, @Nullable Readable<S> source,
            Cons2<PendingCellConfig, Readable<S>> setter) {
        return new Prop<>(Kind.CELL, requires, label, (view, v) -> view.withCell(config -> setter.get(config, source)), null,
                source == null ? null : source, false);
    }

    public static <Req extends HostCapability, V> Prop<Req, V> raw(Class<Req> requires, String label, Kind kind, Cons<Object> mutation) {
        return new Prop<>(kind, requires, label, (view, v) -> {
            switch (kind) {
                case ELEMENT:
                    view.withElement(mutation::get);
                    break;
                case TABLE:
                    view.withTable(mutation::get);
                    break;
                case CELL:
                default:
                    view.withCell(mutation::get);
                    break;
            }
        }, null, null, true);
    }

    public Kind kind() {
        return kind;
    }

    public String label() {
        return label;
    }

    public boolean isReactive() {
        return source != null;
    }

    public @Nullable V resolve() {
        return source == null ? value : source.get();
    }

    public void writeTo(HostView view) {
        if (view == null) {
            return;
        }
        @Nullable V resolved = resolve();
        if (!valueless && resolved == null && source == null && value == null) {
            return;
        }
        applier.get(view, resolved);
    }

    public interface HostView {
        void withElement(Cons<Element> action);

        void withTable(Cons<Table> action);

        void withCell(Cons<PendingCellConfig> action);

        <H extends HostCapability> void withCapability(Class<H> capabilityClass, Cons<H> action);
    }
}


