package solim.overlay;

import arc.util.Nullable;

public interface DockAnchorStore {

    @Nullable
    DockAnchor load();

    void save(@Nullable DockAnchor anchor);
}
