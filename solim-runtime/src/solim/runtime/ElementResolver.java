package solim.runtime;

import arc.scene.Element;
import solim.core.Component;

/** Resolves children that may be Element or Component. */
public final class ElementResolver {
	private ElementResolver() {}

	public static Element resolve(Object child) {
		if (child instanceof Element) {
			return (Element) child;
		}
		if (child instanceof Component) {
			Component c = (Component) child;
			ComponentContext.registerChild(c);
			return ParentStack.isolate(c::element);
		}
		throw new IllegalArgumentException("Cannot resolve child to Element: " + child);
	}
}
