package solim.runtime;

import arc.scene.Element;
import arc.util.Nullable;
import solim.core.Component;

public final class ElementResolver {
    private static final int MAX_PREVIEW = 80;

    private ElementResolver() {
    }

    public static Element resolve(@Nullable Object child) {
        if (child instanceof Element) {
            return (Element) child;
        }
        if (child instanceof Component) {
            Component c = (Component) child;
            return AttachmentStack.isolate(c::element);
        }
        throw new IllegalArgumentException("Cannot resolve child to Element: " + describe(child));
    }

    private static String describe(@Nullable Object child) {
        if (child == null) {
            return "<null>";
        }
        String className;
        try {
            className = child.getClass().getName();
        } catch (Throwable t) {
            className = "<unknown>";
        }
        String preview;
        try {
            preview = String.valueOf(child);
        } catch (Throwable t) {
            preview = "<toString failed>";
        }
        if (preview != null && preview.length() > MAX_PREVIEW) {
            preview = preview.substring(0, MAX_PREVIEW) + "…";
        }
        return className + " (" + (preview != null ? preview : "<null>") + ")";
    }
}
