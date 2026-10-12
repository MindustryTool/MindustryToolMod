package solim.test;

import arc.scene.Element;
import solim.core.Component;

/**
 * A {@link Component} with a fixed element and nothing of its own to own.
 *
 * <p>Exists so tests can build a component inline without writing a subclass for every case,
 * now that {@code Component} is an abstract class rather than a functional interface.
 */
public class FixedComponent extends Component {

	public FixedComponent() {
		this(new Element());
	}

	public FixedComponent(Element element) {
		super(element);
	}
}
