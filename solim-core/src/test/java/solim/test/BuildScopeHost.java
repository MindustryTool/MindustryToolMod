package solim.test;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;

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
