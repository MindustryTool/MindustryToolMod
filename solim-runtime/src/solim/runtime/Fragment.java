package solim.runtime;

import arc.scene.Element;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import solim.core.Component;

public final class Fragment extends Component {
    private List<Component> roots;

    private Fragment(List<Component> roots) {
        this.roots = roots;
    }

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
