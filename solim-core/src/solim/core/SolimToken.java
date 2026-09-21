package solim.core;

import arc.scene.Element;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.modifier.PendingCellConfig;

/**
 * Structured envelope stored on {@link Element#userObject} to hold Solim metadata
 * (component reference, cell configuration, expansion flags) without bare string or
 * raw object collisions, while preserving any external user payload.
 */
public final class SolimToken {

    public @Nullable Component component;
    public @Nullable PendingCellConfig cellConfig;
    public boolean expanding;
    public @Nullable Object userPayload;

    public static SolimToken getOrCreate(Element element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        if (element.userObject instanceof SolimToken) {
            return (SolimToken) element.userObject;
        }
        SolimToken token = new SolimToken();
        if (element.userObject != null) {
            token.userPayload = element.userObject;
        }
        element.userObject = token;
        return token;
    }

    public static @Nullable SolimToken get(@Nullable Element element) {
        return element != null && element.userObject instanceof SolimToken
                ? (SolimToken) element.userObject
                : null;
    }

    public static @Nullable Component getComponent(@Nullable Element element) {
        SolimToken token = get(element);
        return token != null ? token.component : null;
    }

    public static void bind(Element element, Component component) {
        SolimToken token = getOrCreate(element);
        token.component = component;
    }

    public static void bind(Element element, Component component, @Nullable PendingCellConfig config) {
        SolimToken token = getOrCreate(element);
        token.component = component;
        token.cellConfig = config;
    }

    public static void setExpanding(Element element, boolean expanding) {
        getOrCreate(element).expanding = expanding;
    }

    public static boolean isExpanding(@Nullable Element element) {
        SolimToken token = get(element);
        return token != null && token.expanding;
    }

    public static boolean isExpandingChild(@Nullable Element child) {
        return child != null
                && (isExpanding(child)
                        || "solim-spacer-table".equals(child.name)
                        || "spacer".equals(child.name)
                        || child.fillParent
                        || child instanceof ScrollPane
                        || hasScrollPaneChild(child));
    }

    private static boolean hasScrollPaneChild(Element child) {
        return child instanceof Table
                && ((Table) child).getChildren().size > 0
                && ((Table) child).getChildren().first() instanceof ScrollPane;
    }
}
