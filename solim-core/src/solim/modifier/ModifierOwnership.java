package solim.modifier;

import solim.core.Disposable;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;

/**
 * Ownership fallback for reactive modifier mixins ({@link ElementConfig}, {@link CellConfig},
 * {@link TableConfig}).
 *
 * <p>Every reactive modifier installs an {@code Effect} and hands it to {@link OwnershipContext},
 * which collects it for whatever is currently building. While a component's {@code build()} is
 * running that is enough — the component pushed itself as the active owner. Outside {@code build()}
 * there is no ambient owner, so the effect would outlive the component it configures and later
 * write into a disposed element. In that case the modifier hands the effect to the component
 * directly.
 *
 * <p>A keyed collection goes through {@code OwnershipScope} instead: the scope keeps what the
 * factory registered and transfers it to the component the factory returned, so the effect ends up
 * with the right owner either way.
 */
final class ModifierOwnership {

	private ModifierOwnership() {}

	/**
	 * Adopts {@code resource} into {@code component} when no ambient build scope captured it.
	 * Every modifier mixin is applied to a component, so the owner cast is unconditional.
	 */
	static <T extends Disposable> T own(Object component, T resource) {
		if (resource != null && OwnershipContext.current() == null) {
			((DisposableOwner) component).adopt(resource);
		}
		return resource;
	}
}
