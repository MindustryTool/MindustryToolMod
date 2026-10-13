package solim.mcp.introspection;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;

@FunctionalInterface
public interface SnapshotRoot {

    @Nullable
    Element get();

    static SnapshotRoot sceneRoot() {
        return () -> Core.scene != null ? Core.scene.root : null;
    }

    static SnapshotRoot fixed(Element root) {
        return () -> root;
    }
}
