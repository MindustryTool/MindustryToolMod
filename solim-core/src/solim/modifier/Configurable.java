package solim.modifier;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;

public interface Configurable<SELF extends Configurable<SELF> & DisposableOwner> {

    SELF self();

    SELF children(@Nullable Runnable block);

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

    default SELF props(Prop... props) {
        Props<SELF> recorded = new Props<>(self(), props);
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
                pending.append(props);
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