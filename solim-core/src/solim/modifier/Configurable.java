package solim.modifier;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;

public interface Configurable<SELF extends Configurable<SELF> & DisposableOwner> {

    SELF self();

    boolean isBuilt();

    @Nullable
    Element element();

    @Nullable
    Table table();

    @Nullable
    PendingCellConfig cellConfig();

    @Nullable
    Props<SELF> pendingProps();

    void setPendingProps(@Nullable Props<SELF> props);

    default SELF props(Prop<? super SELF, ?> p1) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(1);
        list.add(p1);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(2);
        list.add(p1);
        list.add(p2);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(3);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(4);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(5);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5, Prop<? super SELF, ?> p6) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(6);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5, Prop<? super SELF, ?> p6,
                       Prop<? super SELF, ?> p7) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(7);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        list.add(p7);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5, Prop<? super SELF, ?> p6,
                       Prop<? super SELF, ?> p7, Prop<? super SELF, ?> p8) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(8);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        list.add(p7);
        list.add(p8);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5, Prop<? super SELF, ?> p6,
                       Prop<? super SELF, ?> p7, Prop<? super SELF, ?> p8, Prop<? super SELF, ?> p9) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(9);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        list.add(p7);
        list.add(p8);
        list.add(p9);
        return recordProps(list);
    }

    default SELF props(Prop<? super SELF, ?> p1, Prop<? super SELF, ?> p2, Prop<? super SELF, ?> p3,
                       Prop<? super SELF, ?> p4, Prop<? super SELF, ?> p5, Prop<? super SELF, ?> p6,
                       Prop<? super SELF, ?> p7, Prop<? super SELF, ?> p8, Prop<? super SELF, ?> p9,
                       Prop<? super SELF, ?> p10) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(10);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        list.add(p7);
        list.add(p8);
        list.add(p9);
        list.add(p10);
        return recordProps(list);
    }

    @SuppressWarnings("unchecked")
    default SELF props(Prop<? super SELF, ?>... props) {
        List<Prop<? super SELF, ?>> list = new ArrayList<>(props.length);
        for (int i = 0; i < props.length; i++) {
            list.add(props[i]);
        }
        return recordProps(list);
    }

    default SELF recordProps(List<Prop<? super SELF, ?>> list) {
        Props<SELF> recorded = new Props<>(self(), list);
        recorded.applyCellRecords();
        if (isBuilt()) {
            OwnershipContext.push(self());
            try {
                recorded.applyAll();
            } finally {
                OwnershipContext.pop();
            }
        } else {
            Props<SELF> pending = pendingProps();
            if (pending != null) {
                pending.append(list);
            } else {
                setPendingProps(recorded);
            }
        }
        return self();
    }

    default void applyPendingProps() {
        Props<SELF> pending = pendingProps();
        if (pending != null) {
            setPendingProps(null);
            pending.applyAll();
        }
    }
}