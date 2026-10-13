package solim.test;

import arc.func.Cons;
import solim.core.Component;
import solim.core.OwnershipContext;

/**
 * The two ownership paths a reactive modifier mixin can take.
 *
 * <p>
 * {@link #STANDALONE} is the {@code ModifierOwnership} fallback: no build scope
 * is open, so
 * the mixin hands the effect to the component it configures. {@link #AMBIENT}
 * is what
 * {@link Component#build()} does: the mixin registers the effect with the
 * ambient owner instead.
 * Disposal tests run both so neither path can regress unnoticed.
 */
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
