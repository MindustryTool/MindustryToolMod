package solim.test;

import arc.func.Cons;
import solim.core.Component;
import solim.core.OwnershipContext;

public enum ModifierScope {

    STANDALONE {
        @Override
        public <T extends Component> T inScope(T component, Cons<T> body) {
            body.get(component);
            return component;
        }
    },

    AMBIENT {
        @Override
        public <T extends Component> T inScope(T component, Cons<T> body) {
            OwnershipContext.push(component);
            try {
                body.get(component);
            } finally {
                OwnershipContext.pop();
            }
            return component;
        }
    };

    public abstract <T extends Component> T inScope(T component, Cons<T> body);
}
