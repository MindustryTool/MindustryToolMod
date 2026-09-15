package solim.mcp.introspection;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;

/**
 * Supplies the root {@link Element} for introspection. Defaults to {@link Core#scene}'s root when
 * running inside Mindustry and can be overridden (e.g. in tests) with a fixed root.
 */
@FunctionalInterface
public interface SnapshotRoot {
	/** Returns the root element, or {@code null} if no scene is available. */
	@Nullable
	Element get();

	static SnapshotRoot sceneRoot() {
		return () -> Core.scene != null ? Core.scene.root : null;
	}

	static SnapshotRoot fixed(Element root) {
		return () -> root;
	}
}