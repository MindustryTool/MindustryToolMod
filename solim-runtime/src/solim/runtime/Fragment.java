package solim.runtime;

import arc.scene.Element;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import solim.core.Component;

/**
 * Internal multi-root ownership unit for dynamic structural components with
 * void factories.
 *
 * <p>
 * Wraps the components and resources captured from a void factory block into a
 * single ownable
 * and disposable unit. A fragment with zero roots is empty and is treated by
 * mount sites exactly
 * like a value factory returning {@code null} (collapse). A fragment with one
 * or more roots
 * mounts every root's element, each with its own cell configuration.
 *
 * <p>
 * A fragment has no single element of its own: {@link #element()} always
 * throws. Mount sites
 * walk {@link #roots()} and attach each root instead.
 *
 * <p>
 * This is an internal engine type: it is not part of the public Solim UI API.
 */
public final class Fragment extends Component {
    private List<Component> roots;

    private Fragment(List<Component> roots) {
        this.roots = roots;
    }

    /**
     * Captures components and reactive resources created inside the given block as
     * a single
     * fragment. Components are captured in creation order, in an isolated
     * parent-stack context,
     * without being attached to a live parent. Non-component resources (Computed,
     * Effect, Query,
     * Binding) are owned by the fragment and disposed with it. If the block throws,
     * all partially
     * created resources are disposed, ambient state is restored, and the exception
     * propagates.
     */
    public static Fragment capture(Runnable block) {
        List<Component> captured = AttachmentStack.takeCaptureList();
        Fragment fragment = new Fragment(captured);
        try {
            AttachmentStack.capture(block, captured, fragment);
        } catch (Throwable t) {
            fragment.dispose();
            throw t;
        }
        return fragment;
    }

    public List<Component> roots() {
        return isDisposed() ? Collections.<Component>emptyList() : Collections.unmodifiableList(roots);
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }

    public int size() {
        return roots.size();
    }

    /**
     * A fragment mounts its roots one by one, so it must never be attached itself:
     * doing so would
     * ask it for the single element it does not have.
     */
    @Override
    protected boolean attachesToParent() {
        return false;
    }

    @Override
    protected Element build() {
        throw new UnsupportedOperationException(
                "Fragment is a multi-root ownership unit; mount roots individually via roots()");
    }

    @Override
    public void onDispose() {
        List<Component> captured = this.roots;
        this.roots = Collections.emptyList();
        for (Component root : captured) {
            try {
                root.dispose();
            } catch (Throwable t) {
                Log.err("Error disposing fragment root", t);
            }
        }
        AttachmentStack.releaseCaptureList(captured);
    }

    public static Fragment of(List<Component> roots) {
        return new Fragment(new ArrayList<>(roots));
    }
}
