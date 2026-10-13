package solim.test;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;

/**
 * Lazily-built component that runs a body inside a real {@link Component#build()}
 * ownership scope.
 *
 * <p>Disposal tests need the genuine ambient path: components and effects created inside
 * {@code build()} are adopted by the host rather than by themselves, so disposing the host
 * proves the ambient ownership chain works without the test having to reimplement it.
 */
public final class BuildScopeHost extends Component {

	private final Runnable body;
	private final Table root = new Table();

	public BuildScopeHost(Runnable body) {
		this.body = body;
	}

	@Override
	protected Element build() {
		body.run();
		return root;
	}
}