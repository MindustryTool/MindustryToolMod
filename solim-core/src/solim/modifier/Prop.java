package solim.modifier;

import arc.func.Cons;
import arc.func.Cons2;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.reactive.Readable;

public final class Prop {

    public enum Kind {
        ELEMENT,
        TABLE,
        CELL
    }

    @FunctionalInterface
    public interface Applier {
        void apply(@Nullable Object target, @Nullable Object value);
    }

    private final Kind kind;
    private final String label;
    private final Applier applier;
    private final @Nullable Object value;
    private final @Nullable Readable<?> source;
    private final boolean valueLess;

    private Prop(Kind kind, String label, Applier applier, @Nullable Object value,
            @Nullable Readable<?> source, boolean valueLess) {
        this.kind = kind;
        this.label = label;
        this.applier = applier;
        this.value = value;
        this.source = source;
        this.valueLess = valueLess;
    }

    public static Prop element(String label, @Nullable Object value, Cons2<Element, Object> setter) {
        return new Prop(Kind.ELEMENT, label, (t, v) -> setter.get((Element) t, v), value, null, false);
    }

    public static Prop element(String label, @Nullable Readable<?> source, Cons2<Element, Object> setter) {
        return new Prop(Kind.ELEMENT, label, (t, v) -> setter.get((Element) t, v), null, source, false);
    }

    public static Prop table(String label, @Nullable Object value, Cons2<Table, Object> setter) {
        return new Prop(Kind.TABLE, label, (t, v) -> setter.get((Table) t, v), value, null, false);
    }

    public static Prop table(String label, @Nullable Readable<?> source, Cons2<Table, Object> setter) {
        return new Prop(Kind.TABLE, label, (t, v) -> setter.get((Table) t, v), null, source, false);
    }

    public static Prop cell(String label, @Nullable Object value, Cons2<PendingCellConfig, Object> setter) {
        return new Prop(Kind.CELL, label, (t, v) -> setter.get((PendingCellConfig) t, v), value, null, false);
    }

    public static Prop cell(String label, @Nullable Readable<?> source, Cons2<PendingCellConfig, Object> setter) {
        return new Prop(Kind.CELL, label, (t, v) -> setter.get((PendingCellConfig) t, v), null, source, false);
    }

    /**
     * A cell property whose setter receives the source itself rather than the resolved value,
     * so a reactive source can be recorded into the pending cell configuration. The surrounding
     * {@code Props} still wraps application in an effect when the source is non-null, which
     * keeps the live parent cell in sync exactly like the old mixin bindings did.
     */
    public static Prop cellSource(String label, @Nullable Readable<?> source,
            Cons2<PendingCellConfig, Readable<?>> setter) {
        return new Prop(Kind.CELL, label, (t, v) -> setter.get((PendingCellConfig) t, source),
                source, source, false);
    }

    public static Prop raw(String label, Kind kind, Cons<Object> mutation) {
        return new Prop(kind, label, (t, v) -> mutation.get(t), null, null, true);
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

    @Nullable
    public Object resolve() {
        return source == null ? value : source.get();
    }

    public void writeTo(@Nullable Object target) {
        if (target == null) {
            return;
        }
        Object resolved = resolve();
        if (resolved == null && source == null && !valueLess) {
            return;
        }
        applier.apply(target, resolved);
    }
}
