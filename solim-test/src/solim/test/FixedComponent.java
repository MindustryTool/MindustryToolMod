package solim.test;

import arc.scene.Element;
import solim.core.Component;

public class FixedComponent extends Component {

    public FixedComponent() {
        this(new Element());
    }

    public FixedComponent(Element element) {
        super(element);
    }
}
